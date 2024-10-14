package org.opentripplanner.osm.tagmapping;

import org.opentripplanner.osm.model.OsmWithTags;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

/**
 * OSM way properties for optimizing distance (not traveling time) in routing.
 */
class ConstantSpeedFinlandMapper implements OsmTagMapper {

  private float speed;

  public ConstantSpeedFinlandMapper() {
    super();
    this.speed = 22.22f; // 80 kmph by default
  }

  public ConstantSpeedFinlandMapper(float speed) {
    super();
    this.speed = speed;
  }

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setCarSpeed("highway=*", speed);
    // Read the rest from the default set
    new FinlandMapper().populateProperties(props);
    props.maxPossibleCarSpeed = speed;
  }

  @Override
  public float getCarSpeedForWay(OsmWithTags way, boolean backward) {
    /*
     * Set the same 80 km/h speed for all roads, so that car routing finds shortest path
     */
    return speed;
  }

  @Override
  public Float getMaxUsedCarSpeed(WayPropertySet wayPropertySet) {
    // This is needed because the way property set uses normal speed limits from Finland mapper
    // to set the walk safety limits which resets the maximum used car speed to be something else
    // than what is used for the street edge car speeds.
    return speed;
  }
}
