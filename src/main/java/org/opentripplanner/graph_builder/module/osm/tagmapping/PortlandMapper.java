package org.opentripplanner.graph_builder.module.osm.tagmapping;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;

import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.specifier.ExactMatchSpecifier;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.Absent;

public class PortlandMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setMixinProperties("footway=sidewalk", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties("footway=sidewalk", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties(
      new ExactMatchSpecifier(new Absent("absent")),
      withModes(ALL).walkSafety(1.2)
    );
    props.setMixinProperties("highway=trunk", withModes(ALL).walkSafety(1.2));
    props.setMixinProperties("highway=trunk_link", withModes(ALL).walkSafety(1.2));
    props.setMixinProperties("highway=primary", withModes(ALL).walkSafety(1.2));
    props.setMixinProperties("highway=primary_link", withModes(ALL).walkSafety(1.2));
    props.setMixinProperties("highway=secondary", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties("highway=secondary_link", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties("highway=tertiary", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties("highway=tertiary_link", withModes(ALL).walkSafety(1.1));
    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }
}
