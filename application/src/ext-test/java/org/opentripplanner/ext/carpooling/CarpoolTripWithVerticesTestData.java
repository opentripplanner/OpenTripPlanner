package org.opentripplanner.ext.carpooling;

import java.util.stream.IntStream;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Builds {@link CarpoolTripWithVertices} with free-standing dummy vertices, for tests that exercise
 * trip storage without a real street graph.
 */
public final class CarpoolTripWithVerticesTestData {

  private CarpoolTripWithVerticesTestData() {}

  public static CarpoolTripWithVertices withDummyVertices(CarpoolTrip trip) {
    var vertices = IntStream.range(0, trip.stops().size())
      .mapToObj(i -> (Vertex) StreetModelForTest.intersectionVertex(60.0 + i * 0.001, 10.0))
      .toList();
    return new CarpoolTripWithVertices(trip, vertices);
  }
}
