package org.opentripplanner.graph_builder.module.osm;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.BICYCLE;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.PEDESTRIAN;

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
    props.setProperties("highway=*;layer=-1;tunnel=yes;indoor=yes", withModes(ALL));
    props.setProperties("highway=cycleway;tunnel=yes;indoor=yes", withModes(BICYCLE));
    // sadly we need these permutations since otherwise they would match with the final props
    // I'm not sure if this is a bug or working as intended
    props.setProperties("highway=footway;tunnel=yes;indoor=yes", withModes(PEDESTRIAN));
    props.setProperties("highway=footway;tunnel=yes;layer=-1", withModes(PEDESTRIAN));
    props.setProperties("highway=footway", withModes(PEDESTRIAN));

    // Disallow any use of underground indoor pedestrian tunnels
    props.setProperties("highway=footway;layer=-1;tunnel=yes;indoor=yes", withModes(NONE));
    // Read the rest from the default set
    new DefaultWayPropertySetSource().populateProperties(props);
  }
}
