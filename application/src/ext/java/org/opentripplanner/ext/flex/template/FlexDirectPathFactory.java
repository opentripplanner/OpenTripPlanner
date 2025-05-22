package org.opentripplanner.ext.flex.template;

import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.filter.expr.Matcher;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;

public class FlexDirectPathFactory {

  private final FlexAccessEgressCallbackAdapter callbackService;
  private final FlexPathCalculator accessPathCalculator;
  private final FlexPathCalculator egressPathCalculator;
  private final Duration maxTransferDuration;
  private final Matcher<Trip> matcher;

  public FlexDirectPathFactory(
    FlexAccessEgressCallbackAdapter callbackService,
    FlexPathCalculator accessPathCalculator,
    FlexPathCalculator egressPathCalculator,
    Duration maxTransferDuration,
    Matcher<Trip> matcher
  ) {
    this.callbackService = callbackService;
    this.accessPathCalculator = accessPathCalculator;
    this.egressPathCalculator = egressPathCalculator;
    this.maxTransferDuration = maxTransferDuration;
    this.matcher = matcher;
  }

  public Collection<DirectFlexPath> calculateDirectFlexPaths(
    Collection<NearbyStop> streetAccesses,
    Collection<NearbyStop> streetEgresses,
    List<FlexServiceDate> dates,
    int requestTime,
    boolean arriveBy
  ) {
    Collection<DirectFlexPath> directFlexPaths = new ArrayList<>();

    var flexAccessTemplates = new FlexAccessFactory(
      callbackService,
      accessPathCalculator,
      maxTransferDuration,
      matcher
    ).calculateFlexAccessTemplates(streetAccesses, dates);

    var flexEgressTemplates = new FlexEgressFactory(
      callbackService,
      egressPathCalculator,
      maxTransferDuration,
      matcher
    ).calculateFlexEgressTemplates(streetEgresses, dates);

    Multimap<StopLocation, NearbyStop> streetEgressByStop = HashMultimap.create();
    streetEgresses.forEach(it -> streetEgressByStop.put(it.stop, it));

    for (FlexAccessTemplate template : flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();

      // TODO: Document or reimplement this. Why are we using the egress to see if the
      //      access-transfer-stop (last-stop) is used by at least one egress-template?
      //      Is it because:
      //      - of the group-stop expansion?
      //      - of the alight-restriction check?
      //      - nearest stop to trip match?
      //      Fix: Find out why and refactor out the business logic and reuse it.
      //      Problem: Any asymmetrical restriction which apply/do not apply to the egress,
      //               but do not apply/apply to the access, like booking-notice.
      if (
        flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))
      ) {
        for (NearbyStop egress : streetEgressByStop.get(transferStop)) {
          createDirectGraphPath(template, egress, arriveBy, requestTime).ifPresent(
            directFlexPaths::add
          );
        }
      }
    }

    return directFlexPaths;
  }

  private Optional<DirectFlexPath> createDirectGraphPath(
    FlexAccessTemplate accessTemplate,
    NearbyStop egress,
    boolean arriveBy,
    int requestTime
  ) {
    var accessNearbyStop = accessTemplate.accessEgress;
    var trip = accessTemplate.trip;
    int accessBoardStopPosition = accessTemplate.boardStopPosition;
    int accessAlightStopPosition = accessTemplate.alightStopPosition;
    int requestedBookingTime = accessTemplate.requestedBookingTime;

    var flexToVertex = egress.state.getVertex();

    if (!isRouteable(accessTemplate, flexToVertex)) {
      return Optional.empty();
    }

    var flexEdge = accessTemplate.getFlexEdge(flexToVertex, egress.stop);

    if (flexEdge == null) {
      return Optional.empty();
    }

    final State[] afterFlexState = flexEdge.traverse(accessNearbyStop.state);

    var finalStateOpt = EdgeTraverser.traverseEdges(afterFlexState[0], egress.edges);

    if (finalStateOpt.isEmpty()) {
      return Optional.empty();
    }

    var finalState = finalStateOpt.get();
    var flexDurations = accessTemplate.calculateFlexPathDurations(flexEdge, finalState);

    int timeShift;

    if (arriveBy) {
      int lastStopArrivalTime = flexDurations.mapToFlexTripArrivalTime(requestTime);
      int latestArrivalTime = trip.latestArrivalTime(
        lastStopArrivalTime,
        accessBoardStopPosition,
        accessAlightStopPosition,
        flexDurations.trip()
      );

      if (latestArrivalTime == MISSING_VALUE) {
        return Optional.empty();
      }

      // No need to time-shift latestArrivalTime for meeting the min-booking notice restriction,
      // the time is already as-late-as-possible
      var bookingInfo = RoutingBookingInfo.of(
        requestedBookingTime,
        trip.getPickupBookingInfo(accessTemplate.boardStopPosition)
      );
      if (bookingInfo.exceedsMinimumBookingNotice(latestArrivalTime)) {
        return Optional.empty();
      }

      // Shift from departing at departureTime to arriving at departureTime
      timeShift = flexDurations.mapToRouterArrivalTime(latestArrivalTime) - flexDurations.total();
    } else {
      int firstStopDepartureTime = flexDurations.mapToFlexTripDepartureTime(requestTime);

      // Time-shift departure so the minimum-booking-notice restriction is honored.
      var bookingInfo = trip.getPickupBookingInfo(accessBoardStopPosition);
      firstStopDepartureTime = RoutingBookingInfo.of(
        requestedBookingTime,
        bookingInfo
      ).earliestDepartureTime(firstStopDepartureTime);

      int earliestDepartureTime = trip.earliestDepartureTime(
        firstStopDepartureTime,
        accessBoardStopPosition,
        accessAlightStopPosition,
        flexDurations.trip()
      );

      if (earliestDepartureTime == MISSING_VALUE) {
        return Optional.empty();
      }

      timeShift = flexDurations.mapToRouterDepartureTime(earliestDepartureTime);
    }

    return Optional.of(new DirectFlexPath(timeShift, finalState));
  }

  protected boolean isRouteable(FlexAccessTemplate accessTemplate, Vertex flexVertex) {
    if (accessTemplate.accessEgress.state.getVertex() == flexVertex) {
      return false;
    } else return (
      accessTemplate.calculator.calculateFlexPath(
        accessTemplate.accessEgress.state.getVertex(),
        flexVertex,
        accessTemplate.boardStopPosition,
        accessTemplate.alightStopPosition
      ) !=
      null
    );
  }
}
