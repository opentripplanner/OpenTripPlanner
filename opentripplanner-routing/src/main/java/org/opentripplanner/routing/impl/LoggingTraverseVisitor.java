package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingTraverseVisitor implements TraverseVisitor {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingTraverseVisitor.class);

    @Override
    public void visitEdge(Edge edge, State state) {
        String nextName = edge.getName();
        LOG.info("Traversing edge {}", nextName);
    }

    @Override
    public void visitVertex(State state) {
        LOG.info("Visiting {}", state);
    }

    @Override
    public void visitEnqueue(State state) {
        LOG.info("Enqueing {}", state);
    }
}
