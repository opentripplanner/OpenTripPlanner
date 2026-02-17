package org.opentripplanner.service.vehiclerental.street;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Polygons;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;

class GeofencingVertexUpdaterTest {

  StreetVertex insideFrognerPark1 = intersectionVertex(59.928667, 10.699322);
  StreetVertex insideFrognerPark2 = intersectionVertex(59.9245634, 10.703902);
  StreetVertex outsideFrognerPark1 = intersectionVertex(59.921212, 10.70637639);
  StreetVertex outsideFrognerPark2 = intersectionVertex(59.91824, 10.70109);
  StreetVertex insideBusinessZone = intersectionVertex(59.95961972533365, 10.76411762080707);
  StreetVertex outsideBusinessZone = intersectionVertex(59.963673477748955, 10.764723087536936);

  // Edge fully inside Frogner Park
  StreetEdge insideFrognerPark = streetEdge(insideFrognerPark1, insideFrognerPark2);
  // Edge crossing the Frogner Park boundary (inside -> outside)
  StreetEdge crossingFrognerParkBoundary = streetEdge(insideFrognerPark2, outsideFrognerPark1);
  // Edge crossing the business area boundary (inside -> outside)
  StreetEdge businessBorder = streetEdge(insideBusinessZone, outsideBusinessZone);

  final GeofencingVertexUpdater updater = new GeofencingVertexUpdater(ignored ->
    List.of(insideFrognerPark, crossingFrognerParkBoundary, businessBorder)
  );

  static GeometryFactory fac = GeometryUtils.getGeometryFactory();
  final GeofencingZone zone = new GeofencingZone(
    id("frogner-park"),
    null,
    Polygons.OSLO_FROGNER_PARK,
    true,
    false
  );

  MultiPolygon osloMultiPolygon = fac.createMultiPolygon(new Polygon[] { Polygons.OSLO });
  final GeofencingZone businessArea = new GeofencingZone(
    id("oslo"),
    null,
    osloMultiPolygon,
    false,
    false
  );

  @Test
  void boundaryOnlyProcessing_edgeInsideZoneNotMarked() {
    // With boundary-only processing, edges fully inside a zone are NOT marked
    // (restrictions are tracked in routing state instead)
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getToVertex().rentalRestrictions());

    updater.applyGeofencingZones(List.of(zone, businessArea));

    // Edge fully inside zone should still have no restrictions
    // (boundary-only approach doesn't mark interior edges)
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getToVertex().rentalRestrictions());
  }

  @Test
  void boundaryOnlyProcessing_boundaryCrossingEdgeMarked() {
    // Edges crossing the zone boundary should be marked with GeofencingBoundaryExtension
    assertInstanceOf(
      NoRestriction.class,
      crossingFrognerParkBoundary.getToVertex().rentalRestrictions()
    );

    updater.applyGeofencingZones(List.of(zone, businessArea));

    // The "to" vertex of the boundary-crossing edge should have the boundary extension
    // (since we're going from inside to outside, it's an exit)
    var toRestrictions = crossingFrognerParkBoundary.getToVertex().rentalRestrictions();
    assertInstanceOf(GeofencingBoundaryExtension.class, toRestrictions);

    var boundaryExt = (GeofencingBoundaryExtension) toRestrictions;
    assertEquals(zone, boundaryExt.zone());
    // Going from insideFrognerPark2 to outsideFrognerPark1, so exiting (entering = false)
    assertFalse(boundaryExt.entering());
  }

  @Test
  void businessAreaBorder() {
    assertInstanceOf(NoRestriction.class, businessBorder.getFromVertex().rentalRestrictions());

    var result = updater.applyGeofencingZones(List.of(zone, businessArea));

    // Business area borders use the old BusinessAreaBorder extension
    var ext = businessBorder.getFromVertex().rentalRestrictions();
    assertInstanceOf(BusinessAreaBorder.class, ext);

    // Result should contain modified edges
    assertFalse(result.modifiedEdges().isEmpty());
  }

  @Test
  void geofencingZoneIndexCreated() {
    var result = updater.applyGeofencingZones(List.of(zone, businessArea));

    assertNotNull(result.index());
    assertFalse(result.index().isEmpty());
    assertEquals(2, result.index().size());

    // Index should be able to find zones containing points
    var zonesAtFrogner = result.index().getZonesContaining(insideFrognerPark1.getCoordinate());
    assertTrue(zonesAtFrogner.contains(zone));
  }
}
