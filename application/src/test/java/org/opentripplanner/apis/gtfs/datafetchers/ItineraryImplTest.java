package org.opentripplanner.apis.gtfs.datafetchers;

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
 * Focused test for {@link ItineraryImpl#id}, the GTFS API's exposure of a stable, refetchable
 * itinerary id (#7878). See {@link org.opentripplanner.routing.refetch.ItineraryReferenceMapper}
 * for the shared core this delegates to.
 */
class ItineraryImplTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  private static final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private static final RegularStop STOP_B = ENV_BUILDER.stop("B");

  private static final ItineraryImpl IMPL = new ItineraryImpl();

  @Test
  void idIsEncodedFromRouteRequestInLocalContext() throws Exception {
    var itinerary = simpleBusItinerary();
    var env = environment(
      itinerary,
      Map.of(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY, routeRequest())
    );

    var id = IMPL.id().get(env);

    assertNotNull(id);
    var decoded = ItineraryReferenceSerializer.decode(id);
    assertNotNull(decoded);
    assertEquals(itinerary.legs().getFirst().legReference(), decoded.legReferences().getFirst());
  }

  @Test
  void idIsNullWithoutLocalContext() throws Exception {
    var env = environment(simpleBusItinerary(), null);

    assertNull(IMPL.id().get(env));
  }

  @Test
  void idIsNullWithoutRouteRequestInLocalContext() throws Exception {
    var env = environment(simpleBusItinerary(), Map.of("locale", "en"));

    assertNull(IMPL.id().get(env));
  }

  @Test
  void idIsNullForUnsupportedStreetOnlyItinerary() throws Exception {
    var streetOnlyItinerary = newItinerary(Place.forStop(STOP_A), 0)
      .walk(200, Place.forStop(STOP_B))
      .build();
    var env = environment(
      streetOnlyItinerary,
      Map.of(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY, routeRequest())
    );

    assertNull(IMPL.id().get(env));
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
