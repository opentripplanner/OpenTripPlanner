package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * OSM way properties for the Houston, Texas, USA area.
 * <p>
 * The differences compared to the default property set are:
 * <p>
 * 1. In Houston we want to disallow usage of downtown pedestrian tunnel system.
 *
 * @author demory
 * @see WayPropertySetSource
 * @see DefaultWayPropertySetSource
 */

public class HoustonWayPropertySetSource implements WayPropertySetSource {

  @Override
  public void populateProperties(WayPropertySet props) {
    // Disallow any use of underground indoor pedestrian tunnels
    props.setProperties(
      "highway=footway;layer=-1;tunnel=yes;indoor=yes",
      withModes(StreetTraversalPermission.NONE)
    );
    props.setProperties(
      "highway=*;layer=-1;tunnel=yes;indoor=yes",
      withModes(StreetTraversalPermission.ALL)
    );
    props.setProperties(
      "highway=footway;tunnel=yes;indoor=yes",
      withModes(StreetTraversalPermission.PEDESTRIAN)
    );

    // Read the rest from the default set
    new DefaultWayPropertySetSource().populateProperties(props);
  }
}
