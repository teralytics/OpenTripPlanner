package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.DateUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * A termination strategy that terminates after multiple targets have been reached.
 * 
 * Useful for implementing a restricted batch search - i.e. doing one-to-many search 
 * without building a full shortest path tree.
 * 
 * @author avi
 */
public class MultiTargetTerminationStrategy implements SearchTerminationStrategy {

    private final Set<Vertex> unreachedTargets;
    private final Set<Vertex> reachedTargets;
    
    private double reachPercentage;
    private long abortTime = Long.MIN_VALUE;
    private double timeout;

    public MultiTargetTerminationStrategy(Set<Vertex> targets) {
        this(targets, Long.MIN_VALUE);
    }

    public MultiTargetTerminationStrategy(Set<Vertex> targets, double timeout, double reach) {
        unreachedTargets = new HashSet<Vertex>(targets);
        reachedTargets = new HashSet<Vertex>(targets.size());
        this.timeout = timeout;
        this.reachPercentage = reach;
    }

    public MultiTargetTerminationStrategy(Set<Vertex> targets, double timeout) {
        this(targets, timeout, 0.95);
    }

    public Set<Vertex> getUnreachedTargets() {
        return unreachedTargets;
    }

    public Set<Vertex> getReachedTargets() {
        return reachedTargets;
    }

    /**
     * Updates the list of reached targets and returns True if all the
     * targets have been reached.
     */
    @Override
    public boolean shouldSearchTerminate(Vertex origin, Vertex target, State current,
                                         ShortestPathTree spt, RoutingRequest traverseOptions) {
        setAbortTime();
        Vertex currentVertex = current.getVertex();
                
        // TODO(flamholz): update this to handle vertices that are not in the graph
        // but rather along edges in the graph.
        if (unreachedTargets.contains(currentVertex)) {
            unreachedTargets.remove(currentVertex);
            reachedTargets.add(currentVertex);
        }
        return unreachedTargets.size() == 0 || 
            (abortTime < Long.MAX_VALUE && System.currentTimeMillis() > abortTime && getReachedRatio() >= reachPercentage);
    }
    
    private double getReachedRatio() {
        int nReached = reachedTargets.size();
        int total = nReached + unreachedTargets.size();
        return 1.0 * nReached / total;
    }
    
    private void setAbortTime() {
        if (abortTime == Long.MIN_VALUE && timeout != Double.MIN_VALUE) {
            abortTime = DateUtils.absoluteTimeout(timeout);
        } else if (timeout == Double.MIN_VALUE) {
            abortTime = Long.MAX_VALUE;
        }
    }

}
