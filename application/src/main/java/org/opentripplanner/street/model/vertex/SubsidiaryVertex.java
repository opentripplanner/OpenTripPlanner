package org.opentripplanner.street.model.vertex;

import org.opentripplanner.framework.i18n.I18NString;

/**
 * When applying turn restrictions to a graph makes copies of an IntersectionVertex,
 * the original is the parent vertex and the copies are subsidiary vertices. The
 * subsidiary vertices know their parent, and for most purposes of checking whether
 * any two vertices are the same, you should check that the parents are the same
 * object. A subsidiary vertex is at exactly the same geographical coordinates than
 * the parent, it just has a different arrival history. From a given starting location
 * and traverse mode, you can only traverse into one vertex of a group of vertices
 * like this.
 */
public class SubsidiaryVertex extends IntersectionVertex {

  private final IntersectionVertex parent;
  private final int counter;

  private static int counterSource = 0;

  public SubsidiaryVertex(IntersectionVertex parent) {
    super(
      parent.getX(),
      parent.getY(),
      parent.hasDrivingTrafficLight(),
      parent.hasWalkingTrafficLight()
    );
    this.parent = parent;
    this.counter = counterSource++;
  }

  public Vertex getParent() {
    return parent;
  }

  @Override
  public I18NString getName() {
    return NO_NAME;
  }

  @Override
  public VertexLabel getLabel() {
    return new VertexLabel.SubsidiaryVertexLabel(parent.getLabel(), counter);
  }
}
