package org.opentripplanner.osm.tagmapping;

import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.Condition;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;

/**
 * The Twin Cities of Minneapolis and St. Paul, Minnesota have so-called skyways, which are elevated
 * walkways that connect buildings and neighborhoods. These skyways are typically pedestrian-only.
 * Access to them is time-restricted and can be unpredictable.
 * Therefore, we don't add them to the graph.
 */
class TwinCitiesMapper extends OsmTagMapper {

  @Override
  public WayPropertySet buildWayPropertySet() {
    var props = WayPropertySet.of();
    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("name", "Minneapolis Skyway")),
      withModes(NONE)
    );
    props.setProperties(
      new ExactMatchSpecifier(new Condition.Equals("name", "Saint Paul Skyway")),
      withModes(NONE)
    );

    props.addPickers(super.buildWayPropertySet());
    return props.build();
  }
}
