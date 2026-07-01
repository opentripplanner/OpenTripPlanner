package org.opentripplanner.service.vehiclerental.street.geofencing;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdFactory.id;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;
import static org.opentripplanner.street.model.StreetModelFactory.streetEdge;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.TestGeofencingZoneBuilder;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.Polygons;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;

class GeofencingZoneApplierTest {

  StreetVertex insideFrognerPark1 = intersectionVertex(59.928667, 10.699322);
  StreetVertex insideFrognerPark2 = intersectionVertex(59.9245634, 10.703902);
  StreetVertex outsideFrognerPark1 = intersectionVertex(59.921212, 10.70637639);
  StreetVertex outsideFrognerPark2 = intersectionVertex(59.91824, 10.70109);
  StreetVertex insideBusinessZone = intersectionVertex(59.95961972533365, 10.76411762080707);
  StreetVertex outsideBusinessZone = intersectionVertex(59.963673477748955, 10.764723087536936);

  StreetEdge insideFrognerPark = streetEdge(insideFrognerPark1, insideFrognerPark2);
  StreetEdge halfInHalfOutFrognerPark = streetEdge(insideFrognerPark2, outsideFrognerPark1);
  StreetEdge businessBorder = streetEdge(insideBusinessZone, outsideBusinessZone);
  final Set<Edge> allEdges = Set.of(insideFrognerPark, halfInHalfOutFrognerPark, businessBorder);
  final GeofencingZoneApplier applier = new GeofencingZoneApplier(
    ignored -> allEdges,
    ignored -> allEdges,
    true
  );

  static GeometryFactory fac = GeometryUtils.getGeometryFactory();
  final GeofencingZone zone = TestGeofencingZoneBuilder.of(id("frogner-park"))
    .withGeometry(Polygons.OSLO_FROGNER_PARK)
    .noDropOff()
    .build();

  MultiPolygon osloMultiPolygon = fac.createMultiPolygon(new Polygon[] { Polygons.OSLO });
  final GeofencingZone businessArea = TestGeofencingZoneBuilder.of(id("oslo"))
    .withGeometry(osloMultiPolygon)
    .asBusinessArea()
    .build();

  @Test
  void interiorVertexHasNoBoundaryExtensions() {
    var result = applier.applyGeofencingZones(List.of(zone, businessArea));

    assertTrue(insideFrognerPark1.listGeofencingBoundaries().isEmpty());
    assertFalse(result.boundaryVertices().contains(insideFrognerPark1));
  }

  @Test
  void boundaryVerticesGetExtensions() {
    var result = applier.applyGeofencingZones(List.of(zone, businessArea));

    // fromv (insideFrognerPark2) should have a boundary extension
    var boundaries = insideFrognerPark2.listGeofencingBoundaries();
    assertFalse(boundaries.isEmpty());

    // boundary vertices tracked for cleanup
    assertTrue(result.boundaryVertices().contains(insideFrognerPark2));
    assertTrue(result.boundaryVertices().contains(outsideFrognerPark1));
    // fromv (inside) should have entering=false (exiting when traversing fromv→tov)
    var boundary = boundaries
      .stream()
      .filter(b -> b.zone().equals(zone))
      .findFirst()
      .orElseThrow();
    assertFalse(boundary.entering());
  }

  @Test
  void zoneIndexContainsAppliedZones() {
    var result = applier.applyGeofencingZones(List.of(zone, businessArea));

    assertNotNull(result.zoneIndex());

    // point inside Frogner Park should find the zone
    var zones = result.zoneIndex().findZonesContaining(insideFrognerPark1.getCoordinate());
    assertTrue(zones.contains(zone));

    // point outside both should find neither restricted zone
    var outsideZones = result.zoneIndex().findZonesContaining(outsideFrognerPark2.getCoordinate());
    assertFalse(outsideZones.contains(zone));
  }
}
