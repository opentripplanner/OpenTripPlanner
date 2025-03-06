package org.opentripplanner.street.model.vertex;

import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;

/**
 * TODO: decide what to do with this. Currently temporary vertices have only incoming or outgoing edges
 * But this one needs to have both since different vertex is start vertex
 * Created by mabu on 20.5.2016.
 */
public class TemporarySplitterVertex extends SplitterVertex implements TemporaryVertex {

  private boolean wheelchairAccessible;

  public TemporarySplitterVertex(String label, double x, double y, StreetEdge streetEdge) {
    super(label, x, y, streetEdge.getName());
    this.wheelchairAccessible = streetEdge.isWheelchairAccessible();
  }

  @Override
  public void addOutgoing(Edge edge) {
    // Removed check for edge being temporary
    super.addOutgoing(edge);
  }

  @Override
  public void addIncoming(Edge edge) {
    // Removed check for edge being temporary
    super.addIncoming(edge);
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  public void setWheelchairAccessible(boolean wheelchairAccessible) {
    this.wheelchairAccessible = wheelchairAccessible;
  }
}
