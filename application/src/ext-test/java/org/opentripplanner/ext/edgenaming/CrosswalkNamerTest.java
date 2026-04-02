package org.opentripplanner.ext.edgenaming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.edgenaming.StreetEdgeBuilderFactory.edgeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.StreetEdge;

class CrosswalkNamerTest {

  private static final OsmWay CROSSWALK = new OsmWay();
  private static final OsmWay SIDEWALK_TO_CROSSWALK = new OsmWay();
  private static final OsmWay OTHER_SIDEWALK1_TO_CROSSWALK = new OsmWay();
  private static final OsmWay OTHER_SIDEWALK2_TO_CROSSWALK = new OsmWay();
  private static final OsmWay STREET = new OsmWay();
  private static final OsmWay SERVICE_ROAD = new OsmWay();
  private static final OsmWay TURN_LANE = new OsmWay();
  private static final OsmWay MOTORWAY_RAMP = new OsmWay();
  private static final OsmWay OTHER_STREET = new OsmWay();

  @BeforeAll
  static void setUp() {
    CROSSWALK.addTag("highway", "footway");
    CROSSWALK.addTag("footway", "crossing");
    CROSSWALK.addTag("crossing:markings", "yes");
    CROSSWALK.getNodeRefs().add(new long[] { 10001, 10002, 10003, 10004 });

    SIDEWALK_TO_CROSSWALK.addTag("highway", "footway");
    SIDEWALK_TO_CROSSWALK.addTag("footway", "sidewalk");
    SIDEWALK_TO_CROSSWALK.getNodeRefs().add(new long[] { 10000, 10001 });

    OTHER_SIDEWALK1_TO_CROSSWALK.addTag("highway", "footway");
    OTHER_SIDEWALK1_TO_CROSSWALK.addTag("footway", "sidewalk");
    OTHER_SIDEWALK1_TO_CROSSWALK.getNodeRefs().add(new long[] { 10004, 10005 });

    OTHER_SIDEWALK2_TO_CROSSWALK.addTag("highway", "footway");
    OTHER_SIDEWALK2_TO_CROSSWALK.addTag("footway", "sidewalk");
    OTHER_SIDEWALK2_TO_CROSSWALK.getNodeRefs().add(new long[] { 10004, 10006 });

    STREET.setId(50001);
    STREET.addTag("highway", "primary");
    STREET.addTag("name", "3rd Street");
    STREET.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });

    OTHER_STREET.setId(50002);
    OTHER_STREET.addTag("highway", "primary");
    OTHER_STREET.addTag("name", "Other Street");
    OTHER_STREET.getNodeRefs().add(new long[] { 30001, 30002, 30003, 30004, 30005 });

    // Reusing ids and nodes for SERVICE_ROAD, MOTORWAY_RAMP and TURN_LANE as they are not used together with STREET.
    SERVICE_ROAD.setId(50001);
    SERVICE_ROAD.addTag("highway", "service");
    SERVICE_ROAD.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });

    TURN_LANE.setId(50001);
    TURN_LANE.addTag("highway", "primary_link");
    TURN_LANE.addTag("oneway", "yes");
    TURN_LANE.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });

    MOTORWAY_RAMP.setId(50001);
    MOTORWAY_RAMP.addTag("highway", "motorway_link");
    MOTORWAY_RAMP.addTag("oneway", "yes");
    MOTORWAY_RAMP.addTag("turn:lanes", "right");
    MOTORWAY_RAMP.getNodeRefs().add(new long[] { 20001, 20002, 20003, 10002, 20004, 20005 });
  }

  @Test
  void testGetIntersectingStreet() {
    var intersectingStreet = CrosswalkNamer.getIntersectingStreet(
      CROSSWALK,
      List.of(STREET, OTHER_STREET)
    );
    assertTrue(intersectingStreet.isPresent());
    assertEquals(50001, intersectingStreet.get().getId());

    var intersectingStreet2 = CrosswalkNamer.getIntersectingStreet(
      CROSSWALK,
      List.of(OTHER_STREET)
    );
    assertFalse(intersectingStreet2.isPresent());
  }

  @ParameterizedTest
  @MethodSource("streetTypes")
  void recordEdgesAndFinalizeNames(OsmWay crossStreet, String name) {
    var builder = new ModelBuilder();
    var crosswalk = builder.addWay(
      CROSSWALK,
      new WgsCoordinate(33.9527949, -83.9954059),
      new WgsCoordinate(33.9527436, -83.9954582)
    );
    var sidewalkToCrosswalk = builder.addWay(
      SIDEWALK_TO_CROSSWALK,
      new WgsCoordinate(33.9527949, -83.9954059),
      new WgsCoordinate(33.9526896, -83.9955389)
    );
    var sidewalk1ToCrosswalk = builder.addWay(
      OTHER_SIDEWALK1_TO_CROSSWALK,
      new WgsCoordinate(33.9528618, -83.9953530),
      new WgsCoordinate(33.9527436, -83.9954582)
    );
    var sidewalk2ToCrosswalk = builder.addWay(
      OTHER_SIDEWALK2_TO_CROSSWALK,
      new WgsCoordinate(33.9528618, -83.9953630),
      new WgsCoordinate(33.9527436, -83.9954582)
    );
    builder.addWay(
      crossStreet,
      new WgsCoordinate(33.9528839, -83.9956473),
      new WgsCoordinate(33.9526837, -83.9953494)
    );
    builder.addWay(
      OTHER_STREET,
      new WgsCoordinate(33.9528839, -83.9956473),
      new WgsCoordinate(33.9521700, -83.9954001)
    );

    CrosswalkNamer namer = new CrosswalkNamer();
    builder.recordEdges(namer);
    assertEquals(1, namer.getUnnamedCrosswalks().size());

    namer.finalizeNames();
    assertEquals(String.format("crosswalk over %s", name), crosswalk.edge.getName().toString());
    assertFalse(crosswalk.edge.nameIsDerived());
    assertEquals(
      String.format("crosswalk over %s", name),
      sidewalkToCrosswalk.edge.getName().toString()
    );
    assertFalse(crosswalk.edge.nameIsDerived());

    // These sidewalks should not be renamed because they are adjacent
    // to the same crosswalk on the same side.
    assertTrue(sidewalk1ToCrosswalk.edge.nameIsDerived());
    assertTrue(sidewalk2ToCrosswalk.edge.nameIsDerived());
  }

  private static Stream<Arguments> streetTypes() {
    return Stream.of(
      Arguments.of(STREET, STREET.getTag("name")),
      Arguments.of(SERVICE_ROAD, "service road"),
      Arguments.of(TURN_LANE, "turn lane"),
      Arguments.of(MOTORWAY_RAMP, "freeway ramp")
    );
  }

  private static class ModelBuilder {

    private final List<EdgePair> pairs = new ArrayList<>();

    EdgePair addWay(OsmWay way, WgsCoordinate... coordinates) {
      var edge = edgeBuilder(coordinates)
        .withPermission(
          way.isFootway() ? StreetTraversalPermission.PEDESTRIAN : StreetTraversalPermission.CAR
        )
        .withName(Objects.requireNonNullElse(way.getAssumedName(), I18NString.of("path")))
        .withBogusName(!way.isNamed())
        .buildAndConnect();

      var p = new EdgePair(way, edge);
      pairs.add(p);
      return p;
    }

    void recordEdges(EdgeNamer namer) {
      pairs.forEach(p ->
        namer.recordEdges(
          p.way,
          new StreetEdgePair(p.edge, null),
          new OsmDatabase(DataImportIssueStore.NOOP)
        )
      );
    }
  }

  private record EdgePair(OsmWay way, StreetEdge edge) {}
}
