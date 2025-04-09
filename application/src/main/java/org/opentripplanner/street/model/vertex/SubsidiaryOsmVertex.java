package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

public class SubsidiaryOsmVertex extends OsmVertex {

  private OsmVertex parent;
  private int counter;

  private static int counterSource = 0;

  public SubsidiaryOsmVertex(OsmVertex parent) {
    super(
      parent.getX(),
      parent.getY(),
      parent.nodeId,
      parent.hasDrivingTrafficLight(),
      parent.hasWalkingTrafficLight()
    );
    this.parent = parent;
    this.counter = counterSource++;
  }

  @Override
  public I18NString getName() {
    return NO_NAME;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.SubsidiaryOsmNodeLabel(nodeId, counter);
  }
}
