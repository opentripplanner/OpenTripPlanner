package org.opentripplanner.routing.refetch;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
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
import org.opentripplanner.routing.algorithm.mapping.StreetPathToLegsMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transfer.regular.model.PathTransfer;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.utils.collection.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefetchItineraryService {

  private static final Logger LOG = LoggerFactory.getLogger(RefetchItineraryService.class);

  private final TransitService transitService;
  private final RegularTransferService transferService;
  private final Graph graph;
  private final LinkingContextFactory linkingContextFactory;
  private final StreetPathToLegsMapper streetPathToLegsMapper;
  private final StreetLimitationParametersService streetLimitationParametersService;

  public RefetchItineraryService(OtpServerRequestContext serverContext) {
    this(
      serverContext.graph(),
      serverContext.transitService(),
      serverContext.transferService(),
      serverContext.streetDetailsService(),
      serverContext.linkingContextFactory(),
      serverContext.streetLimitationParametersService()
    );
  }

  public RefetchItineraryService(
    Graph graph,
    TransitService transitService,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    LinkingContextFactory linkingContextFactory,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    this.transitService = transitService;
    this.transferService = transferService;
    this.linkingContextFactory = linkingContextFactory;
    this.graph = graph;
    this.streetPathToLegsMapper = new StreetPathToLegsMapper(
      new TransitServiceResolver(transitService),
      transitService.getTimeZone(),
      graph.streetNotesService,
      streetDetailsService,
      graph.ellipsoidToGeoidDifference
    );
    this.streetLimitationParametersService = streetLimitationParametersService;
  }

  /// Refetch an itinerary
  ///
  /// @param from An optional from location. If null the first legReference will be the start of the itinerary.
  /// @param to An optional to location. If null the last legReference will be the end of the itinerary.
  /// @param legReferences A list of leg references describing the parts of the itinerary.
  /// @throws RefetchItineraryException If there is some issue with the input values that should be mapped so some kind of InvalidInput message to the user.
  public Itinerary refetchItinerary(
    @Nullable GenericLocation from,
    @Nullable GenericLocation to,
    List<LegReference> legReferences,
    RouteRequest routeRequest
  ) throws RefetchItineraryException {
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
        var transitLeg = scheduledTransitLeg.getLeg(transitService);
        if (transitLeg == null) {
          throw new RefetchItineraryException("Could not find leg " + scheduledTransitLeg);
        }
        transitLegs.add(transitLeg);
      } else {
        throw new RefetchItineraryException(
          "RefetchItinerary not implemented for non transit legs yet"
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

    var legs = new ArrayList<Leg>();
    if (fromVertices != null) {
      var mode = routeRequest.journey().access().mode();
      var firstLeg = transitLegs.getFirst();
      var firstStop = findVertex(firstLeg.from());
      var boardSlack = routeRequest.preferences().transit().boardSlack().valueOf(firstLeg.mode());
      var time = firstLeg.startTime().toInstant().minus(boardSlack);
      var access = accessEgress(
        fromVertices,
        Set.of(firstStop),
        routeRequest,
        mode,
        time,
        true
      ).orElseThrow(() -> new RefetchItineraryException("Could not calculate access"));
      legs.addAll(access);
    }

    legs.add(transitLegs.getFirst());

    for (int i = 1; i < transitLegs.size(); i++) {
      var legA = transitLegs.get(i - 1);
      var legB = transitLegs.get(i);

      // ScheduledTransitLegs are guaranteed to have StopLocation in both ends
      var transferFrom = Objects.requireNonNull(legA.to().stop);
      var transferTo = Objects.requireNonNull(legB.from().stop);

      if (!transferFrom.equals(transferTo)) {
        var alightSlack = routeRequest.preferences().transit().alightSlack().valueOf(legA.mode());
        var transferStartTime = legA.endTime().toInstant().plus(alightSlack);

        var request = StreetSearchRequest.copyOf(transferRequest)
          .withStartTime(transferStartTime)
          .build();

        var transferLegs = transfer(transferFrom, transferTo, request).orElseThrow(() ->
          new RefetchItineraryException(
            "Could not transfer from " + transferFrom.getId() + " to " + transferTo.getId()
          )
        );
        legs.addAll(transferLegs);
      }

      legs.add(legB);
    }

    if (toVertices != null) {
      var mode = routeRequest.journey().egress().mode();
      var lastLeg = transitLegs.getLast();
      var lastStop = findVertex(lastLeg.to());
      var alightSlack = routeRequest.preferences().transit().alightSlack().valueOf(lastLeg.mode());
      var time = lastLeg.endTime().toInstant().plus(alightSlack);
      var egress = accessEgress(
        Set.of(lastStop),
        toVertices,
        routeRequest,
        mode,
        time,
        false
      ).orElseThrow(() -> new RefetchItineraryException("Could not calculate egress"));
      legs.addAll(egress);
    }
    return Itinerary.ofScheduledTransit(legs)
      // We don't try to calculate a cost for the refetched itinerary.
      .withGeneralizedCost(Cost.ZERO)
      .build();
  }

  /// This takes a place that has to have a stop id in it and maps to GenericLocation
  private Vertex findVertex(Place place) {
    return Objects.requireNonNull(
      graph.getStopVertex(Objects.requireNonNull(place.stop).getId()),
      "Could not find stop vertex " + place.stop.getId()
    );
  }

  private Optional<List<Leg>> accessEgress(
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    RouteRequest routeRequest,
    StreetMode streetMode,
    Instant time,
    boolean arriveBy
  ) {
    var search = createStreetSearch(
      fromVertices,
      toVertices,
      routeRequest,
      streetMode,
      time,
      arriveBy
    );

    return search
      .getPathsToTarget()
      .stream()
      .min(Comparator.comparing(StreetPath::weight))
      .map(this::streetPathToLegs);
  }

  private StreetSearchBuilder createStreetSearch(
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    RouteRequest routeRequest,
    StreetMode streetMode,
    Instant time,
    boolean arriveBy
  ) {
    var maxDuration = routeRequest
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(streetMode);

    var streetSearchRequest = StreetSearchRequestMapper.map(routeRequest)
      .withMode(streetMode)
      .withStartTime(time)
      .withArriveBy(arriveBy)
      .build();

    return StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withHeuristic(new EuclideanRemainingWeightHeuristic(streetLimitationParametersService))
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(maxDuration))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(streetSearchRequest)
      .withFrom(fromVertices)
      .withTo(toVertices);
  }

  private Optional<List<Leg>> transfer(
    StopLocation from,
    StopLocation to,
    StreetSearchRequest transferRequest
  ) {
    var mode = transferRequest.mode();
    return transferService
      .findTransfersByStop(from)
      .stream()
      .filter(pathTransfer -> pathTransfer.to.equals(to) && pathTransfer.getModes().contains(mode))
      .flatMap(pathTransfer -> mapPathTransferStreetPath(pathTransfer, transferRequest).stream())
      .min(Comparator.comparing(StreetPath::weight))
      .map(this::streetPathToLegs);
  }

  private Optional<StreetPath> mapPathTransferStreetPath(
    PathTransfer pathTransfer,
    StreetSearchRequest request
  ) {
    var edges = pathTransfer.getEdges();
    if (CollectionUtils.isEmpty(edges)) {
      // We don't support straight line transfers currently.
      return Optional.empty();
    }

    return EdgeTraverser.createPath(pathTransfer.getEdges(), request);
  }

  private List<Leg> streetPathToLegs(StreetPath path) {
    // We don't support via locations in transfers currently.
    List<ViaLocation> viaLocations = List.of();
    return streetPathToLegsMapper.map(path, viaLocations, null);
  }
}
