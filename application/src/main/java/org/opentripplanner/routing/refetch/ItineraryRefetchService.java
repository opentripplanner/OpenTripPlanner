package org.opentripplanner.routing.refetch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.ScheduledTransitLeg;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.mapping.StreetPathToLegsMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestBuilder;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.transit.transfer.regular.RegularTransferRepositorySnapshot;
import org.opentripplanner.transit.transfer.regular.api.RegularTransfersService;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItineraryRefetchService {

  private static final Logger LOG = LoggerFactory.getLogger(ItineraryRefetchService.class);

  private final TransitService transitService;
  private final TransitAlertService transitAlertService;
  private final ConstrainedTransferService constrainedTransferService;
  private final RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> regularTransfers;
  private final Graph graph;
  private final LinkingContextFactory linkingContextFactory;
  private final StreetPathToLegsMapper streetPathToLegsMapper;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final GeofencingZoneService geofencingZoneService;

  public ItineraryRefetchService(
    Graph graph,
    TransitService transitService,
    TransitAlertService transitAlertService,
    RegularTransferRepositorySnapshot<NearbyStop, RouteRequest> regularTransfers,
    StreetDetailsService streetDetailsService,
    LinkingContextFactory linkingContextFactory,
    StreetLimitationParametersService streetLimitationParametersService,
    GeofencingZoneService geofencingZoneService
  ) {
    this.constrainedTransferService = transitService.getConstrainedTransferService();
    this.transitService = transitService;
    this.transitAlertService = transitAlertService;
    this.regularTransfers = regularTransfers;
    this.linkingContextFactory = linkingContextFactory;
    this.graph = graph;
    this.streetPathToLegsMapper = new StreetPathToLegsMapper(
      new TransitServiceResolver(transitService),
      transitService.getTimeZone(),
      streetDetailsService,
      graph.ellipsoidToGeoidDifference
    );
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.geofencingZoneService = geofencingZoneService;
  }

  /// Refetch an itinerary
  ///
  /// @param from          An optional from location. If null the first legReference will be the
  ///                      start of the itinerary.
  /// @param to            An optional to location. If null the last legReference will be the end of
  ///                      the itinerary.
  /// @param legReferences A list of leg references describing the parts of the itinerary.
  /// @throws ItineraryRefetchException If there is some issue with the input values that should be
  ///                                   mapped so some kind of InvalidInput message to the user.
  public Itinerary refetchItinerary(
    @Nullable GenericLocation from,
    @Nullable GenericLocation to,
    List<LegReference> legReferences,
    RouteRequest routeRequest
  ) throws ItineraryRefetchException {
    if (legReferences.isEmpty()) {
      throw new IllegalArgumentException("legReferences must not be empty");
    }

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var transitLegs = getScheduledTransitLegs(legReferences);

      Set<Vertex> fromVertices = null;
      if (from != null) {
        var fromStop = Objects.requireNonNull(transitLegs.getFirst().from().stop);
        if (!stopMatchesLocation(fromStop, from)) {
          fromVertices = linkingContextFactory.getOrCreateStreetVertices(
            temporaryVerticesContainer,
            from,
            VertexCreationService.LocationType.FROM,
            routeRequest.journey().access().mode()
          );
        }
      }
      Set<Vertex> toVertices = null;
      if (to != null) {
        var toStop = Objects.requireNonNull(transitLegs.getLast().to().stop);
        if (!stopMatchesLocation(toStop, to)) {
          toVertices = linkingContextFactory.getOrCreateStreetVertices(
            temporaryVerticesContainer,
            to,
            VertexCreationService.LocationType.TO,
            routeRequest.journey().egress().mode()
          );
        }
      }

      return refetch(fromVertices, toVertices, transitLegs, routeRequest);
    }
  }

  private boolean stopMatchesLocation(StopLocation stop, GenericLocation location) {
    var locationId = location.stopId();
    if (locationId == null) {
      return false;
    }
    if (stop.getId().equals(locationId)) {
      return true;
    }
    var stopGroup = transitService.getStopLocationsGroup(locationId);
    if (stopGroup == null) {
      return false;
    }
    // See if the location is a StopLocationGroup and contains the stop
    return stopGroup
      .getChildStops()
      .stream()
      .anyMatch(child -> child.getId().equals(stop.getId()));
  }

  private List<ScheduledTransitLeg> getScheduledTransitLegs(List<LegReference> legReferences) {
    var transitLegs = new ArrayList<ScheduledTransitLeg>(legReferences.size());
    for (LegReference legReference : legReferences) {
      if (legReference instanceof ScheduledTransitLegReference scheduledTransitLeg) {
        var transitLeg = scheduledTransitLeg.getLeg(transitService, transitAlertService);
        if (transitLeg == null) {
          throw new ItineraryRefetchException("Could not find leg " + scheduledTransitLeg);
        }
        transitLegs.add(transitLeg);
      } else {
        throw new ItineraryRefetchException(
          "Itinerary refetch is not implemented for non transit legs yet"
        );
      }
    }
    return transitLegs;
  }

  private Itinerary refetch(
    @Nullable Set<Vertex> fromVertices,
    @Nullable Set<Vertex> toVertices,
    List<ScheduledTransitLeg> transitLegs,
    RouteRequest routeRequest
  ) {
    var transferRequest = StreetSearchRequestMapper.mapToTransferRequest(routeRequest).build();
    var transfers = regularTransfers.forRequest(routeRequest);

    var legs = new ArrayList<Leg>();
    if (fromVertices != null) {
      var firstLeg = transitLegs.getFirst();
      var firstStop = findVertex(firstLeg.from());
      var boardSlack = routeRequest.preferences().transit().boardSlack().valueOf(firstLeg.mode());
      var time = firstLeg.startTime().toInstant().minus(boardSlack);
      var access = accessEgress(
        fromVertices,
        Set.of(firstStop),
        routeRequest,
        AccessEgressType.ACCESS,
        time
      ).orElseThrow(() -> new ItineraryRefetchException("Could not calculate access"));
      legs.addAll(streetPathToLegs(access));
    }

    ConstrainedTransfer prevLegConstrainedTransfer = null;

    for (int i = 0; i < transitLegs.size() - 1; i++) {
      var transitLeg = transitLegs.get(i);
      var nextTransitLeg = transitLegs.get(i + 1);

      // ScheduledTransitLegs are guaranteed to have StopLocation in both ends.
      var transferFromStop = Objects.requireNonNull(transitLeg.to().stop);
      var transferToStop = Objects.requireNonNull(nextTransitLeg.from().stop);

      if (prevLegConstrainedTransfer != null) {
        transitLeg = transitLeg
          .copyOf()
          .withTransferFromPreviousLeg(prevLegConstrainedTransfer)
          .build();
        prevLegConstrainedTransfer = null;
      }

      var constrainedTransfer = constrainedTransferService.findTransfer(
        transitLeg.trip(),
        transitLeg.alightStopPosInPattern(),
        transferFromStop,
        nextTransitLeg.trip(),
        nextTransitLeg.boardStopPosInPattern(),
        transferToStop
      );
      if (constrainedTransfer != null) {
        transitLeg = transitLeg.copyOf().withTransferToNextLeg(constrainedTransfer).build();
        prevLegConstrainedTransfer = constrainedTransfer;
      }

      legs.add(transitLeg);

      if (!transferFromStop.equals(transferToStop)) {
        var alightSlack = routeRequest
          .preferences()
          .transit()
          .alightSlack()
          .valueOf(transitLeg.mode());

        var transferStartTime = transitLeg.endTime().toInstant().plus(alightSlack);

        var request = StreetSearchRequest.copyOf(transferRequest)
          .withStartTime(transferStartTime)
          .build();

        var transferLegs = transfer(
          transfers,
          transferFromStop,
          transferToStop,
          request
        ).orElseThrow(() ->
          new ItineraryRefetchException(
            "Could not transfer from " + transferFromStop.getId() + " to " + transferToStop.getId()
          )
        );

        legs.addAll(transferLegs);
      }
    }

    var lastTransitLeg = transitLegs.getLast();
    if (prevLegConstrainedTransfer != null) {
      lastTransitLeg = lastTransitLeg
        .copyOf()
        .withTransferFromPreviousLeg(prevLegConstrainedTransfer)
        .build();
    }
    legs.add(lastTransitLeg);

    boolean arrivedAtDestinationWithRentedVehicle = false;
    if (toVertices != null) {
      var lastLeg = transitLegs.getLast();
      var lastStop = findVertex(lastLeg.to());
      var alightSlack = routeRequest.preferences().transit().alightSlack().valueOf(lastLeg.mode());
      var time = lastLeg.endTime().toInstant().plus(alightSlack);
      var egress = accessEgress(
        Set.of(lastStop),
        toVertices,
        routeRequest,
        AccessEgressType.EGRESS,
        time
      ).orElseThrow(() -> new ItineraryRefetchException("Could not calculate egress"));
      legs.addAll(streetPathToLegs(egress));
      arrivedAtDestinationWithRentedVehicle = egress.lastState().isRentingVehicleFromStation();
    }
    return Itinerary.ofScheduledTransit(legs)
      // We don't try to calculate a cost for the refetched itinerary.
      .withGeneralizedCost(Cost.ZERO)
      .withArrivedAtDestinationWithRentedVehicle(arrivedAtDestinationWithRentedVehicle)
      .build();
  }

  /// This takes a place that has to have a stop id in it and maps to GenericLocation
  private Vertex findVertex(Place place) {
    return Objects.requireNonNull(
      graph.getStopVertex(Objects.requireNonNull(place.stop).getId()),
      "Could not find stop vertex " + place.stop.getId()
    );
  }

  /// The access is searched backwards from the first boarding stop, so it arrives in time to board.
  /// The egress is searched forwards from the last alighting stop.
  private Optional<StreetPath> accessEgress(
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    RouteRequest routeRequest,
    AccessEgressType type,
    Instant time
  ) {
    var search = createStreetSearch(fromVertices, toVertices, routeRequest, type, time);

    return search.getPathsToTarget().stream().min(Comparator.comparing(StreetPath::weight));
  }

  private StreetSearchBuilder createStreetSearch(
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    RouteRequest routeRequest,
    AccessEgressType type,
    Instant time
  ) {
    var streetMode = type.isAccess()
      ? routeRequest.journey().access().mode()
      : routeRequest.journey().egress().mode();
    var maxDuration = routeRequest
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(streetMode);

    var streetSearchRequest = StreetSearchRequestMapper.map(routeRequest)
      .withMode(streetMode)
      .withStartTime(time)
      .withArriveBy(type.isAccess());
    if (type.isAccess()) {
      applyAccessRentalRules(streetSearchRequest, streetMode, toVertices);
    }

    return StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withHeuristic(new EuclideanRemainingWeightHeuristic(streetLimitationParametersService))
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(maxDuration))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(streetSearchRequest.build())
      .withFrom(fromVertices)
      .withTo(toVertices);
  }

  /// A rented vehicle has to be dropped off before boarding: like the router, the access never
  /// keeps it, whatever the destination policy says. The access search starts at the boarding stop,
  /// so it also needs the geofencing zones there, or it could drop a free-floating vehicle where
  /// that is not allowed.
  private void applyAccessRentalRules(
    StreetSearchRequestBuilder request,
    StreetMode streetMode,
    Set<Vertex> boardingStops
  ) {
    request
      .withBike(b -> b.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)))
      .withCar(c -> c.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)))
      .withScooter(s -> s.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)));

    if (streetMode.includesRenting() && geofencingZoneService.hasIndexedZones()) {
      var zones = boardingStops
        .stream()
        .flatMap(stop -> geofencingZoneService.findZonesContaining(stop.getCoordinate()).stream())
        .collect(Collectors.toSet());
      request.withArriveByDestinationZones(zones);
    }
  }

  /// The transfer is read from the regular transfers of the request, the same path that the
  /// router uses for the itinerary.
  private Optional<List<Leg>> transfer(
    RegularTransfersService<NearbyStop> transfers,
    StopLocation from,
    StopLocation to,
    StreetSearchRequest transferRequest
  ) {
    var path = transfers.findPath(from.getIndex(), to.getIndex());
    if (path == null || CollectionUtils.isEmpty(path.edges())) {
      // We don't support straight line transfers currently.
      return Optional.empty();
    }
    return EdgeTraverser.createPath(path.edges(), transferRequest).map(this::streetPathToLegs);
  }

  private List<Leg> streetPathToLegs(StreetPath path) {
    // We don't support via locations in transfers currently.
    List<ViaLocation> viaLocations = List.of();
    return streetPathToLegsMapper.map(path, viaLocations, null);
  }
}
