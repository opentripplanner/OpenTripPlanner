package org.opentripplanner.graph_builder.module.osm.tagmapping;

import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.graph_builder.module.osm.specifier.ExactMatchSpecifier.exact;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.ALL;

import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.specifier.ExactMatchSpecifier;
import org.opentripplanner.graph_builder.module.osm.specifier.Test;
import org.opentripplanner.graph_builder.module.osm.specifier.Test.Absent;

public class PortlandMapper implements OsmTagMapper {

  @Override
  public void populateProperties(WayPropertySet props) {
    props.setMixinProperties("footway=sidewalk", withModes(ALL).walkSafety(1.1));
    props.setMixinProperties(
      new ExactMatchSpecifier(new Absent("name")),
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
    props.setMixinProperties(
      new ExactMatchSpecifier(new Test.GreaterThan("lanes", 4)),
      withModes(ALL).walkSafety(1.1)
    );
    props.setMixinProperties("sidewalk=both", withModes(ALL).walkSafety(0.8));
    props.setMixinProperties("sidewalk=left", withModes(ALL).walkSafety(0.9));
    props.setMixinProperties("sidewalk=right", withModes(ALL).walkSafety(0.9));
    props.setMixinProperties("surface=unpaved", withModes(ALL).walkSafety(1.4));
    // high penalty for streets with no sidewalk
    props.setMixinProperties(exact("sidewalk=no;maxspeed=55 mph"), withModes(ALL).walkSafety(6));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=50 mph"), withModes(ALL).walkSafety(5));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=45 mph"), withModes(ALL).walkSafety(4));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=40 mph"), withModes(ALL).walkSafety(3));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=35 mph"), withModes(ALL).walkSafety(2));
    props.setMixinProperties(exact("sidewalk=no;maxspeed=30 mph"), withModes(ALL).walkSafety(1.5));
    // Read the rest from the default set
    new DefaultMapper().populateProperties(props);
  }
}
