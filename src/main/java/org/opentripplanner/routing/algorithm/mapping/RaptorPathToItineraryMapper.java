package org.opentripplanner.routing.algorithm.mapping;

import static org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter.toOtpDomainCost;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.FrequencyTransitLeg;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.LegMode;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransferWithDuration;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.transit.raptor.api.path.AccessPathLeg;
import org.opentripplanner.transit.raptor.api.path.EgressPathLeg;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.path.PathLeg;
import org.opentripplanner.transit.raptor.api.path.TransferPathLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.geometry.GeometryUtils;

/**
 * This maps the paths found by the Raptor search algorithm to the itinerary structure currently
 * used by OTP. The paths, access/egress transfers and transit layer only contains the minimal
 * information needed for routing. Additional information has to be fetched from the graph index to
 * create complete itineraries that can be shown in a trip planner.
 */
public class RaptorPathToItineraryMapper {

  private final Graph graph;

  private final TransitLayer transitLayer;

  private final RouteRequest request;

  private final ZonedDateTime transitSearchTimeZero;

  private final GraphPathToItineraryMapper graphPathToItineraryMapper;

  /**
   * Constructs an itinerary mapper for a request and a set of results
   *
   * @param transitLayer          the currently active transit layer (may have real-time data
   *                              applied)
   * @param transitSearchTimeZero the point in time all times in seconds are counted from
   * @param request               the current routing request
   */
  public RaptorPathToItineraryMapper(
    Graph graph,
    TransitService transitService,
    TransitLayer transitLayer,
    ZonedDateTime transitSearchTimeZero,
    RouteRequest request
  ) {
    this.graph = graph;
    this.transitLayer = transitLayer;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.request = request;
    this.graphPathToItineraryMapper =
      new GraphPathToItineraryMapper(
        transitService.getTimeZone(),
        graph.streetNotesService,
        graph.ellipsoidToGeoidDifference
      );
  }

  public Itinerary createItinerary(Path<TripSchedule> path) {
    var optimizedPath = path instanceof OptimizedPath ? (OptimizedPath<TripSchedule>) path : null;

    // Map access leg
    List<Leg> legs = new ArrayList<>(mapAccessLeg(path.accessLeg()));

    PathLeg<TripSchedule> pathLeg = path.accessLeg().nextLeg();

    Leg transitLeg = null;

    while (!pathLeg.isEgressLeg()) {
      // Map transit leg
      if (pathLeg.isTransitLeg()) {
        transitLeg = mapTransitLeg(transitLeg, pathLeg.asTransitLeg());
        legs.add(transitLeg);
      }
      // Map transfer leg
      else if (pathLeg.isTransferLeg()) {
        legs.addAll(
          mapTransferLeg(
            pathLeg.asTransferLeg(),
            request.modes.transferMode == StreetMode.BIKE ? LegMode.BICYCLE : LegMode.WALK
          )
        );
      }

      pathLeg = pathLeg.nextLeg();
    }

    // Map egress leg
    EgressPathLeg<TripSchedule> egressPathLeg = pathLeg.asEgressLeg();
    Itinerary mapped = mapEgressLeg(egressPathLeg);
    legs.addAll(mapped == null ? List.of() : mapped.getLegs());

    Itinerary itinerary = new Itinerary(legs);

    // Map general itinerary fields
    itinerary.setGeneralizedCost(toOtpDomainCost(path.generalizedCost()));
    itinerary.setArrivedAtDestinationWithRentedVehicle(
      mapped != null && mapped.isArrivedAtDestinationWithRentedVehicle()
    );

    if (optimizedPath != null) {
      itinerary.setWaitTimeOptimizedCost(
        toOtpDomainCost(optimizedPath.generalizedCostWaitTimeOptimized())
      );
      itinerary.setTransferPriorityCost(toOtpDomainCost(optimizedPath.transferPriorityCost()));
    }

    return itinerary;
  }

  private List<Leg> mapAccessLeg(AccessPathLeg<TripSchedule> accessPathLeg) {
    AccessEgress accessPath = (AccessEgress) accessPathLeg.access();

    if (accessPath.durationInSeconds() == 0) {
      return List.of();
    }

    GraphPath graphPath = new GraphPath(accessPath.getLastState());

    Itinerary subItinerary = graphPathToItineraryMapper.generateItinerary(graphPath);

    if (subItinerary.getLegs().isEmpty()) {
      return List.of();
    }

    return subItinerary
      .withTimeShiftToStartAt(createZonedDateTime(accessPathLeg.fromTime()))
      .getLegs();
  }

  private Leg mapTransitLeg(Leg prevTransitLeg, TransitPathLeg<TripSchedule> pathLeg) {
    TripSchedule tripSchedule = pathLeg.trip();

    // Find stop positions in pattern where this leg boards and alights.
    // We cannot assume every stop appears only once in a pattern, so we
    // have to match stop and time.
    int boardStopIndexInPattern = tripSchedule.findDepartureStopPosition(
      pathLeg.fromTime(),
      pathLeg.fromStop()
    );
    int alightStopIndexInPattern = tripSchedule.findArrivalStopPosition(
      pathLeg.toTime(),
      pathLeg.toStop()
    );

    if (tripSchedule.isFrequencyBasedTrip()) {
      int frequencyHeadwayInSeconds = tripSchedule.frequencyHeadwayInSeconds();
      return new FrequencyTransitLeg(
        tripSchedule.getOriginalTripTimes(),
        tripSchedule.getOriginalTripPattern(),
        boardStopIndexInPattern,
        alightStopIndexInPattern,
        createZonedDateTime(pathLeg.fromTime() + frequencyHeadwayInSeconds),
        createZonedDateTime(pathLeg.toTime()),
        tripSchedule.getServiceDate(),
        transitSearchTimeZero.getZone().normalized(),
        (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg()),
        (ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg(),
        toOtpDomainCost(pathLeg.generalizedCost()),
        frequencyHeadwayInSeconds,
        null
      );
    }
    return new ScheduledTransitLeg(
      tripSchedule.getOriginalTripTimes(),
      tripSchedule.getOriginalTripPattern(),
      boardStopIndexInPattern,
      alightStopIndexInPattern,
      createZonedDateTime(pathLeg.fromTime()),
      createZonedDateTime(pathLeg.toTime()),
      tripSchedule.getServiceDate(),
      transitSearchTimeZero.getZone().normalized(),
      (prevTransitLeg == null ? null : prevTransitLeg.getTransferToNextLeg()),
      (ConstrainedTransfer) pathLeg.getConstrainedTransferAfterLeg(),
      toOtpDomainCost(pathLeg.generalizedCost()),
      null
    );
  }

  private List<Leg> mapTransferLeg(TransferPathLeg<TripSchedule> pathLeg, LegMode transferMode) {
    var transferFromStop = transitLayer.getStopByIndex(pathLeg.fromStop());
    var transferToStop = transitLayer.getStopByIndex(pathLeg.toStop());
    Transfer transfer = ((TransferWithDuration) pathLeg.transfer()).transfer();

    Place from = Place.forStop(transferFromStop);
    Place to = Place.forStop(transferToStop);
    return mapNonTransitLeg(pathLeg, transfer, transferMode, from, to);
  }

  private Itinerary mapEgressLeg(EgressPathLeg<TripSchedule> egressPathLeg) {
    AccessEgress egressPath = (AccessEgress) egressPathLeg.egress();

    if (egressPath.durationInSeconds() == 0) {
      return null;
    }

    GraphPath graphPath = new GraphPath(egressPath.getLastState());

    Itinerary subItinerary = graphPathToItineraryMapper.generateItinerary(graphPath);

    if (subItinerary.getLegs().isEmpty()) {
      return null;
    }

    return subItinerary.withTimeShiftToStartAt(createZonedDateTime(egressPathLeg.fromTime()));
  }

  private List<Leg> mapNonTransitLeg(
    PathLeg<TripSchedule> pathLeg,
    Transfer transfer,
    LegMode transferMode,
    Place from,
    Place to
  ) {
    List<Edge> edges = transfer.getEdges();
    if (edges == null || edges.isEmpty()) {
      return List.of(
        StreetLeg
          .create()
          .withMode(transferMode)
          .withStartTime(createZonedDateTime(pathLeg.fromTime()))
          .withEndTime(createZonedDateTime(pathLeg.toTime()))
          .withFrom(from)
          .withTo(to)
          .withDistanceMeters(transfer.getDistanceMeters())
          .withGeneralizedCost(toOtpDomainCost(pathLeg.generalizedCost()))
          .withGeometry(GeometryUtils.makeLineString(transfer.getCoordinates()))
          .withWalkSteps(List.of())
          .build()
      );
    } else {
      // A RoutingRequest with a RoutingContext must be constructed so that the edges
      // may be re-traversed to create the leg(s) from the list of edges.
      RouteRequest traverseRequest = Transfer.prepareTransferRoutingRequest(request);
      traverseRequest.setArriveBy(false);
      RoutingContext routingContext = new RoutingContext(
        traverseRequest,
        graph,
        (Vertex) null,
        null
      );

      StateEditor se = new StateEditor(routingContext, edges.get(0).getFromVertex());
      se.setTimeSeconds(createZonedDateTime(pathLeg.fromTime()).toEpochSecond());

      State s = se.makeState();
      ArrayList<State> transferStates = new ArrayList<>();
      transferStates.add(s);
      for (Edge e : edges) {
        s = e.traverse(s);
        transferStates.add(s);
      }

      State[] states = transferStates.toArray(new State[0]);
      GraphPath graphPath = new GraphPath(states[states.length - 1]);

      Itinerary subItinerary = graphPathToItineraryMapper.generateItinerary(graphPath);

      if (subItinerary.getLegs().isEmpty()) {
        return List.of();
      }

      return subItinerary.getLegs();
    }
  }

  private ZonedDateTime createZonedDateTime(int timeInSeconds) {
    return transitSearchTimeZero.plusSeconds(timeInSeconds);
  }
}
