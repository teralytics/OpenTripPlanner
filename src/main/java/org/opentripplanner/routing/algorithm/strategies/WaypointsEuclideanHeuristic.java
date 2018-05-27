package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.List;

public class WaypointsEuclideanHeuristic extends DefaultRemainingWeightHeuristic {

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();
    private List<GenericLocation> remainingWaypoints;
    private double alphaDistanceInM;

    public WaypointsEuclideanHeuristic(double alphaInM) {
        this.alphaDistanceInM = alphaInM;
    }

    @Override
    public void initialize(RoutingRequest options, Vertex origin, Vertex target, long abortTime) {
        super.initialize(options, origin, target, abortTime);
        remainingWaypoints = new ArrayList<>();
        remainingWaypoints.addAll(options.waypoints);
        remainingWaypoints.add(options.to);
    }

    @Override
    public double computeForwardWeight(State state, Vertex target) {
        Vertex sv = state.getVertex();
        if (remainingWaypoints.size() == 1) {
            return calculateHeuristicCost(sv.getY(), sv.getX(), remainingWaypoints);
        } else {
            GenericLocation nextLocation = remainingWaypoints.get(0);
            double euclideanDistance = distanceLibrary.fastDistance(sv.getY(), sv.getX(), nextLocation.lat, nextLocation.lng);
            if (euclideanDistance <= alphaDistanceInM) {
                remainingWaypoints.remove(0);
            }
            return calculateHeuristicCost(sv.getY(), sv.getX(), remainingWaypoints);
        }
    }

    private double calculateHeuristicCost(Double lat, Double lon, List<GenericLocation> points) {
        GenericLocation newLoc = new GenericLocation(lat, lon);
        List<GenericLocation> pts = new ArrayList<>();
        pts.add(newLoc);
        pts.addAll(points);
        return calculateHeuristicCost(pts);
    }

    private double calculateHeuristicCost(List<GenericLocation> points) {
        double distance = 0.0;
        for (int i = 1; i < points.size(); i++) {
            GenericLocation prev = points.get(i - 1);
            GenericLocation next = points.get(i);
            distance += distanceLibrary.fastDistance(prev.lat, prev.lng, next.lat, next.lng);
        }
        return super.walkReluctance * distance / super.maxStreetSpeed;
    }
}