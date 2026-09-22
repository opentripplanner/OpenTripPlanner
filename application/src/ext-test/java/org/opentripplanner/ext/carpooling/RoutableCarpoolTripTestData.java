package org.opentripplanner.ext.carpooling;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Builds {@link RoutableCarpoolTrip}s with a placeholder corridor that serves no stops and covers
 * no area, for tests that exercise trip storage or insertion without a routed street graph.
 */
public final class RoutableCarpoolTripTestData {

  private RoutableCarpoolTripTestData() {}

  public static RoutableCarpoolTrip withDummyVertices(CarpoolTrip trip) {
    var vertices = IntStream.range(0, trip.stops().size())
      .mapToObj(i -> (Vertex) StreetModelForTest.intersectionVertex(60.0 + i * 0.001, 10.0))
      .toList();
    return withVertices(trip, vertices);
  }

  public static RoutableCarpoolTrip withVertices(CarpoolTrip trip, List<Vertex> vertices) {
    int legs = vertices.size() - 1;
    var durations = Collections.nCopies(legs, Duration.ZERO);
    return new RoutableCarpoolTrip(
      trip,
      vertices,
      new CarpoolCorridor(durations, durations, List.of())
    );
  }
}
