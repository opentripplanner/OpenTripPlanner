package org.opentripplanner.routing.refetch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.itineraryreference.BikeRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.BikeWalkingPreferences;
import org.opentripplanner.model.plan.itineraryreference.CarRentalPreferences;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.model.plan.itineraryreference.RentalDestinationPolicy;
import org.opentripplanner.model.plan.itineraryreference.RentalNetworks;
import org.opentripplanner.model.plan.itineraryreference.RidingPreferences;
import org.opentripplanner.model.plan.itineraryreference.ScooterRentalPreferences;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.RoutingPreferencesBuilder;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.api.request.preference.VehicleRentalPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.transit.transfer.regular.FixedRegularTransfers;

/**
 * UC1 acceptance test for a single scheduled-transit itinerary without access, egress or
 * transfers.
 *
 * <p>Proves the chain:
 * {@code Itinerary -> ItineraryReference -> encode -> decode -> ItineraryRefetchService ->
 * same scheduled transit itinerary}.
 */
class ItineraryReferenceMapperTest {

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
    // Register stop vertices (no street edges) so ItineraryRefetchService.findVertex(...) can
    // resolve the board/alight stops; access/egress street searches still fail (no edges to
    // reach them), which is what refetchUsesDecodedFromAndToLocations relies on.
    GRAPH.addVertex(
      TransitStopVertex.of().withId(STOP_A.getId()).withCoordinate(STOP_A.getCoordinate()).build()
    );
    GRAPH.addVertex(
      TransitStopVertex.of().withId(STOP_B.getId()).withCoordinate(STOP_B.getCoordinate()).build()
    );
    GRAPH.index();
  }

  /** The values of {@link #withClientRentalPreferences}, as stored in a reference. */
  private static final BikeRentalPreferences CLIENT_BIKE_RENTAL = new BikeRentalPreferences(
    new RidingPreferences(
      7.5,
      3.5,
      VehicleRoutingOptimizeType.TRIANGLE,
      new TimeSlopeSafetyTriangle(0.2, 0.3, 0.5)
    ),
    new BikeWalkingPreferences(1.2, 4.0, Duration.ofSeconds(45), Cost.costOfSeconds(90)),
    new RentalNetworks(Set.of("bike-a", "bike-b"), Set.of("bike-c")),
    new RentalDestinationPolicy(true, Cost.costOfSeconds(120))
  );
  private static final CarRentalPreferences CLIENT_CAR_RENTAL = new CarRentalPreferences(
    4.5,
    new RentalNetworks(Set.of("car-a"), Set.of("car-b"))
  );
  private static final ScooterRentalPreferences CLIENT_SCOOTER_RENTAL =
    new ScooterRentalPreferences(
      new RidingPreferences(
        6.5,
        2.5,
        VehicleRoutingOptimizeType.FLAT_STREETS,
        new TimeSlopeSafetyTriangle(0.6, 0.1, 0.3)
      ),
      new RentalNetworks(Set.of("scooter-a"), Set.of("scooter-b")),
      new RentalDestinationPolicy(true, Cost.costOfSeconds(60))
    );

  @Test
  void roundTripSingleScheduledTransitLeg() {
    var refetchService = createRefetchService();
    var routeRequest = routeRequest();

    var originalItinerary = refetchService.refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest
    );

    var reference = ItineraryReferenceMapper.toItineraryReference(originalItinerary, routeRequest);

    var encoded = ItineraryReferenceSerializer.encode(reference);
    assertNotNull(encoded);
    var decodedReference = ItineraryReferenceSerializer.decode(encoded);
    assertNotNull(decodedReference);

    var refetchRouteRequest = ItineraryReferenceMapper.toRouteRequest(
      decodedReference,
      routeRequest
    );
    var refetchedItinerary = refetchService.refetchItinerary(
      decodedReference.from(),
      decodedReference.to(),
      decodedReference.legReferences(),
      refetchRouteRequest
    );

    assertEquals(originalItinerary.toStr(), refetchedItinerary.toStr());
    assertEquals("A ~ BUS tripX 10:00 11:00 ~ B []", refetchedItinerary.toStr());
  }

  @Test
  void itineraryWithoutTransitLegReferenceIsUnsupported() {
    var from = Place.forStop(STOP_A);
    var to = Place.forStop(STOP_B);

    var streetOnlyItinerary = newItinerary(from, 0).walk(200, to).build();

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(streetOnlyItinerary, routeRequest())
    );
  }

  @Test
  void ignoresStreetLegsWhenExtractingTransitSpine() {
    var boardPlace = Place.forStop(STOP_A);
    var alightPlace = Place.forStop(STOP_B);

    // WALK -> BUS -> WALK: the walk (access/egress) legs have no LegReference and must be
    // ignored rather than making the whole itinerary unsupported.
    var itinerary = newItinerary(boardPlace, 0)
      .walk(200, boardPlace)
      .bus(1, 200, 300, alightPlace)
      .walk(200, alightPlace)
      .build();

    var expectedLegReference = itinerary.legs().get(1).legReference();
    assertNotNull(expectedLegReference);

    var reference = ItineraryReferenceMapper.toItineraryReference(itinerary, routeRequest());

    assertThat(reference.legReferences()).containsExactly(expectedLegReference);
  }

  @Test
  void toItineraryReferenceCapturesRoutingContextFromRouteRequest() {
    var refetchService = createRefetchService();
    var itinerary = refetchService.refetchItinerary(null, null, List.of(legRef()), routeRequest());

    var customRouteRequest = routeRequest()
      .copyOf()
      .withJourney(journey ->
        journey
          .withAccess(new StreetRequest(StreetMode.WALK))
          .withEgress(new StreetRequest(StreetMode.WALK))
          .withWheelchair(true)
      )
      .withPreferences(preferences ->
        preferences
          .withTransit(transit ->
            transit.withDefaultBoardSlackSec(60).withDefaultAlightSlackSec(30)
          )
          .withWalk(walk -> walk.withSpeed(2.5).withReluctance(3.0).withSafetyFactor(0.5))
      )
      .buildRequest();

    var reference = ItineraryReferenceMapper.toItineraryReference(itinerary, customRouteRequest);

    assertEquals(GenericLocation.fromStopId(STOP_A.getId()), reference.from());
    assertEquals(GenericLocation.fromStopId(STOP_B.getId()), reference.to());
    assertEquals(StreetMode.WALK, reference.accessMode());
    assertEquals(StreetMode.WALK, reference.egressMode());
    assertEquals(StreetMode.WALK, reference.transferMode());
    assertTrue(reference.wheelchair());
    assertEquals(Duration.ofSeconds(60), reference.boardSlack());
    assertEquals(Duration.ofSeconds(30), reference.alightSlack());
    assertEquals(2.5, reference.walkSpeed());
    assertEquals(3.0, reference.walkReluctance());
    assertEquals(0.5, reference.walkSafetyFactor());
  }

  /**
   * Per-mode board/alight slack overrides ({@code TransitMode.BUS}/{@code TransitMode.RAIL}) are
   * server-only: no supported API lets a client set them.
   * {@link ItineraryReference#boardSlack()}/{@link ItineraryReference#alightSlack()} are plain
   * {@link Duration}s, not {@code DurationForEnum}s, so they only capture the client-settable
   * default, per {@link ItineraryReference}'s class doc.
   */
  @Test
  void toItineraryReferenceDropsServerOnlyPerModeSlackOverrides() {
    var refetchService = createRefetchService();
    var itinerary = refetchService.refetchItinerary(null, null, List.of(legRef()), routeRequest());

    var routeRequestWithOverrides = routeRequest()
      .copyOf()
      .withPreferences(preferences ->
        preferences.withTransit(transit ->
          transit
            .withBoardSlack(b -> b.withDefaultSec(60).with(TransitMode.BUS, Duration.ofSeconds(90)))
            .withAlightSlack(b ->
              b.withDefaultSec(30).with(TransitMode.RAIL, Duration.ofSeconds(45))
            )
        )
      )
      .buildRequest();

    var reference = ItineraryReferenceMapper.toItineraryReference(
      itinerary,
      routeRequestWithOverrides
    );

    assertEquals(Duration.ofSeconds(60), reference.boardSlack());
    assertEquals(Duration.ofSeconds(30), reference.alightSlack());

    // Rebuilding with a *different* default request that has its own per-mode overrides proves
    // those overrides - not the ones from routeRequestWithOverrides above - are what ends up on
    // the rebuilt request, i.e. overrides are always read fresh from the current server config.
    var differentDefaultRequest = routeRequest()
      .copyOf()
      .withPreferences(preferences ->
        preferences.withTransit(transit ->
          transit.withBoardSlack(b ->
            b.withDefaultSec(60).with(TransitMode.BUS, Duration.ofSeconds(999))
          )
        )
      )
      .buildRequest();

    var rebuilt = ItineraryReferenceMapper.toRouteRequest(reference, differentDefaultRequest);

    assertEquals(
      Duration.ofSeconds(999),
      rebuilt.preferences().transit().boardSlack().valueOf(TransitMode.BUS)
    );
  }

  /**
   * The encoded values are the ones in effect when the itinerary was planned, so they replace the
   * current server defaults on refetch. Server-only settings that are not encoded, such as
   * {@code maxAccessEgressDuration}, keep the value of the current default request.
   */
  @Test
  void toRouteRequestOverlaysEncodedValuesOnTheCurrentDefaultRequest() {
    var reference = new ItineraryReference(
      List.of(legRef()),
      GenericLocation.fromStopId(STOP_A.getId()),
      GenericLocation.fromStopId(STOP_B.getId()),
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ofSeconds(60),
      Duration.ofSeconds(30),
      2.5,
      3.0,
      0.5,
      true,
      null,
      null,
      null
    );
    var currentDefault = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(journey -> journey.withWheelchair(false))
      .withPreferences(preferences ->
        preferences
          .withTransit(transit ->
            transit.withDefaultBoardSlackSec(120).withDefaultAlightSlackSec(90)
          )
          .withWalk(walk -> walk.withSpeed(1.0).withReluctance(5.0))
          .withStreet(street ->
            street.withAccessEgress(accessEgress ->
              accessEgress.withMaxDuration(Duration.ofMinutes(12), Map.of())
            )
          )
      )
      .buildDefault();

    var rebuilt = ItineraryReferenceMapper.toRouteRequest(reference, currentDefault);

    var preferences = rebuilt.preferences();
    assertEquals(reference.from(), rebuilt.from());
    assertEquals(reference.to(), rebuilt.to());
    assertTrue(rebuilt.journey().wheelchair());
    assertEquals(Duration.ofSeconds(60), preferences.transit().boardSlack().defaultValue());
    assertEquals(Duration.ofSeconds(30), preferences.transit().alightSlack().defaultValue());
    assertEquals(2.5, preferences.walk().speed());
    assertEquals(3.0, preferences.walk().reluctance());
    assertEquals(0.5, preferences.walk().safetyFactor());
    assertEquals(
      Duration.ofMinutes(12),
      preferences.street().accessEgress().maxDuration().defaultValue()
    );
  }

  @Test
  void refetchUsesDecodedFromAndToLocations() {
    var refetchService = createRefetchService();

    // A coordinate that doesn't match a stop, on the edge-less test GRAPH, forces
    // ItineraryRefetchService to attempt (and fail) a real access search - this only happens if
    // the reference's `from` is actually passed through, rather than always passing null as it
    // used to.
    var reference = new ItineraryReference(
      List.of(legRef()),
      GenericLocation.fromCoordinate(0, 0),
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );

    var routeRequest = ItineraryReferenceMapper.toRouteRequest(reference, routeRequest());
    var e = assertThrows(ItineraryRefetchException.class, () ->
      refetchService.refetchItinerary(
        reference.from(),
        reference.to(),
        reference.legReferences(),
        routeRequest
      )
    );
    assertEquals("Could not calculate access", e.getMessage());
  }

  /**
   * {@code LegReferenceSerializer} only produces {@link ScheduledTransitLegReference} today, but
   * {@code toRouteRequest}'s board/alight-stop fallback must not assume that holds for every
   * possible {@link org.opentripplanner.model.plan.legreference.LegReference} - a mismatch has to
   * make the {@code refetchItinerary(id:)} query result {@code null}, not cause a
   * {@link ClassCastException}.
   */
  @Test
  void toRouteRequestRejectsUnsupportedLegReferenceTypeInsteadOfThrowingClassCastException() {
    var reference = new ItineraryReference(
      List.of((LegReference) (transitService, transitAlertService) -> null),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toRouteRequest(reference, routeRequest())
    );
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

  private ItineraryRefetchService createRefetchService() {
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
      new TransitServiceResolver(TRANSIT_ENV.transitService())
    );

    return new ItineraryRefetchService(
      GRAPH,
      TRANSIT_ENV.transitService(),
      new TransitAlertServiceImpl(),
      FixedRegularTransfers.of().snapshot(),
      null,
      linkingContextFactory,
      StreetLimitationParametersService.DEFAULT,
      GeofencingZoneService.EMPTY
    );
  }

  /**
   * {@code from}/{@code to} match the trip's board/alight stops exactly, so
   * {@code ItineraryRefetchService.stopMatchesLocation} skips access/egress reconstruction - this
   * keeps the UC1 acceptance test independent of the (edge-less) test {@code GRAPH}.
   */
  private RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(STOP_A.getId()))
      .withTo(GenericLocation.fromStopId(STOP_B.getId()))
      .buildRequest();
  }

  private RouteRequest routeRequest(StreetMode access, StreetMode egress, StreetMode transfer) {
    return routeRequest()
      .copyOf()
      .withJourney(journey ->
        journey
          .withAccess(new StreetRequest(access))
          .withEgress(new StreetRequest(egress))
          .withTransfer(new StreetRequest(transfer))
      )
      .buildRequest();
  }

  /**
   * In production the "default" request is the router-config default, which has no from/to at all
   * ({@link RouteRequest#defaultValue()}). {@code toRouteRequest} must therefore supply them from
   * the reference, otherwise {@code RouteRequest.validate()} rejects the rebuilt request with
   * {@code LOCATION_NOT_FOUND} and the refetch query fails for every real call.
   */
  @Test
  void toRouteRequestWorksWithARealDefaultRequest() {
    var reference = ItineraryReferenceMapper.toItineraryReference(
      createRefetchService().refetchItinerary(null, null, List.of(legRef()), routeRequest()),
      routeRequest()
    );

    var routeRequest = ItineraryReferenceMapper.toRouteRequest(
      reference,
      RouteRequest.defaultValue()
    );

    assertEquals(reference.from(), routeRequest.from());
    assertEquals(reference.to(), routeRequest.to());
  }

  /**
   * A reference may carry no from/to (they are optional). The rebuilt request still has to be
   * valid, so the board/alight stops of the transit spine are used as a fallback.
   */
  @Test
  void toRouteRequestFallsBackToTheTransitSpineStops() {
    var reference = new ItineraryReference(
      List.of(legRef()),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );

    var routeRequest = ItineraryReferenceMapper.toRouteRequest(
      reference,
      RouteRequest.defaultValue()
    );

    assertEquals(GenericLocation.fromStopId(STOP_A.getId()), routeRequest.from());
    assertEquals(GenericLocation.fromStopId(STOP_B.getId()), routeRequest.to());
  }

  /**
   * Only walk and rental preferences are stored. A refetch with another access or egress mode
   * would use the server's preferences for it and could differ from the original itinerary, so
   * such itineraries get no reference.
   */
  @ParameterizedTest
  @EnumSource(
    value = StreetMode.class,
    names = { "WALK", "BIKE_RENTAL", "CAR_RENTAL", "SCOOTER_RENTAL" },
    mode = EnumSource.Mode.EXCLUDE
  )
  void accessOrEgressModeOtherThanWalkOrRentalIsUnsupported(StreetMode mode) {
    var itinerary = createRefetchService().refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest()
    );

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(
        itinerary,
        routeRequest(mode, StreetMode.WALK, StreetMode.WALK)
      )
    );
    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(
        itinerary,
        routeRequest(StreetMode.WALK, mode, StreetMode.WALK)
      )
    );
  }

  @ParameterizedTest
  @EnumSource(value = StreetMode.class, names = { "WALK" }, mode = EnumSource.Mode.EXCLUDE)
  void transferModeOtherThanWalkIsUnsupported(StreetMode mode) {
    var itinerary = createRefetchService().refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest()
    );
    var request = routeRequest(StreetMode.WALK, StreetMode.WALK, mode);

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(itinerary, request)
    );
  }

  /**
   * A reference holds the preferences of the rental modes the itinerary was planned with, and no
   * others, so a walk-only id stays short.
   */
  @ParameterizedTest
  @CsvSource({
    "WALK, WALK, false, false, false",
    "BIKE_RENTAL, WALK, true, false, false",
    "WALK, CAR_RENTAL, false, true, false",
    "SCOOTER_RENTAL, BIKE_RENTAL, true, false, true",
  })
  void storesThePreferencesOfTheRentalModesUsed(
    StreetMode access,
    StreetMode egress,
    boolean hasBike,
    boolean hasCar,
    boolean hasScooter
  ) {
    var itinerary = createRefetchService().refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest()
    );

    var reference = ItineraryReferenceMapper.toItineraryReference(
      itinerary,
      routeRequest(access, egress, StreetMode.WALK)
    );

    assertEquals(access, reference.accessMode());
    assertEquals(egress, reference.egressMode());
    assertEquals(hasBike, reference.bikeRental() != null);
    assertEquals(hasCar, reference.carRental() != null);
    assertEquals(hasScooter, reference.scooterRental() != null);
  }

  /**
   * A reference keeps the rental preferences a client can set, in effect when the itinerary was
   * planned. The ones only the server sets, like the pickup time, are not stored.
   */
  @Test
  void storesTheRentalPreferencesAClientCanSet() {
    var itinerary = createRefetchService().refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest()
    );
    var bikeAndScooter = withClientAndServerRentalPreferences(
      routeRequest(StreetMode.BIKE_RENTAL, StreetMode.SCOOTER_RENTAL, StreetMode.WALK)
    );
    var car = withClientAndServerRentalPreferences(
      routeRequest(StreetMode.CAR_RENTAL, StreetMode.WALK, StreetMode.WALK)
    );

    var bikeAndScooterReference = ItineraryReferenceMapper.toItineraryReference(
      itinerary,
      bikeAndScooter
    );
    var carReference = ItineraryReferenceMapper.toItineraryReference(itinerary, car);

    assertEquals(CLIENT_BIKE_RENTAL, bikeAndScooterReference.bikeRental());
    assertEquals(CLIENT_SCOOTER_RENTAL, bikeAndScooterReference.scooterRental());
    assertEquals(CLIENT_CAR_RENTAL, carReference.carRental());
  }

  /**
   * On refetch, the stored rental preferences replace the current server defaults. The ones only
   * the server sets come from the current default request.
   */
  @Test
  void toRouteRequestOverlaysStoredRentalPreferencesOnTheCurrentDefaultRequest() {
    var bikeAndScooter = rentalReference(
      StreetMode.BIKE_RENTAL,
      StreetMode.SCOOTER_RENTAL,
      CLIENT_BIKE_RENTAL,
      null,
      CLIENT_SCOOTER_RENTAL
    );
    var car = rentalReference(
      StreetMode.WALK,
      StreetMode.CAR_RENTAL,
      null,
      CLIENT_CAR_RENTAL,
      null
    );
    var currentDefault = RouteRequest.defaultValue()
      .copyOf()
      .withPreferences(this::withServerRentalPreferences)
      .buildDefault();

    var bikeAndScooterPreferences = ItineraryReferenceMapper.toRouteRequest(
      bikeAndScooter,
      currentDefault
    ).preferences();
    var carPreferences = ItineraryReferenceMapper.toRouteRequest(car, currentDefault).preferences();

    var expected = RoutingPreferences.of()
      .apply(p -> {
        withServerRentalPreferences(p);
        withClientRentalPreferences(p);
      })
      .build();
    assertEquals(expected.bike(), bikeAndScooterPreferences.bike());
    assertEquals(expected.scooter(), bikeAndScooterPreferences.scooter());
    assertEquals(expected.car(), carPreferences.car());
  }

  /**
   * A decoded id may come from another OTP version. A mode this version cannot refetch the same
   * way makes the refetch return {@code null}.
   */
  @ParameterizedTest
  @CsvSource({ "BIKE, WALK, WALK", "WALK, CAR_TO_PARK, WALK", "WALK, WALK, BIKE_RENTAL" })
  void toRouteRequestRejectsAModeThatIsNotSupported(
    StreetMode access,
    StreetMode egress,
    StreetMode transfer
  ) {
    var reference = new ItineraryReference(
      List.of(legRef()),
      null,
      null,
      access,
      egress,
      transfer,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      null,
      null,
      null
    );

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toRouteRequest(reference, routeRequest())
    );
  }

  private RouteRequest withClientAndServerRentalPreferences(RouteRequest request) {
    return request
      .copyOf()
      .withPreferences(preferences -> {
        withClientRentalPreferences(preferences);
        withServerRentalPreferences(preferences);
      })
      .buildRequest();
  }

  private ItineraryReference rentalReference(
    StreetMode access,
    StreetMode egress,
    BikeRentalPreferences bike,
    CarRentalPreferences car,
    ScooterRentalPreferences scooter
  ) {
    return new ItineraryReference(
      List.of(legRef()),
      GenericLocation.fromStopId(STOP_A.getId()),
      GenericLocation.fromStopId(STOP_B.getId()),
      access,
      egress,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
      1.0,
      false,
      bike,
      car,
      scooter
    );
  }

  /** Rental preferences a client can set in the GTFS API, all different from the defaults. */
  private void withClientRentalPreferences(RoutingPreferencesBuilder preferences) {
    preferences
      .withBike(bike ->
        bike
          .withSpeed(7.5)
          .withReluctance(3.5)
          .withOptimizeType(VehicleRoutingOptimizeType.TRIANGLE)
          .withOptimizeTriangle(t -> t.withTime(0.2).withSlope(0.3).withSafety(0.5))
          .withWalking(walking ->
            walking
              .withSpeed(1.2)
              .withReluctance(4.0)
              .withMountDismountTime(Duration.ofSeconds(45))
              .withMountDismountCost(90)
          )
          .withRental(rental ->
            rental
              .withAllowedNetworks(Set.of("bike-a", "bike-b"))
              .withBannedNetworks(Set.of("bike-c"))
              .withAllowArrivingInRentedVehicleAtDestination(true)
              .withArrivingInRentalVehicleAtDestinationCost(120)
          )
      )
      .withCar(car ->
        car
          .withReluctance(4.5)
          .withRental(rental ->
            rental.withAllowedNetworks(Set.of("car-a")).withBannedNetworks(Set.of("car-b"))
          )
      )
      .withScooter(scooter ->
        scooter
          .withSpeed(6.5)
          .withReluctance(2.5)
          .withOptimizeType(VehicleRoutingOptimizeType.FLAT_STREETS)
          .withOptimizeTriangle(t -> t.withTime(0.6).withSlope(0.1).withSafety(0.3))
          .withRental(rental ->
            rental
              .withAllowedNetworks(Set.of("scooter-a"))
              .withBannedNetworks(Set.of("scooter-b"))
              .withAllowArrivingInRentedVehicleAtDestination(true)
              .withArrivingInRentalVehicleAtDestinationCost(60)
          )
      );
  }

  /** Rental preferences only the server sets, all different from the defaults. */
  private void withServerRentalPreferences(RoutingPreferencesBuilder preferences) {
    preferences
      .withBike(bike ->
        bike
          .withWalking(walking -> walking.withStairsReluctance(8.0))
          .withRental(rental -> withServerRental(rental))
      )
      .withCar(car -> car.withRental(rental -> withServerRental(rental)))
      .withScooter(scooter -> scooter.withRental(rental -> withServerRental(rental)));
  }

  private static void withServerRental(VehicleRentalPreferences.Builder rental) {
    rental
      .withPickupTime(Duration.ofMinutes(3))
      .withPickupCost(240)
      .withDropOffTime(Duration.ofMinutes(2))
      .withDropOffCost(180)
      .withElectricAssistSlopeSensitivity(0.5);
  }

  /** Via locations are not stored in the reference, so such an itinerary gets no reference. */
  @Test
  void viaLocationsAreUnsupported() {
    var itinerary = createRefetchService().refetchItinerary(
      null,
      null,
      List.of(legRef()),
      routeRequest()
    );
    var request = routeRequest()
      .copyOf()
      .withViaLocations(List.of(new PassThroughViaLocation("Via B", List.of(STOP_B.getId()))))
      .buildRequest();

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(itinerary, request)
    );
  }

  /**
   * A {@link org.opentripplanner.model.plan.leg.FrequencyTransitLeg} is a
   * {@code ScheduledTransitLeg} and so passes the supported-leg-reference check, but frequency
   * trips are not in the timetable's trip-times index, so the leg could never be looked up again
   * on refetch. Such an itinerary must not get an id at all.
   */
  @Test
  void frequencyLegIsUnsupported() {
    var itinerary = newItinerary(Place.forStop(STOP_A), 0)
      .frequencyBus(1, 200, 300, Place.forStop(STOP_B))
      .build();

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(itinerary, routeRequest())
    );
  }

  /**
   * Legs without a {@link org.opentripplanner.model.plan.legreference.LegReference} that are not
   * street legs (flex, carpool) must make the itinerary unsupported rather than being silently
   * dropped from the transit spine - a partially-encoded id would refetch a different trip.
   */
  @Test
  void nonStreetLegWithoutAReferenceIsUnsupported() {
    var boardPlace = Place.forStop(STOP_A);
    var alightPlace = Place.forStop(STOP_B);

    var itinerary = newItinerary(boardPlace, 0)
      .bus(1, 200, 300, alightPlace)
      .flex(400, 500, alightPlace)
      .build();

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(itinerary, routeRequest())
    );
  }

  /**
   * An id with more legs than {@link ItineraryReference#MAX_LEG_REFERENCES} would be rejected when
   * decoded, so such an itinerary must not get an id at all.
   */
  @Test
  void itineraryWithTooManyTransitLegsIsUnsupported() {
    var stopA = Place.forStop(STOP_A);
    var stopB = Place.forStop(STOP_B);
    var builder = newItinerary(stopA, 0);
    for (int i = 0; i <= ItineraryReference.MAX_LEG_REFERENCES; i++) {
      builder.bus(i, i * 100, i * 100 + 50, i % 2 == 0 ? stopB : stopA);
    }
    var itinerary = builder.build();

    assertThrows(UnsupportedItineraryReferenceException.class, () ->
      ItineraryReferenceMapper.toItineraryReference(itinerary, routeRequest())
    );
  }
}
