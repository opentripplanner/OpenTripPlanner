package org.opentripplanner.street.search.request;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class FromToViaVertexRequestTest {

  private static final GenericLocation FROM = new GenericLocation(
    "from",
    null,
    WgsCoordinate.GREENWICH.latitude(),
    WgsCoordinate.GREENWICH.longitude()
  );
  private static final Set<Vertex> FROM_VERTICES = Set.of(createVertex("from"));
  private static final GenericLocation TO = new GenericLocation(
    "to",
    null,
    WgsCoordinate.GREENWICH.latitude(),
    WgsCoordinate.GREENWICH.longitude()
  );
  private static final Set<Vertex> TO_VERTICES = Set.of(createVertex("to"));
  private static final Set<TransitStopVertex> FROM_STOPS = Set.of(
    createStopVertexForTest("from-stop")
  );
  private static final Set<TransitStopVertex> TO_STOPS = Set.of(createStopVertexForTest("to-stop"));
  private static final VisitViaLocation VISIT_VIA_LOCATION = new VisitViaLocation(
    "Via coordinate",
    Duration.ofMinutes(10),
    List.of(),
    List.of(WgsCoordinate.GREENWICH)
  );
  private static final Set<Vertex> VIA_VERTICES = Set.of(createVertex("via"));
  private static final FromToViaVertexRequest FROM_TO_VIA_VERTEX_REQUEST =
    new FromToViaVertexRequest(
      FROM_STOPS,
      TO_STOPS,
      Map.ofEntries(
        entry(FROM, FROM_VERTICES),
        entry(TO, TO_VERTICES),
        entry(VISIT_VIA_LOCATION.coordinateLocation(), VIA_VERTICES)
      )
    );

  @Test
  void fromStops() {
    assertEquals(FROM_STOPS, FROM_TO_VIA_VERTEX_REQUEST.fromStops());
  }

  @Test
  void toStops() {
    assertEquals(TO_STOPS, FROM_TO_VIA_VERTEX_REQUEST.toStops());
  }

  @Test
  void findVertices() {
    assertEquals(FROM_VERTICES, FROM_TO_VIA_VERTEX_REQUEST.findVertices(FROM));
    assertEquals(TO_VERTICES, FROM_TO_VIA_VERTEX_REQUEST.findVertices(TO));
    assertEquals(
      VIA_VERTICES,
      FROM_TO_VIA_VERTEX_REQUEST.findVertices(VISIT_VIA_LOCATION.coordinateLocation())
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "FromToViaVertexRequest{fromStops: [{test:from-stop lat,lng=51.48,0.0}], " +
      "toStops: [{test:to-stop lat,lng=51.48,0.0}], " +
      "verticesByLocation: [to (51.48, 0.0)=[{to lat,lng=1.0,1.0}], " +
      "Via coordinate (51.48, 0.0)=[{via lat,lng=1.0,1.0}], " +
      "from (51.48, 0.0)=[{from lat,lng=1.0,1.0}]]}",
      FROM_TO_VIA_VERTEX_REQUEST.toString()
    );
  }

  private static Vertex createVertex(String name) {
    return new LabelledIntersectionVertex(name, 1, 1, false, false);
  }

  private static TransitStopVertex createStopVertexForTest(String id) {
    return TransitStopVertex.of()
      .withStop(
        RegularStop.of(new FeedScopedId("test", id), () -> 1)
          .withCoordinate(WgsCoordinate.GREENWICH)
          .build()
      )
      .build();
  }
}
