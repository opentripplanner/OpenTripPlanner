package org.opentripplanner.apis.gtfs.datafetchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.executionContext;

import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.apis.gtfs.TestGtfsGraphQLRequestContext;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.refetch.ItineraryReferenceMapper;
import org.opentripplanner.routing.refetch.RefetchItineraryService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.TransferServiceTestFactory;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * Focused test for {@link QueryTypeImpl#itinerary}, the GTFS API's single-id refetch query
 * (#7878). Delegates entirely to the shared
 * {@link org.opentripplanner.routing.refetch.ItineraryReferenceMapper}/
 * {@link RefetchItineraryService} core - see {@link ItineraryImplTest} for id exposure.
 */
class QueryTypeImplItineraryTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  private static final RegularStop STOP_A = ENV_BUILDER.stop("A");
  private static final RegularStop STOP_B = ENV_BUILDER.stop("B");
  private static final TransitTestEnvironment TRANSIT_ENV = ENV_BUILDER.addTrip(
    TripInput.of("tripX").addStop(STOP_A, "10:00").addStop(STOP_B, "11:00")
  ).build();

  private static final Graph GRAPH = new Graph();

  static {
    GRAPH.addVertex(
      TransitStopVertex.of().withId(STOP_A.getId()).withCoordinate(STOP_A.getCoordinate()).build()
    );
    GRAPH.addVertex(
      TransitStopVertex.of().withId(STOP_B.getId()).withCoordinate(STOP_B.getCoordinate()).build()
    );
    GRAPH.index();
  }

  private static final QueryTypeImpl SUBJECT = new QueryTypeImpl();

  @Test
  void refetchesItineraryByEncodedId() throws Exception {
    var refetchService = refetchItineraryService();
    var routeRequest = routeRequest();
    var legRef = legRef();

    var reference = ItineraryReferenceMapper.toItineraryReference(
      refetchService.refetchItinerary(null, null, List.of(legRef), routeRequest),
      routeRequest
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    var itinerary = SUBJECT.itinerary().get(environment(id, refetchService, routeRequest));

    assertEquals("A ~ BUS tripX 10:00 11:00 ~ B []", itinerary.toStr());
  }

  @Test
  void invalidIdReturnsNull() throws Exception {
    var itinerary = SUBJECT.itinerary().get(
      environment("this-is-not-a-valid-id", refetchItineraryService(), routeRequest())
    );

    assertNull(itinerary);
  }

  @Test
  void invalidFiniteWalkSpeedReturnsNullInsteadOfThrowing() throws Exception {
    var reference = new ItineraryReference(
      List.of(legRef()),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      DurationForEnum.of(TransitMode.class).build(),
      DurationForEnum.of(TransitMode.class).build(),
      -1,
      2.0,
      DurationForEnum.of(StreetMode.class).build(),
      false
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    var itinerary = SUBJECT.itinerary().get(
      environment(id, refetchItineraryService(), routeRequest())
    );

    assertNull(itinerary);
  }

  private ScheduledTransitLegReference legRef() {
    var tripData = TRANSIT_ENV.tripData("tripX");
    var stops = tripData.tripPattern().getStops();
    return new ScheduledTransitLegReference(
      tripData.trip().getId(),
      SERVICE_DATE,
      stops.indexOf(STOP_A),
      stops.indexOf(STOP_B),
      STOP_A.getId(),
      STOP_B.getId(),
      null
    );
  }

  private static RefetchItineraryService refetchItineraryService() {
    var vertexCreationService = new VertexCreationService(
      new VertexLinker(
        GRAPH,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        10,
        false
      )
    );
    var linkingContextFactory = new LinkingContextFactory(GRAPH, vertexCreationService);
    return new RefetchItineraryService(
      GRAPH,
      TRANSIT_ENV.transitService(),
      new TransitAlertServiceImpl(),
      TransferServiceTestFactory.defaultTransferService(),
      null,
      linkingContextFactory,
      StreetLimitationParametersService.DEFAULT
    );
  }

  private static RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(STOP_A.getId()))
      .withTo(GenericLocation.fromStopId(STOP_B.getId()))
      .buildRequest();
  }

  private static graphql.schema.DataFetchingEnvironment environment(
    String id,
    RefetchItineraryService refetchItineraryService,
    RouteRequest defaultRouteRequest
  ) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext())
      .arguments(Map.of("id", id))
      .context(
        new TestGtfsGraphQLRequestContext(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          defaultRouteRequest,
          refetchItineraryService
        )
      )
      .build();
  }
}
