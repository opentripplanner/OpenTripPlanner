package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.carTunnel;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cobblestones;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.footwaySidewalk;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.highwayTertiary;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.highwayTertiaryWithSidewalk;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.highwayTrunk;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.noSidewalk;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.noSidewalkHighSpeed;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.pedestrianTunnel;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.sidewalkBoth;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.southeastLaBonitaWay;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.southwestMayoStreet;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

public class PortlandMapperTest {

  static double delta = 0.1;

  static WayPropertySet wps = new WayPropertySet();

  static Stream<Arguments> cases() {
    return Stream.of(
      Arguments.of(southeastLaBonitaWay(), 0.8),
      Arguments.of(southwestMayoStreet(), 0.9),
      Arguments.of(sidewalkBoth(), 0.96),
      Arguments.of(pedestrianTunnel(), 1.0),
      Arguments.of(highwayTertiaryWithSidewalk(), 1.056),
      Arguments.of(cobblestones(), 1.2),
      Arguments.of(noSidewalk(), 1.2),
      Arguments.of(carTunnel(), 1.2),
      Arguments.of(footwaySidewalk(), 1.32),
      Arguments.of(highwayTertiary(), 1.32),
      Arguments.of(highwayTrunk(), 1.44),
      Arguments.of(fiveLanes(), 1.584),
      Arguments.of(noSidewalkHighSpeed(), 7.19)
    );
  }

  static {
    var source = new PortlandMapper();
    source.populateProperties(wps);
  }

  @ParameterizedTest(name = "way {0} should have walk safety factor {1}")
  @MethodSource("cases")
  void walkSafety(OSMWithTags way, double expected) {
    var score = wps.getDataForWay(way);

    var ws = score.walkSafety();
    assertEquals(expected, ws.forward(), delta);
    assertEquals(expected, ws.back(), delta);
  }
}
