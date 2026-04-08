package org.opentripplanner.osm.tagmapping;

import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

/**
 * OSM way properties for optimizing distance (not traveling time) in routing.
 */
class ConstantSpeedMapper extends FinlandMapper {

  private final float speed;

  public ConstantSpeedMapper() {
    super();
    // 80 km/h by default
    this.speed = 22.22f;
  }

  public ConstantSpeedMapper(float speed) {
    super();
    this.speed = speed;
  }

  @Override
  public WayPropertySet buildWayPropertySet() {
    var props = WayPropertySet.of();
    props.setCarSpeed("highway=*", speed);
    var s = super.buildWayPropertySet();
    props.addPickers(s);
    props.setMaxPossibleCarSpeed(speed);
    return props.build();
  }

  @Override
  public float getCarSpeedForWay(
    OsmEntity way,
    TraverseDirection direction,
    DataImportIssueStore issueStore
  ) {
    /*
     * Set the same 80 km/h speed for all roads, so that car routing finds shortest path
     */
    return speed;
  }
}
