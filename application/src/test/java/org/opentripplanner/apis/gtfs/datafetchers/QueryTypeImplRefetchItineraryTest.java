package org.opentripplanner.apis.gtfs.datafetchers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.apis.support.graphql.DataFetchingSupport.executionContext;
import static org.opentripplanner.routing.refetch.RentalRefetchTestModel.NETWORK;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.DataFetchingEnvironmentImpl;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.apis.gtfs.TestGtfsGraphQLRequestContext;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.refetch.ItineraryReferenceMapper;
import org.opentripplanner.routing.refetch.ItineraryRefetchService;
import org.opentripplanner.routing.refetch.RentalRefetchTestModel;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.transit.transfer.regular.FixedRegularTransfers;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Focused test for {@link QueryTypeImpl#refetchItinerary}, the GTFS API's single-id refetch query
 * (#7878). It maps the id with the shared
 * {@link org.opentripplanner.routing.refetch.ItineraryReferenceMapper}, decides whether live rental
 * availability is used, and refetches with {@link ItineraryRefetchService} - see
 * {@link ItineraryImplTest} for id exposure.
 */
class QueryTypeImplRefetchItineraryTest {

  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  private static final ZoneId TIME_ZONE = ZoneId.of("Europe/Paris");
  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE,
    TIME_ZONE
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
  private static final ItineraryImpl IMPL_FOR_ID = new ItineraryImpl();

  @Test
  void refetchesItineraryByEncodedId() throws Exception {
    var refetchService = itineraryRefetchService();
    var routeRequest = routeRequest();
    var legRef = legRef();

    var reference = ItineraryReferenceMapper.toItineraryReference(
      refetchService.refetchItinerary(null, null, List.of(legRef), routeRequest),
      routeRequest
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    Object result = SUBJECT.refetchItinerary().get(environment(id, refetchService, routeRequest));

    assertEquals("A ~ BUS tripX 10:00 11:00 ~ B []", itineraryOf(result).toStr());
  }

  /**
   * The refetched itinerary must expose an {@code itineraryId} of its own, so a client can refresh
   * the same trip again. {@code ItineraryImpl#itineraryId} reads the route request from the local
   * context, which this query - unlike {@code planConnection} - has to put there itself.
   */
  @Test
  void refetchedItineraryCarriesTheRouteRequestForItsOwnId() throws Exception {
    var refetchService = itineraryRefetchService();
    var routeRequest = routeRequest();

    var reference = ItineraryReferenceMapper.toItineraryReference(
      refetchService.refetchItinerary(null, null, List.of(legRef()), routeRequest),
      routeRequest
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    Object result = SUBJECT.refetchItinerary().get(environment(id, refetchService, routeRequest));

    var localContext = localContextOf(result);
    assertThat(localContext).containsKey(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY);
    assertEquals(id, itineraryId(itineraryOf(result), localContext));
  }

  /**
   * An itinerary with a rental access and egress, planned with the client's own preferences, is
   * refetched with the same preferences, not with the server's. The client's preferences still
   * rent a vehicle, but a different way than the server's would.
   */
  @ParameterizedTest
  @MethodSource("clientRentalPreferences")
  void refetchesARentalItineraryWithThePreferencesItWasPlannedWith(
    StreetMode mode,
    TraverseMode rentedVehicle,
    Consumer<RoutingPreferencesBuilder> clientPreferences
  ) throws Exception {
    var refetchService = RentalRefetchTestModel.createRefetchService();
    var withServerPreferences = RentalRefetchTestModel.request(mode);
    var planRequest = withServerPreferences
      .copyOf()
      .withPreferences(clientPreferences)
      .buildRequest();
    var planned = refetchRentalItinerary(refetchService, planRequest);
    var id = itineraryId(planned, Map.of(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY, planRequest));

    Object result = SUBJECT.refetchItinerary().get(
      environment(
        id,
        refetchService,
        RentalRefetchTestModel.TRANSIT_ENV.transitService(),
        RouteRequest.defaultValue(),
        Locale.ENGLISH
      )
    );

    var refetched = itineraryOf(result);
    assertEquals(planned.toStr(), refetched.toStr());
    assertTrue(
      refetched
        .legs()
        .stream()
        .anyMatch(
          leg -> leg instanceof StreetLeg streetLeg && streetLeg.getMode() == rentedVehicle
        ),
      refetched.toStr()
    );
    assertEquals(id, itineraryId(refetched, localContextOf(result)));
    // The client's preferences matter: with the server's, the itinerary would be a different one
    var withoutClientPreferences = refetchRentalItinerary(refetchService, withServerPreferences);
    assertNotEquals(planned.toStr(), withoutClientPreferences.toStr());
  }

  /**
   * Like planConnection, a refetch uses live rental availability for a trip planned for now. The
   * server default does not, so the refetch has to override it.
   */
  @Test
  void usesLiveRentalAvailabilityForATripPlannedForNow() throws Exception {
    var departure = ZonedDateTime.now(TIME_ZONE).plusHours(1);
    var envBuilder = TransitTestEnvironment.of(departure.toLocalDate(), TIME_ZONE);
    var stopA = envBuilder.stop("A");
    var stopB = envBuilder.stop("B");
    var departureTime = departure.toLocalTime().toSecondOfDay();
    var transitEnv = envBuilder
      .addTrip(
        TripInput.of("tripNow")
          .addStop(stopA, TimeUtils.timeToStrLong(departureTime))
          .addStop(stopB, TimeUtils.timeToStrLong(departureTime + 600))
      )
      .build();
    var tripData = transitEnv.tripData("tripNow");
    var legRef = new ScheduledTransitLegReference(
      tripData.trip().getId(),
      departure.toLocalDate(),
      0,
      1,
      stopA.getId(),
      stopB.getId(),
      null
    );

    var routeRequest = rentalRefetchRouteRequest(
      legRef,
      itineraryRefetchService(transitEnv),
      transitEnv.transitService(),
      false
    );

    assertTrue(routeRequest.preferences().bike().rental().useAvailabilityInformation());
    assertTrue(routeRequest.preferences().car().rental().useAvailabilityInformation());
    assertTrue(routeRequest.preferences().scooter().rental().useAvailabilityInformation());
  }

  /**
   * For a trip later on, the vehicles available now say little about the ones available then. The
   * server default uses them, so the refetch has to override it.
   */
  @Test
  void doesNotUseLiveRentalAvailabilityForATripLaterOn() throws Exception {
    var routeRequest = rentalRefetchRouteRequest(
      legRef(),
      itineraryRefetchService(),
      TRANSIT_ENV.transitService(),
      true
    );

    assertFalse(routeRequest.preferences().bike().rental().useAvailabilityInformation());
    assertFalse(routeRequest.preferences().car().rental().useAvailabilityInformation());
    assertFalse(routeRequest.preferences().scooter().rental().useAvailabilityInformation());
  }

  /**
   * The id holds no locale. Names in the refetched itinerary must be in the language of the
   * refetch request ({@code Accept-Language}), not the server default: {@code GraphQLUtils} reads
   * the {@code locale} put into the local context before it falls back to {@code Accept-Language}.
   */
  @Test
  void refetchedItineraryUsesTheLocaleOfTheRefetchRequest() throws Exception {
    var refetchService = itineraryRefetchService();
    var serverDefault = routeRequest()
      .copyOf()
      .withPreferences(p -> p.withLocale(Locale.ENGLISH))
      .buildRequest();

    var reference = ItineraryReferenceMapper.toItineraryReference(
      refetchService.refetchItinerary(null, null, List.of(legRef()), serverDefault),
      serverDefault
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    Object result = SUBJECT.refetchItinerary().get(
      environment(id, refetchService, TRANSIT_ENV.transitService(), serverDefault, Locale.GERMAN)
    );

    var localContext = localContextOf(result);
    assertThat(localContext).containsEntry("locale", Locale.GERMAN);
    var routeRequest = (RouteRequest) localContext.get(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY);
    assertEquals(Locale.GERMAN, routeRequest.preferences().locale());
  }

  @Test
  void invalidIdReturnsNull() throws Exception {
    var itinerary = SUBJECT.refetchItinerary().get(
      environment("this-is-not-a-valid-id", itineraryRefetchService(), routeRequest())
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
      Duration.ofSeconds(0),
      Duration.ofSeconds(0),
      -1,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );
    var id = ItineraryReferenceSerializer.encode(reference);

    var itinerary = SUBJECT.refetchItinerary().get(
      environment(id, itineraryRefetchService(), routeRequest())
    );

    assertNull(itinerary);
  }

  /**
   * The client's preferences for each rental mode, each one different from the server's in a way
   * that changes the itinerary, but still rents a vehicle of the mode.
   */
  private static Stream<Arguments> clientRentalPreferences() {
    return Stream.of(
      Arguments.of(
        StreetMode.BIKE_RENTAL,
        TraverseMode.BICYCLE,
        (Consumer<RoutingPreferencesBuilder>) p -> p.withBike(bike -> bike.withSpeed(10))
      ),
      Arguments.of(
        StreetMode.CAR_RENTAL,
        TraverseMode.CAR,
        (Consumer<RoutingPreferencesBuilder>) p ->
          p.withCar(car -> car.withRental(r -> r.withBannedNetworks(Set.of(NETWORK))))
      ),
      Arguments.of(
        StreetMode.SCOOTER_RENTAL,
        TraverseMode.SCOOTER,
        (Consumer<RoutingPreferencesBuilder>) p -> p.withScooter(scooter -> scooter.withSpeed(10))
      )
    );
  }

  private static Itinerary refetchRentalItinerary(
    ItineraryRefetchService refetchService,
    RouteRequest request
  ) {
    return refetchService.refetchItinerary(
      RentalRefetchTestModel.ORIGIN,
      RentalRefetchTestModel.DESTINATION,
      List.of(RentalRefetchTestModel.legRef()),
      request
    );
  }

  /**
   * Refetches the itinerary of {@code legRef}, planned with bike rental access and egress, and
   * returns the route request the refetch used. The server default sets
   * {@code serverUsesAvailability}, so a test can show that the refetch overrides it.
   */
  private static RouteRequest rentalRefetchRouteRequest(
    ScheduledTransitLegReference legRef,
    ItineraryRefetchService refetchService,
    TransitService transitService,
    boolean serverUsesAvailability
  ) throws Exception {
    var planRequest = RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(legRef.fromStopId()))
      .withTo(GenericLocation.fromStopId(legRef.toStopId()))
      .withJourney(journey ->
        journey
          .withAccess(new StreetRequest(StreetMode.BIKE_RENTAL))
          .withEgress(new StreetRequest(StreetMode.BIKE_RENTAL))
      )
      .buildRequest();
    var reference = ItineraryReferenceMapper.toItineraryReference(
      refetchService.refetchItinerary(null, null, List.of(legRef), planRequest),
      planRequest
    );
    var serverDefault = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(p ->
        p
          .withBike(b ->
            b.withRental(r -> r.withUseAvailabilityInformation(serverUsesAvailability))
          )
          .withCar(c -> c.withRental(r -> r.withUseAvailabilityInformation(serverUsesAvailability)))
          .withScooter(s ->
            s.withRental(r -> r.withUseAvailabilityInformation(serverUsesAvailability))
          )
      )
      .buildDefault();

    Object result = SUBJECT.refetchItinerary().get(
      environment(reference.encode(), refetchService, transitService, serverDefault, Locale.ENGLISH)
    );

    return (RouteRequest) localContextOf(result).get(ItineraryImpl.ROUTE_REQUEST_CONTEXT_KEY);
  }

  private static String itineraryId(Itinerary itinerary, Map<String, ?> localContext)
    throws Exception {
    return IMPL_FOR_ID.itineraryId().get(
      DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext())
        .source(itinerary)
        .localContext(localContext)
        .build()
    );
  }

  @SuppressWarnings("unchecked")
  private static Itinerary itineraryOf(Object dataFetcherResult) {
    return ((DataFetcherResult<Itinerary>) dataFetcherResult).getData();
  }

  @SuppressWarnings("unchecked")
  private static Map<String, ?> localContextOf(Object dataFetcherResult) {
    return (Map<String, ?>) ((DataFetcherResult<?>) dataFetcherResult).getLocalContext();
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

  private static ItineraryRefetchService itineraryRefetchService() {
    return itineraryRefetchService(TRANSIT_ENV);
  }

  private static ItineraryRefetchService itineraryRefetchService(
    TransitTestEnvironment transitEnv
  ) {
    var vertexCreationService = new VertexCreationService(
      new VertexLinker(
        GRAPH,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        10,
        false
      )
    );
    var linkingContextFactory = new LinkingContextFactory(
      GRAPH,
      vertexCreationService,
      new TransitServiceResolver(transitEnv.transitService())
    );
    return new ItineraryRefetchService(
      GRAPH,
      transitEnv.transitService(),
      new TransitAlertServiceImpl(),
      FixedRegularTransfers.of().snapshot(),
      null,
      linkingContextFactory,
      StreetLimitationParametersService.DEFAULT,
      GeofencingZoneService.EMPTY
    );
  }

  private static RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(STOP_A.getId()))
      .withTo(GenericLocation.fromStopId(STOP_B.getId()))
      .buildRequest();
  }

  private static DataFetchingEnvironment environment(
    String id,
    ItineraryRefetchService itineraryRefetchService,
    RouteRequest defaultRouteRequest
  ) {
    return environment(
      id,
      itineraryRefetchService,
      TRANSIT_ENV.transitService(),
      defaultRouteRequest,
      defaultRouteRequest.preferences().locale()
    );
  }

  private static DataFetchingEnvironment environment(
    String id,
    ItineraryRefetchService itineraryRefetchService,
    TransitService transitService,
    RouteRequest defaultRouteRequest,
    Locale requestLocale
  ) {
    return DataFetchingEnvironmentImpl.newDataFetchingEnvironment(executionContext())
      .arguments(Map.of("id", id))
      .locale(requestLocale)
      .context(
        new TestGtfsGraphQLRequestContext(
          null,
          transitService,
          new TransitAlertServiceImpl(),
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
          itineraryRefetchService
        )
      )
      .build();
  }
}
