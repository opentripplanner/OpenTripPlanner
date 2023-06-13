package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

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
  }

  @Override
  public float getCarSpeedForWay(OSMWithTags way, boolean backward) {
    /*
     * Set the same 80 km/h speed for all roads, so that car routing finds shortest path
     */
    return speed;
  }
}
