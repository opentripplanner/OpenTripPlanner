package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
import org.opentripplanner.service.vehiclerental.street.BusinessAreaBorder;
import org.opentripplanner.service.vehiclerental.street.GeofencingZoneExtension;
import org.opentripplanner.service.vehiclerental.street.NoRestriction;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;

class GeofencingVertexUpdaterTest {

  StreetVertex insideFrognerPark1 = intersectionVertex(59.928667, 10.699322);
  StreetVertex insideFrognerPark2 = intersectionVertex(59.9245634, 10.703902);
  StreetVertex outsideFrognerPark1 = intersectionVertex(59.921212, 10.70637639);
  StreetVertex outsideFrognerPark2 = intersectionVertex(59.91824, 10.70109);
  StreetVertex insideBusinessZone = intersectionVertex(59.95961972533365, 10.76411762080707);
  StreetVertex outsideBusinessZone = intersectionVertex(59.963673477748955, 10.764723087536936);

  StreetEdge insideFrognerPark = streetEdge(insideFrognerPark1, insideFrognerPark2);
  StreetEdge halfInHalfOutFrognerPark = streetEdge(insideFrognerPark2, outsideFrognerPark1);
  StreetEdge businessBorder = streetEdge(insideBusinessZone, outsideBusinessZone);
  final GeofencingVertexUpdater updater = new GeofencingVertexUpdater(ignored ->
    List.of(insideFrognerPark, halfInHalfOutFrognerPark, businessBorder)
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
  void insideZone() {
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());

    updater.applyGeofencingZones(List.of(zone, businessArea));

    var ext = insideFrognerPark.getFromVertex().rentalRestrictions();

    assertInstanceOf(GeofencingZoneExtension.class, ext);

    var e = (GeofencingZoneExtension) ext;

    assertEquals(zone, e.zone());
  }

  @Test
  void halfInHalfOutZone() {
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());

    updater.applyGeofencingZones(List.of(zone, businessArea));

    var ext = insideFrognerPark.getFromVertex().rentalRestrictions();

    assertInstanceOf(GeofencingZoneExtension.class, ext);

    var e = (GeofencingZoneExtension) ext;

    assertEquals(zone, e.zone());
  }

  @Test
  void outsideZone() {
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());
    updater.applyGeofencingZones(List.of(zone, businessArea));
    assertInstanceOf(
      GeofencingZoneExtension.class,
      insideFrognerPark.getFromVertex().rentalRestrictions()
    );
  }

  @Test
  void businessAreaBorder() {
    assertInstanceOf(NoRestriction.class, insideFrognerPark.getFromVertex().rentalRestrictions());
    var updated = updater.applyGeofencingZones(List.of(zone, businessArea));

    assertEquals(3, updated.size());

    var ext = (BusinessAreaBorder) businessBorder.getFromVertex().rentalRestrictions();
    assertInstanceOf(BusinessAreaBorder.class, ext);
  }
}
