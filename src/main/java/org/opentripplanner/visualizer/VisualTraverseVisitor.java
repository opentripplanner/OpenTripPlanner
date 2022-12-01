package org.opentripplanner.visualizer;

import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VisualTraverseVisitor implements TraverseVisitor<State, Edge> {

  private static final Logger LOG = LoggerFactory.getLogger(VisualTraverseVisitor.class);

  private final ShowGraph gui;

  private final int SLEEP_AFTER = 50;
  private final int SLEEP_LEN = 2;

  private int sleepAfter = SLEEP_AFTER;

  public VisualTraverseVisitor(ShowGraph gui) {
    this.gui = gui;
  }

  @Override
  public void visitEdge(Edge edge) {
    gui.enqueueHighlightedEdge(edge);
    //gui.highlightVertex(state.getVertex());
  }

  @Override
  public void visitVertex(State state) {
    // every SLEEP_AFTER visits of a vertex, sleep for SLEEP_LEN
    // this slows down the search so it animates prettily
    if (--sleepAfter <= 0) {
      sleepAfter = SLEEP_AFTER;
      try {
        Thread.sleep(SLEEP_LEN);
      } catch (InterruptedException e) {
        LOG.warn("interrupted", e);
      }
    }
    gui.addNewSPTEdge(state);
  }

  @Override
  public void visitEnqueue() {
    //        Edge e = state.getBackEdge();
    //        if (e instanceof Edge) {
    //            gui.enqueueHighlightedEdge((Edge) e);
    //        }
  }
}
