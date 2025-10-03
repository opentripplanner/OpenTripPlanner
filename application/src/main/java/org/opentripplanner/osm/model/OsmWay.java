package org.opentripplanner.osm.model;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import java.util.Set;

public class OsmWay extends OsmEntity {

  private static final Set<String> ESCALATOR_CONVEYING_TAGS = Set.of(
    "yes",
    "forward",
    "backward",
    "reversible"
  );

  private final TLongList nodes = new TLongArrayList();

  public void addNodeRef(long nodeRef) {
    nodes.add(nodeRef);
  }

  public void addNodeRef(long nodeRef, int index) {
    nodes.insert(index, nodeRef);
  }

  public TLongList getNodeRefs() {
    return nodes;
  }

  public String toString() {
    return "osm way " + id;
  }

  /**
   * Returns true if way geometry is a closed loop
   */
  public boolean isClosed() {
    int size = nodes.size();

    if (size > 2) {
      long a = nodes.get(0);
      long b = nodes.get(size - 1);
      return a == b;
    }
    return false;
  }

  /**
   * Returns true if way is both boarding location and closed polygon
   */
  public boolean isBoardingArea() {
    return isBoardingLocation() && isClosed();
  }

  /**
   * @return true if these are steps.
   */
  public boolean isSteps() {
    return isTag("highway", "steps");
  }

  /**
   * @return true if these are stairs.
   */
  public boolean isStairs() {
    return isTag("highway", "steps") && !isOneOfTags("conveying", ESCALATOR_CONVEYING_TAGS);
  }

  /**
   * Checks the wheelchair-accessibility of this way. Stairs and escalators are by default
   * inaccessible but can be made accessible if they explicitly set wheelchair=true.
   */
  public boolean isWheelchairAccessible() {
    if (isSteps()) {
      return isTagTrue("wheelchair");
    } else {
      return super.isWheelchairAccessible();
    }
  }

  public boolean isEscalator() {
    return isTag("highway", "steps") && isOneOfTags("conveying", ESCALATOR_CONVEYING_TAGS);
  }

  public boolean isForwardEscalator() {
    return isEscalator() && isTag("conveying", "forward");
  }

  public boolean isBackwardEscalator() {
    return isEscalator() && isTag("conveying", "backward");
  }

  public boolean isInclineUp() {
    return isTag("incline", "up");
  }

  public boolean isInclineDown() {
    return isTag("incline", "down");
  }

  /**
   * Returns true if the way is considered an area.
   *
   * An area can be specified as such, or be one by default as an amenity.
   */
  public boolean isRoutableArea() {
    return (
      !isTag("area", "no") &&
      (isTag("area", "yes") ||
        isParking() ||
        isBikeParking() ||
        isBoardingArea() ||
        isIndoorRoutable()) &&
      getNodeRefs().size() > 2
    );
  }

  public boolean isBarrier() {
    return hasTag("barrier");
  }

  @Override
  public String url() {
    return String.format("https://www.openstreetmap.org/way/%d", getId());
  }

  /**
   * Returns true if this way is relevant for routing.
   *
   * @return if it is either a routable way, a P&R way or a boarding location.
   */
  public boolean isRelevantForRouting() {
    return isRoutable() || isParkAndRide() || isBikeParking() || isBoardingLocation();
  }
}
