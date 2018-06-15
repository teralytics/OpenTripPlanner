package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class WaypointsEuclideanHeuristic extends EuclideanRemainingWeightHeuristic {

    private List<GenericLocation> remainingWaypoints;

    private double alphaDistanceInM;
    private double importanceMultiplier;
    private double sample = 1.0;

    public WaypointsEuclideanHeuristic(double alphaInM, double importance, double sample) {
        this.alphaDistanceInM = alphaInM;
        this.importanceMultiplier = importance;
        this.sample = sample;
    }

    @Override
    public void initialize(RoutingRequest options, long abortTime) {
        super.initialize(options, abortTime);
        remainingWaypoints = new ArrayList<>();
        if (sample < 1.0) {
            int N = Math.max(1, (int)(sample * options.waypoints.size()));
            long seed = getSeed(options);
            List<GenericLocation> sampledWaypoints = sample(options.waypoints, N, seed);
            this.remainingWaypoints = sampledWaypoints;
        } else {
            this.remainingWaypoints.addAll(options.waypoints);
        }
        remainingWaypoints.add(options.to);
    }

    private long getSeed(RoutingRequest options) {
        long seed = locationHashCode(options.from);
        for (int i = 0; i < options.waypoints.size(); i++) seed += locationHashCode(options.waypoints.get(i));
        seed += locationHashCode(options.to);
        return seed;
    }

    private int locationHashCode(GenericLocation gl) {
        return ((Double) (gl.lat + gl.lng)).hashCode();
    }

    private List<GenericLocation> sample(List<GenericLocation> l, int N, long seed) {
        List<Integer> idx = new ArrayList<>();
        int M = l.size();
        for (int i = 0; i < M; i++) idx.add(i);
        Collections.shuffle(idx, new Random(seed));
        List<Integer> sampledIdx = idx.subList(0, N);
        Collections.sort(sampledIdx);
        List<GenericLocation> sampled = new ArrayList<>(sampledIdx.size());
        for (int j : sampledIdx) sampled.add(l.get(j));
        return sampled;
    }

    @Override
    public double getDistance(State state) {
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