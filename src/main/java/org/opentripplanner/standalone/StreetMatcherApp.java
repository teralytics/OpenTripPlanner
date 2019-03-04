package org.opentripplanner.standalone;

import com.vividsolutions.jts.geom.Geometry;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.impl.map.StreetMatcher;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.io.*;
import java.util.*;

public class StreetMatcherApp {

    private static class MatchingResult {
        private String routeId;
        private Geometry routeGeometry;
        private List<Edge> matchedEdges;
        private Boolean isMatched;

        MatchingResult(String routeId, Geometry g, List<Edge> edges) {
            this.routeId = routeId;
            this.routeGeometry = g;
            this.matchedEdges = edges;
            this.isMatched = edges != null && !edges.isEmpty();
        }

        Geometry matchedGeometry() {
            return matchedEdges.stream().map(x -> (Geometry)x.getGeometry()).reduce(Geometry::union).get();
        }

        String toCsv() {
            return routeId + "|" + routeGeometry + "|" + ((isMatched) ? matchedGeometry() : "");
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {


        String graphPath = "/var/otp/graphs/db-tender-test/Graph.obj";
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(graphPath));
        Graph graph = Graph.load(is, Graph.LoadLevel.FULL, new DefaultStreetVertexIndexFactory());

//        List<String> lines = graph.getEdges().stream().filter(x -> x instanceof PublicTransitEdge).map(x -> x.getId() + "|" + x.getGeometry() + "|" + ((PublicTransitEdge)x).getPublicTransitType()).collect(Collectors.toList());
//        File output = new File("/Users/irakitin/work/db-tender/gtfs/public-transit-edges.csv");
//        Files.write(output.toPath(), lines);

        Map<String, TripPattern> allPatterns = new HashMap<>();
        for (Route r: graph.index.routeForId.values()) {
            Collection<TripPattern> patterns = graph.index.patternsForRoute.get(r);
            for (TripPattern tp: patterns) {
                allPatterns.put(tp.name, tp);
            }
        }

        Map<String, Geometry> routeGeometries = new HashMap<>();
        Map<String, List<PatternHop>> routeHops = new HashMap<>();
        for (Map.Entry<String, TripPattern> entry: allPatterns.entrySet()) {
            List<PatternHop> hops = entry.getValue().getPatternHops();
            routeHops.put(entry.getKey(), hops);
            Geometry g = hops.stream().map(x -> (Geometry)x.getGeometry()).reduce(Geometry::union).get();
            routeGeometries.put(entry.getKey(), g);
        }

        Collection<TraverseMode> modes = new ArrayList<>();
        modes.add(TraverseMode.RAIL);
        modes.add(TraverseMode.TRAM);
        modes.add(TraverseMode.SUBWAY);
        TraverseModeSet modeSet = new TraverseModeSet(modes);

        String input = "";
        Double threshold = 0.001;
        Scanner scanIt = new Scanner(System.in);

        while (!input.equals("Q")) {
            input = scanIt.nextLine();
            threshold = Double.parseDouble(scanIt.nextLine());
            Double maxError = Double.parseDouble(scanIt.nextLine());
            StreetMatcher matcher = new StreetMatcher(graph, threshold, maxError);
            List<PatternHop> hops = routeHops.get(input);
            List<MatchingResult> results = new ArrayList<>();
            for (PatternHop hop: hops) {
                MatchingResult r = matchToEdges(hop.getId(), hop.getGeometry(), matcher, modeSet);
                System.out.println(r.matchedGeometry());
            }
        }

        scanIt.close();

//        List<MatchingResult> results = new ArrayList<>();
//
//        routeGeometries.entrySet().stream().forEach(entry -> {
//            MatchingResult res = matchToEdges(entry.getKey(), entry.getValue(), matcher, railMode);
//            results.add(res);
//            System.out.println(res.toCsv());
//        });
    }

    private static MatchingResult matchToEdges(int hopId, Geometry g, StreetMatcher matcher, TraverseModeSet modes) {
        List<Edge> matchedEdges = matcher.match(g, modes);
        return new MatchingResult(hopId + "", g, matchedEdges);
    }
}
