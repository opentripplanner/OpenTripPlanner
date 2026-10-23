package org.opentripplanner.routing.refetch;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReference;
import org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
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
 * UC1 acceptance test for a single scheduled-transit itinerary without access, egress or
 * transfers.
 *
 * <p>Proves the chain:
 * {@code Itinerary -> ItineraryReference -> encode -> decode -> RefetchItineraryService ->
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
    // Register stop vertices (no street edges) so RefetchItineraryService.findVertex(...) can
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
    assertThat(encoded).isNotNull();
    var decodedReference = ItineraryReferenceSerializer.decode(encoded);
    assertThat(decodedReference).isNotNull();

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

    assertThat(refetchedItinerary.toStr()).isEqualTo(originalItinerary.toStr());
    assertThat(refetchedItinerary.toStr()).isEqualTo("A ~ BUS tripX 10:00 11:00 ~ B []");
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
    assertThat(expectedLegReference).isNotNull();

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
          .withAccess(new StreetRequest(StreetMode.BIKE))
          .withEgress(new StreetRequest(StreetMode.CAR))
          .withWheelchair(true)
      )
      .withPreferences(preferences ->
        preferences
          .withTransit(transit ->
            transit
              .withBoardSlack(b ->
                b.withDefaultSec(60).with(TransitMode.BUS, Duration.ofSeconds(90))
              )
              .withAlightSlack(b ->
                b.withDefaultSec(30).with(TransitMode.RAIL, Duration.ofSeconds(45))
              )
          )
          .withWalk(walk -> walk.withSpeed(2.5).withReluctance(3.0))
      )
      .buildRequest();

    var reference = ItineraryReferenceMapper.toItineraryReference(itinerary, customRouteRequest);

    assertThat(reference.from()).isEqualTo(GenericLocation.fromStopId(STOP_A.getId()));
    assertThat(reference.to()).isEqualTo(GenericLocation.fromStopId(STOP_B.getId()));
    assertThat(reference.accessMode()).isEqualTo(StreetMode.BIKE);
    assertThat(reference.egressMode()).isEqualTo(StreetMode.CAR);
    assertThat(reference.transferMode()).isEqualTo(StreetMode.WALK);
    assertThat(reference.wheelchair()).isTrue();
    assertThat(reference.boardSlack()).isEqualTo(Duration.ofSeconds(60));
    assertThat(reference.alightSlack()).isEqualTo(Duration.ofSeconds(30));
    assertThat(reference.walkSpeed()).isEqualTo(2.5);
    assertThat(reference.walkReluctance()).isEqualTo(3.0);
  }

  /**
   * {@code customRouteRequest} above sets per-mode board/alight slack overrides
   * ({@code TransitMode.BUS}/{@code TransitMode.RAIL}), which no supported API lets a client set.
   * {@link ItineraryReference#boardSlack()}/{@link ItineraryReference#alightSlack()} are plain
   * {@link Duration}s, not {@link DurationForEnum}, so they cannot carry a per-mode override at
   * all - confirming they only capture the client-settable default, per {@link ItineraryReference}'s
   * class doc.
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

    // Only the Duration type itself is asserted here - there is no override to read, because
    // ItineraryReference has no field capable of holding one.
    assertThat(reference.boardSlack()).isInstanceOf(Duration.class);
    assertThat(reference.boardSlack()).isEqualTo(Duration.ofSeconds(60));
    assertThat(reference.alightSlack()).isEqualTo(Duration.ofSeconds(30));

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

    assertThat(rebuilt.preferences().transit().boardSlack().valueOf(TransitMode.BUS)).isEqualTo(
      Duration.ofSeconds(999)
    );
  }

  @Test
  void refetchUsesDecodedFromAndToLocations() {
    var refetchService = createRefetchService();

    // A coordinate that doesn't match a stop, on the edge-less test GRAPH, forces
    // RefetchItineraryService to attempt (and fail) a real access search - this only happens if
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
      false
    );

    var routeRequest = ItineraryReferenceMapper.toRouteRequest(reference, routeRequest());
    var e = assertThrows(RefetchItineraryException.class, () ->
      refetchService.refetchItinerary(
        reference.from(),
        reference.to(),
        reference.legReferences(),
        routeRequest
      )
    );
    assertThat(e.getMessage()).isEqualTo("Could not calculate access");
  }

  /**
   * {@code LegReferenceSerializer} only produces {@link ScheduledTransitLegReference} today, but
   * {@code toRouteRequest}'s board/alight-stop fallback must not assume that holds for every
   * possible {@link org.opentripplanner.model.plan.legreference.LegReference} - a mismatch has to
   * make the {@code itinerary(id:)} query result {@code null}, not cause a
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
      false
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

  private RefetchItineraryService createRefetchService() {
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

  /**
   * {@code from}/{@code to} match the trip's board/alight stops exactly, so
   * {@code RefetchItineraryService.stopMatchesLocation} skips access/egress reconstruction - this
   * keeps the UC1 acceptance test independent of the (edge-less) test {@code GRAPH}.
   */
  private RouteRequest routeRequest() {
    return RouteRequest.of()
      .withFrom(GenericLocation.fromStopId(STOP_A.getId()))
      .withTo(GenericLocation.fromStopId(STOP_B.getId()))
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

    assertThat(routeRequest.from()).isEqualTo(reference.from());
    assertThat(routeRequest.to()).isEqualTo(reference.to());
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
      false
    );

    var routeRequest = ItineraryReferenceMapper.toRouteRequest(
      reference,
      RouteRequest.defaultValue()
    );

    assertThat(routeRequest.from()).isEqualTo(GenericLocation.fromStopId(STOP_A.getId()));
    assertThat(routeRequest.to()).isEqualTo(GenericLocation.fromStopId(STOP_B.getId()));
  }

  /**
   * Legs without a {@link org.opentripplanner.model.plan.legreference.LegReference} that are not
   * street legs (flex, carpool) must make the itinerary unsupported rather than being silently
   * dropped from the transit spine - a partially-encoded id would refetch a different trip.
   */
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
}
