package org.opentripplanner.graph_builder.module.osm.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.module.osm.StreetEdgePair;
import org.opentripplanner.graph_builder.services.osm.EdgeNamer;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.test.support.GeoJsonIo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SidewalkNamerTest {

  private static final I18NString SIDEWALK = I18NString.of("sidewalk");
  private static final Logger LOG = LoggerFactory.getLogger(SidewalkNamerTest.class);

  @Test
  void postprocess() {
    var builder = new ModelBuilder();
    var sidewalk = builder.addUnamedSidewalk(
      new WgsCoordinate(33.75029, -84.39198),
      new WgsCoordinate(33.74932, -84.39275)
    );

    LOG.info(
      "Geometry of {}: {}",
      sidewalk.edge.getName(),
      GeoJsonIo.toUrl(sidewalk.edge.getGeometry())
    );

    var pryorStreet = builder.addStreetEdge(
      "Pryor Street",
      new WgsCoordinate(33.75032, -84.39190),
      new WgsCoordinate(33.74924, -84.39275)
    );

    LOG.info(
      "Geometry of {}: {}",
      pryorStreet.edge.getName(),
      GeoJsonIo.toUrl(pryorStreet.edge.getGeometry())
    );

    assertNotEquals(sidewalk.edge.getName(), pryorStreet.edge.getName());
    builder.postProcess(new SidewalkNamer());
    assertEquals(sidewalk.edge.getName(), pryorStreet.edge.getName());
  }

  private static class ModelBuilder {

    private final List<EdgePair> pairs = new ArrayList<>();

    EdgePair addUnamedSidewalk(WgsCoordinate... coordinates) {
      var edge = edgeBuilder(coordinates)
        .withName(SIDEWALK)
        .withPermission(StreetTraversalPermission.PEDESTRIAN)
        .withBogusName(true)
        .buildAndConnect();

      var way = WayTestData.footwaySidewalk();
      assertTrue(way.isSidewalk());
      var p = new EdgePair(way, edge);
      pairs.add(p);
      return p;
    }

    EdgePair addStreetEdge(String name, WgsCoordinate... coordinates) {
      var edge = edgeBuilder(coordinates)
        .withName(I18NString.of(name))
        .withPermission(StreetTraversalPermission.ALL)
        .buildAndConnect();
      var way = WayTestData.highwayTertiary();
      way.addTag("name", name);
      assertFalse(way.isSidewalk());
      assertTrue(way.isNamed());
      var p = new EdgePair(way, edge);
      pairs.add(p);
      return p;
    }

    void postProcess(EdgeNamer namer) {
      pairs.forEach(p -> namer.recordEdges(p.way, new StreetEdgePair(p.edge, null)));
      namer.postprocess();
    }

    private static StreetEdgeBuilder<?> edgeBuilder(WgsCoordinate... c) {
      var coordinates = Arrays.stream(c).toList();
      var ls = GeometryUtils.makeLineString(c);
      return new StreetEdgeBuilder<>()
        .withFromVertex(
          StreetModelForTest.intersectionVertex(coordinates.getFirst().asJtsCoordinate())
        )
        .withToVertex(
          StreetModelForTest.intersectionVertex(coordinates.getLast().asJtsCoordinate())
        )
        .withGeometry(ls);
    }
  }

  private record EdgePair(OSMWay way, StreetEdge edge) {}
}
