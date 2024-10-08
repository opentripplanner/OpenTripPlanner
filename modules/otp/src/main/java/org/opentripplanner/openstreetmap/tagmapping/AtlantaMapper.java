package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;

import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

/**
 * OSM way properties for the Atlanta, Georgia, USA area.
 * The differences compared to the default
 * property set are: In Atlanta "trunk" is used for the most important primary thoroughfares, but
 * these roads typically still allow pedestrian traffic and often include bus service / stops.
 *
 * @author demory
 * @see OsmTagMapper
 * @see DefaultMapper
 */

class AtlantaMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    // Replace existing matching properties as the logic is that the first statement registered takes precedence over later statements
    props.setProperties("highway=trunk_link", withModes(ALL).bicycleSafety(2.5));
    props.setProperties("highway=trunk", withModes(ALL).bicycleSafety(2.5));

    // Max speed limit in Georgia is 70 mph ~= 113kmh ~= 31.3m/s
    props.maxPossibleCarSpeed = 31.4f;

    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }
}
