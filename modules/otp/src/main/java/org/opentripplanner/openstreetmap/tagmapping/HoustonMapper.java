package org.opentripplanner.openstreetmap.tagmapping;

import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.openstreetmap.wayproperty.specifier.ExactMatchSpecifier;

/**
 * OSM way properties for the Houston, Texas, USA area.
 * <p>
 * The differences compared to the default property set are:
 * <p>
 * 1. In Houston we want to disallow usage of downtown pedestrian tunnel system.
 */

class HoustonMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    // Disallow any use of underground indoor pedestrian tunnels
    props.setProperties(
      // we use an exact match since the default specifier would match more than we want as the
      // many key/value pairs can lead to high scores for ways that should _not_ be matched, like
      // regular car tunnels
      new ExactMatchSpecifier("highway=footway;layer=-1;tunnel=yes;indoor=yes"),
      withModes(NONE)
    );

    // Max speed limit in Texas is 38 m/s ~= 85 mph ~= 137 kph
    props.maxPossibleCarSpeed = 38f;

    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }
}
