package org.opentripplanner.ext.flex.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStopVertex;

import java.util.Locale;

public class FlexTransferEdge extends Edge {

  private final int minTransferTimeSeconds;

  public FlexTransferEdge(
      TransitStopVertex transferFromVertex, TransitStopVertex transferToVertex,
      int minTransferTimeSeconds
  ) {
    super(new Vertex(null, null, 0.0, 0.0) {}, new Vertex(null, null, 0.0, 0.0) {});
    this.minTransferTimeSeconds = minTransferTimeSeconds;
    this.fromv = transferFromVertex;
    this.tov = transferToVertex;
    // Why is this code so dirty? Because we don't want this edge to be added to the edge lists.
  }

  @Override
  public State traverse(State s0) {
    StateEditor editor = s0.edit(this);
    editor.setBackMode(TraverseMode.WALK);
    editor.incrementWeight(minTransferTimeSeconds);
    editor.incrementTimeInSeconds(minTransferTimeSeconds);
    return editor.makeState();
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getName(Locale locale) {
    return null;
  }
}
