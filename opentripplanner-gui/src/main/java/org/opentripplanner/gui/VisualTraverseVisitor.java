package org.opentripplanner.gui;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.GenericAStarFactory;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

public class VisualTraverseVisitor implements TraverseVisitor {

    private ShowGraph gui;
    List<Vertex> seen = new ArrayList<Vertex>();

    public VisualTraverseVisitor(ShowGraph gui) {
        this.gui = gui;
    }
    
    @Override
    public void visitEdge(Edge edge, State state) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (edge instanceof DirectEdge) {
            gui.highlightEdge((DirectEdge) edge);
        }
        gui.highlightVertex(state.getVertex());
    }

    @Override
    public void visitVertex(State state) {
        seen.add(state.getVertex());
        gui.setHighlightedVertices(seen);
        gui.highlightVertex(state.getVertex());
    }

    public GenericAStarFactory getAStarSearchFactory() {
        return new GenericAStarFactory() {
            
            @Override
            public GenericAStar createAStarInstance() {
                GenericAStar astar = new GenericAStar();
                astar.setTraverseVisitor(VisualTraverseVisitor.this);
                return astar;
            }
        };
    }

}
