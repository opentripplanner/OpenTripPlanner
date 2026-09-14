package org.opentripplanner.apis.transmodel.model.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.executionContext;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Focused test for {@link TripPatternType#id}, the Transmodel API's exposure of a stable,
 * refetchable trip pattern id (#7878). See {@link TripPatternQueryTest} for the refetch side.
 */
class TripPatternTypeTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  private static final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private static final RegularStop STOP_B = ENV_BUILDER.stop("B");

  @Test
  void idIsEncodedFromRouteRequestInLocalContext() {
    var itinerary = simpleBusItinerary();
    var env = environment(
      itinerary,
      Map.of(TripPatternType.ROUTE_REQUEST_CONTEXT_KEY, routeRequest())
    );

    var id = TripPatternType.id(env);

    assertNotNull(id);
    var decoded = ItineraryReferenceSerializer.decode(id);
    assertNotNull(decoded);
    assertEquals(itinerary.legs().getFirst().legReference(), decoded.legReferences().getFirst());
  }

  @Test
  void idIsNullWithoutLocalContext() {
    var env = environment(simpleBusItinerary(), null);

    assertNull(TripPatternType.id(env));
  }

  @Test
  void idIsNullWithoutRouteRequestInLocalContext() {
    var env = environment(simpleBusItinerary(), Map.of("locale", "en"));

    assertNull(TripPatternType.id(env));
  }

  @Test
  void idIsNullForUnsupportedStreetOnlyItinerary() {
    var streetOnlyItinerary = newItinerary(Place.forStop(STOP_A), 0)
      .walk(200, Place.forStop(STOP_B))
      .build();
    var env = environment(
      streetOnlyItinerary,
      Map.of(TripPatternType.ROUTE_REQUEST_CONTEXT_KEY, routeRequest())
    );

    assertNull(TripPatternType.id(env));
  }

  private static Itinerary simpleBusItinerary() {
    return newItinerary(Place.forStop(STOP_A), 0).bus(1, 0, 100, Place.forStop(STOP_B)).build();
  }

  private static RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(STOP_A.getId()))
      .withTo(GenericLocation.fromStopId(STOP_B.getId()))
      .buildRequest();
  }

  private static DataFetchingEnvironment environment(
    Itinerary source,
    Map<String, Object> localContext
  ) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext())
      .source(source)
      .localContext(localContext)
      .build();
  }
}
