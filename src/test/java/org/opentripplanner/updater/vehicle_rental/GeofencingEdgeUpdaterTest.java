package org.opentripplanner.updater.vehicle_rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;
import static org.opentripplanner.transit.model._data.TransitModelForTest.id;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.vehicle_rental.GeofencingZone;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;

class GeofencingEdgeUpdaterTest {

  StreetVertex insideFrognerPark1 = intersectionVertex(59.928667, 10.699322);
  StreetVertex insideFrognerPark2 = intersectionVertex(59.9245634, 10.703902);
  StreetVertex outsideFrognerPark = intersectionVertex(59.921212, 10.70637639);

  StreetEdge insideFrognerPark = streetEdge(insideFrognerPark1, insideFrognerPark2);
  StreetEdge halfInHalfOutFrognerPark = streetEdge(insideFrognerPark2, outsideFrognerPark);

  Polygon frognerPark = GeometryUtils.getGeometryFactory().createPolygon(
    new Coordinate[]{
      new Coordinate(
        59.93112978539807,
        10.691099320272173
      ),
      new Coordinate(
        59.92231848097069,

        10.691099320272173
      ),
      new Coordinate(
        59.92231848097069,
        10.711758464910503
      ),
      new Coordinate(
        59.92231848097069,
        10.691099320272173
      ),
      new Coordinate(
        59.93112978539807,
        10.691099320272173
      )
    }
  );

  @Test
  void apply() {

    assertEquals(0, insideFrognerPark.getTraversalExtensions().size());

    var updater = new GeofencingEdgeUpdater((ignored) -> List.of(insideFrognerPark, halfInHalfOutFrognerPark));


    var zone = new GeofencingZone(id("frogner-park"), frognerPark, true, false);
    updater.applyGeofencingZones(List.of(zone));

    assertEquals(1, insideFrognerPark.getTraversalExtensions().size());
  }

}