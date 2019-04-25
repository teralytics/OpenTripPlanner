package org.opentripplanner.standalone;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.linearref.LinearGeometryBuilder;
import com.vividsolutions.jts.linearref.LinearLocation;
import com.vividsolutions.jts.linearref.LocationIndexedLine;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.graph_builder.impl.map.StreetMatcher;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class StreetMatcherApp {

    private static class MatchingResult {
        private String routeId;
        private int hopId;
        private String startStopId;
        private String endStopId;
        private Geometry hopGeometry;
        private List<Edge> matchedEdges = new ArrayList<>();
        private Boolean isMatched;

        MatchingResult(PatternHop hop, List<Edge> edges) {
            this.hopId = hop.getId();
            this.startStopId = hop.getBeginStop().getId().getId();
            this.endStopId = hop.getEndStop().getId().getId();
            this.hopGeometry = hop.getGeometry();
            this.matchedEdges = edges;
            this.isMatched = edges != null && !edges.isEmpty();
        }

        MatchingResult(MatchingResult r) {
            this.routeId = r.routeId;
            this.hopId = r.hopId;
            this.startStopId = r.startStopId;
            this.endStopId = r.endStopId;
            this.hopGeometry = r.hopGeometry;
            this.matchedEdges = r.matchedEdges == null ? null : new ArrayList<>(r.matchedEdges);
            this.isMatched = r.isMatched;
        }

        void setRouteId(String id) {
            this.routeId = id;
        }

        Geometry matchedGeometry() {
            return matchedEdges.stream().map(x -> (Geometry)x.getGeometry()).reduce(Geometry::union).get();
        }

        String toCsv() {
            return routeId + "|" + hopId + "|" + startStopId + "|" + endStopId + "|" + hopGeometry + "|" + ((isMatched) ? matchedGeometry() : "");
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {

        Set<TraverseMode> modes = new HashSet<>();
        modes.add(TraverseMode.RAIL);

        double threshold = Double.parseDouble(args[2]);
        double maxError = threshold * 111111;

        String alreadyMatchedPath = args[3];
        List<String> readLines = Files.readAllLines(new File(alreadyMatchedPath).toPath());

        System.out.println("Read " + readLines.size() + " from '" + alreadyMatchedPath + "'");

        Set<Integer> unmatchedHops = readLines.stream()
                .map(x -> {
                    String[] items = x.split("\\|");
                    if (items.length == 5 || items[5] == null || items[5].isEmpty()) {
                        return Integer.parseInt(items[1]);
                    } else return 0;
                }).collect(Collectors.toSet());
        unmatchedHops.remove(0);

        System.out.println("Will be processing " + unmatchedHops.size() + " unmatched hops");

        String graphPath = args[0];
        System.out.println("Reading OTP graph from '" + graphPath + "'");
        ObjectInputStream is = new ObjectInputStream(new FileInputStream(graphPath));
        Graph graph = Graph.load(is, Graph.LoadLevel.FULL, new DefaultStreetVertexIndexFactory());

        StreetMatcher matcher = new StreetMatcher(graph, threshold, maxError);

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

        Set<PatternHop> hops = new HashSet<>();

        for (List<PatternHop> hs: routeHops.values()) {
            hops.addAll(hs);
        }
        hops = hops.stream().filter(x -> unmatchedHops.contains(x.getId())).collect(Collectors.toSet());
        System.out.println("Filtered all hops to " + hops.size() + " unmatched hops");

        Map<String, List<PatternHop>> hopRoutes = new HashMap<>();
        for (PatternHop h: hops) {
            String key = h.getBeginStop().getId().getId() + "-" + h.getEndStop().getId().getId();
            if (!hopRoutes.containsKey(key)) {
                hopRoutes.put(key, new ArrayList<>());
            }
            hopRoutes.get(key).add(h);
        }

        List<MatchingResult> results = new ArrayList<>();
        for (Map.Entry<String, List<PatternHop>> entry: hopRoutes.entrySet()) {
            System.out.println("Processed " + results.size() + " hops out of " + hops.size());
            PatternHop hop = entry.getValue().get(0);
            MatchingResult res = matchHop(matcher, hop, modes);
            if (res.isMatched) {
                System.out.println("Matched hop " + hop.getId());
            } else {
                System.out.println("Failed to match hop " + hop.getId() + " on route " + hop.getPattern().route.getId().getId());
            }
            for (PatternHop h: entry.getValue()) {
                MatchingResult copy = new MatchingResult(res);
                copy.setRouteId(h.getPattern().route.getId().getId());
                results.add(copy);
            }
        }

        long totalMatched = results.stream().filter(x -> x.isMatched).count();
        long totalUnmatched = results.stream().filter(x -> !x.isMatched).count();

        System.out.println("Total matched hops = " + totalMatched + ", total unmatched = " + totalUnmatched);

        String outputPath = args[1];
        System.out.println("Saving results to '" + outputPath + "'");

        List<String> lines = results.stream().map(MatchingResult::toCsv).collect(Collectors.toList());
        File output = new File(outputPath);
        Files.write(output.toPath(), lines);

//        FOR DEBUG PURPOSES
//        String input = "";
//        Scanner scanIt = new Scanner(System.in);
//        while (!input.equals("Q")) {
//            input = scanIt.nextLine();
//            List<PatternHop> hops = routeHops.get(input);
//            System.out.println(routeGeometries.get(input));
//            for (PatternHop hop: hops) {
//                MatchingResult r = matchHop(matcher, hop, modes);
//                System.out.println(r.matchedGeometry());
//            }
//        }
//        scanIt.close();
    }

    private static MatchingResult matchHop(StreetMatcher matcher, PatternHop hop, Set<TraverseMode> modes) {
        Geometry hopGeometry = hop.getGeometry();
        List<Edge> matchedEdges = matcher.match(hopGeometry, modes);
        return new MatchingResult(hop, matchedEdges);
    }

    private static Geometry segmentLine(Geometry line, int N) {
        double f = 1.0 / N;
        LinearGeometryBuilder lgb = new LinearGeometryBuilder(line.getFactory());
        LocationIndexedLine index = new LocationIndexedLine(line);
        for (int i = 0; i <= N; i++) {
            Coordinate c = index.extractPoint(new LinearLocation(0, i * f));
            lgb.add(c);
        }
        return lgb.getGeometry();
    }
}
