package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.List;

public class WaypointsEuclideanHeuristic extends EuclideanRemainingWeightHeuristic {

    private List<GenericLocation> remainingWaypoints;
    private double alphaDistanceInM;
    private double importanceMultiplier;

    public WaypointsEuclideanHeuristic(double alphaInM, double importance) {
        this.alphaDistanceInM = alphaInM;
        this.importanceMultiplier = importance;
    }

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        super.initialize(options, abortTime);
        remainingWaypoints = new ArrayList<>();
        remainingWaypoints.addAll(options.waypoints);
        remainingWaypoints.add(options.to);
    }

    @Override
    public double estimateRemainingWeight(State state) {
        Vertex sv = state.getVertex();
        if (remainingWaypoints.size() == 1) {
            return calculateHeuristicCost(sv.getY(), sv.getX(), remainingWaypoints);
        } else {
            GenericLocation nextLocation = remainingWaypoints.get(0);
            double euclideanDistance = SphericalDistanceLibrary.fastDistance(sv.getY(), sv.getX(), nextLocation.lat, nextLocation.lng);
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
            distance += SphericalDistanceLibrary.fastDistance(prev.lat, prev.lng, next.lat, next.lng);
        }
        return importanceMultiplier * (super.walkReluctance * distance / super.maxStreetSpeed);
    }
}