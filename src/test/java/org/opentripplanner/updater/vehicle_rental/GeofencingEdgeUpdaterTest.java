package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension;
import org.opentripplanner.street.model.edge.StreetEdgeRentalExtension.GeofencingZoneExtension;
import org.opentripplanner.street.model.vertex.StreetVertex;

class GeofencingEdgeUpdaterTest {

  StreetVertex insideFrognerPark1 = intersectionVertex(59.928667, 10.699322);
  StreetVertex insideFrognerPark2 = intersectionVertex(59.9245634, 10.703902);
  StreetVertex outsideFrognerPark1 = intersectionVertex(59.921212, 10.70637639);
  StreetVertex outsideFrognerPark2 = intersectionVertex(59.91824, 10.70109);
  StreetVertex insideBusinessZone = intersectionVertex(59.95961972533365, 10.76411762080707);
  StreetVertex outsideBusinessZone = intersectionVertex(59.963673477748955, 10.764723087536936);

  StreetEdge insideFrognerPark = streetEdge(insideFrognerPark1, insideFrognerPark2);
  StreetEdge halfInHalfOutFrognerPark = streetEdge(insideFrognerPark2, outsideFrognerPark1);
  StreetEdge businessBorder = streetEdge(insideBusinessZone, outsideBusinessZone);
  final GeofencingEdgeUpdater updater = new GeofencingEdgeUpdater(ignored ->
    List.of(insideFrognerPark, halfInHalfOutFrognerPark, businessBorder)
  );
  StreetEdge outsideFrognerPark = streetEdge(outsideFrognerPark1, outsideFrognerPark2);
  Polygon frognerPark = GeometryUtils
    .getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        new Coordinate(59.93112978539807, 10.691099320272173),
        new Coordinate(59.92231848097069, 10.691099320272173),
        new Coordinate(59.92231848097069, 10.711758464910503),
        new Coordinate(59.92231848097069, 10.691099320272173),
        new Coordinate(59.93112978539807, 10.691099320272173),
      }
    );
  final GeofencingZone zone = new GeofencingZone(id("frogner-park"), frognerPark, true, false);
  Polygon oslo = GeometryUtils
    .getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        new Coordinate(59.961055202323195, 10.62535658370308),
        new Coordinate(59.889009435700416, 10.62535658370308),
        new Coordinate(59.889009435700416, 10.849791142928694),
        new Coordinate(59.961055202323195, 10.849791142928694),
        new Coordinate(59.961055202323195, 10.62535658370308),
      }
    );
  final GeofencingZone businessArea = new GeofencingZone(id("oslo"), oslo, false, false);

  @Test
  void insideZone() {
    assertEquals(0, insideFrognerPark.getTraversalExtensions().size());

    updater.applyGeofencingZones(List.of(zone, businessArea));

    assertEquals(1, insideFrognerPark.getTraversalExtensions().size());
    var ext = insideFrognerPark.getTraversalExtensions().get(0);
    assertEquals("F", ext.network());

    assertInstanceOf(GeofencingZoneExtension.class, ext);

    var e = (GeofencingZoneExtension) ext;

    assertEquals(zone, e.zone());
  }

  @Test
  void halfInHalfOutZone() {
    assertEquals(0, halfInHalfOutFrognerPark.getTraversalExtensions().size());

    updater.applyGeofencingZones(List.of(zone, businessArea));

    assertEquals(1, halfInHalfOutFrognerPark.getTraversalExtensions().size());
    var ext = insideFrognerPark.getTraversalExtensions().get(0);
    assertEquals("F", ext.network());

    assertInstanceOf(GeofencingZoneExtension.class, ext);

    var e = (GeofencingZoneExtension) ext;

    assertEquals(zone, e.zone());
  }

  @Test
  void outsideZone() {
    assertEquals(0, outsideFrognerPark.getTraversalExtensions().size());
    updater.applyGeofencingZones(List.of(zone, businessArea));
    assertEquals(0, outsideFrognerPark.getTraversalExtensions().size());
  }

  @Test
  void businessAreaBorder() {
    assertEquals(0, businessBorder.getTraversalExtensions().size());
    var updated = updater.applyGeofencingZones(List.of(zone, businessArea));
    assertEquals(Set.of(insideFrognerPark, halfInHalfOutFrognerPark, businessBorder), updated);
    assertEquals(1, businessBorder.getTraversalExtensions().size());

    var ext = (StreetEdgeRentalExtension.BusinessAreaBorder) businessBorder
      .getTraversalExtensions()
      .get(0);
    assertInstanceOf(StreetEdgeRentalExtension.BusinessAreaBorder.class, ext);
  }
}
