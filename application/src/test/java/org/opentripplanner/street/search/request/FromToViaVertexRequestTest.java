package org.opentripplanner.street.search.request;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.model.vertex.LabelledIntersectionVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;

class FromToViaVertexRequestTest {

  private static final Set<Vertex> FROM_VERTICES = Set.of(createVertex("from"));
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
      FROM_VERTICES,
      TO_VERTICES,
      FROM_STOPS,
      TO_STOPS,
      Map.of(VISIT_VIA_LOCATION, VIA_VERTICES)
    );

  @Test
  void from() {
    assertEquals(FROM_VERTICES, FROM_TO_VIA_VERTEX_REQUEST.from());
  }

  @Test
  void fromStops() {
    assertEquals(FROM_STOPS, FROM_TO_VIA_VERTEX_REQUEST.fromStops());
  }

  @Test
  void to() {
    assertEquals(TO_VERTICES, FROM_TO_VIA_VERTEX_REQUEST.to());
  }

  @Test
  void toStops() {
    assertEquals(TO_STOPS, FROM_TO_VIA_VERTEX_REQUEST.toStops());
  }

  @Test
  void findVertices() {
    assertEquals(VIA_VERTICES, FROM_TO_VIA_VERTEX_REQUEST.findVertices(VISIT_VIA_LOCATION));
  }

  @Test
  void testToString() {
    assertEquals(
      "FromToViaVertexRequest{from: [{from lat,lng=1.0,1.0}], " +
      "fromStops: [{test:from-stop lat,lng=51.48,0.0}], " +
      "to: [{to lat,lng=1.0,1.0}], " +
      "toStops: [{test:to-stop lat,lng=51.48,0.0}], " +
      "visitViaLocationVertices: {VisitViaLocation" +
      "{label: Via coordinate, minimumWaitTime: 10m, coordinates: [(51.48, 0.0)]}=[{via lat,lng=1.0,1.0}]" +
      "}}",
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
