package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.carTunnel;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cobblestones;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.footway;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.footwaySidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayPedestrian;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayPedestrianWithSidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayPrimary;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwaySecondary;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayService;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayServiceWithSidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayTertiary;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayTertiaryWithSidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayTrunk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.noSidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.noSidewalkHighSpeed;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.pedestrianTunnel;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.sidewalkBoth;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.southeastLaBonitaWay;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.southwestMayoStreet;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class PortlandMapperTest {

  private static final double EPSILON = 0.1;

  private static final WayPropertySet WPS = new PortlandMapper().buildWayPropertySet();

  static Stream<Arguments> cases() {
    return Stream.of(
      Arguments.of(highwayTrunk(), 7.47),
      Arguments.of(highwayPrimary(), 2.8),
      Arguments.of(highwaySecondary(), 2.49),
      Arguments.of(highwayTertiary(), 2.34),
      Arguments.of(highwayTertiaryWithSidewalk(), 1.56),
      Arguments.of(highwayService(), 1.5),
      Arguments.of(highwayPedestrian(), 1),
      Arguments.of(highwayPedestrianWithSidewalk(), 1),
      Arguments.of(highwayServiceWithSidewalk(), 1.5),
      Arguments.of(southeastLaBonitaWay(), 1.04),
      Arguments.of(southwestMayoStreet(), 1.17),
      Arguments.of(sidewalkBoth(), 1.87),
      Arguments.of(footway(), 1.0),
      Arguments.of(pedestrianTunnel(), 1.0),
      Arguments.of(cobblestones(), 2.02),
      Arguments.of(noSidewalk(), 1.7),
      Arguments.of(carTunnel(), 2.16),
      Arguments.of(footwaySidewalk(), 1.0),
      Arguments.of(fiveLanes(), 3.0),
      Arguments.of(noSidewalkHighSpeed(), 10.14)
    );
  }

  @ParameterizedTest(name = "way {0} should have walk safety factor {1}")
  @MethodSource("cases")
  void walkSafety(OsmWay way, double expected) {
    var props = WPS.getDataForWay(way);

    assertEquals(expected, props.forward().walkSafety(), EPSILON);
    assertEquals(expected, props.backward().walkSafety(), EPSILON);
  }

  public static List<OsmWay> pedestrianCases() {
    return List.of(footway(), footwaySidewalk());
  }

  @ParameterizedTest
  @MethodSource("pedestrianCases")
  void permissions(OsmWay way) {
    var props = WPS.getDataForWay(way);
    assertEquals(PEDESTRIAN, props.forward().getPermission());
  }
}
