package org.opentripplanner.graph_builder.module.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.carTunnel;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cobblestones;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.footwaySidewalk;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.highwayTertiary;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.highwayTertiaryWithSidewalk;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.highwayTrunk;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.noSidewalk;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.noSidewalkHighSpeed;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.pedestrianTunnel;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.sidewalkBoth;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.southeastLaBonitaWay;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.southwestMayoStreet;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.test.support.VariableSource;

class PortlandMapperTest {

  static double delta = 0.1;

  static WayPropertySet wps = new WayPropertySet();
  static Stream<Arguments> cases = Stream.of(
    Arguments.of(southeastLaBonitaWay(), 0.8),
    Arguments.of(southwestMayoStreet(), 0.9),
    Arguments.of(sidewalkBoth(), 0.96d),
    Arguments.of(pedestrianTunnel(), 1.0d),
    Arguments.of(highwayTertiaryWithSidewalk(), 1.056d),
    Arguments.of(cobblestones(), 1.2d),
    Arguments.of(noSidewalk(), 1.2d),
    Arguments.of(footwaySidewalk(), 1.32d),
    Arguments.of(highwayTertiary(), 1.32d),
    Arguments.of(highwayTrunk(), 1.44d),
    Arguments.of(carTunnel(), 1.44d),
    Arguments.of(fiveLanes(), 1.584),
    Arguments.of(noSidewalkHighSpeed(), 7.19)
  );

  static {
    var source = new PortlandMapper();
    source.populateProperties(wps);
  }

  @ParameterizedTest(name = "way {0} should have walk safety factor {1}")
  @VariableSource("cases")
  void walkSafety(OSMWithTags way, double expected) {
    var score = wps.getDataForWay(way);

    var ws = score.getWalkSafetyFeatures();
    assertEquals(expected, ws.forward(), delta);
    assertEquals(expected, ws.back(), delta);
  }
}
