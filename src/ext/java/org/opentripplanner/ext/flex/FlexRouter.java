package org.opentripplanner.ext.flex;

import gnu.trove.set.TIntSet;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FlexRouter {

  /* Transit data */
  private final Graph graph;
  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  private final Collection<Transfer> transitTransfers;
  private final FlexIndex flexIndex;
  private final FlexPathCalculator flexPathCalculator;

  /* Request data */
  private final ZonedDateTime startOfTime;
  private final int departureTime;
  private final boolean arriveBy;

  private final TIntSet[] servicesRunning;
  private final int[] differenceFromStartOfTime;
  private final ServiceDate[] serviceDates;

  /* State */
  private List<FlexAccessTemplate> flexAccessTemplates = null;
  private List<FlexEgressTemplate> flexEgressTemplates = null;

  public FlexRouter(
      RoutingRequest request,
      int additionalPastSearchDays,
      int additionalFutureSearchDays,
      Collection<NearbyStop> streetAccesses,
      Collection<NearbyStop> egressTransfers
  ) {
    this.graph = request.rctx.graph;
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    this.transitTransfers = graph.getTransferTable().getTransfers();
    this.flexIndex = graph.index.getFlexIndex();
    this.flexPathCalculator = new DirectFlexPathCalculator(graph);

    ZoneId tz = graph.getTimeZone().toZoneId();
    Instant searchInstant = request.getDateTime().toInstant();
    LocalDate searchDate = LocalDate.ofInstant(searchInstant, tz);
    this.startOfTime = DateMapper.asStartOfService(searchDate, tz);
    this.departureTime = DateMapper.secondsSinceStartOfTime(startOfTime, searchInstant);
    this.arriveBy = request.arriveBy;

    int totalDays = additionalPastSearchDays + 1 + additionalFutureSearchDays;

    this.differenceFromStartOfTime = new int[totalDays];
    this.servicesRunning = new TIntSet[totalDays];
    this.serviceDates = new ServiceDate[totalDays];

    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      LocalDate date = searchDate.plusDays(d);
      int index = d + additionalPastSearchDays;
      ServiceDate serviceDate = new ServiceDate(date);
      differenceFromStartOfTime[index] = DateMapper.secondsSinceStartOfTime(startOfTime, date);
      servicesRunning[index] = graph.index.getServiceCodesRunningForDate().get(serviceDate);
      serviceDates[index] = serviceDate;
    }
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    this.flexAccessTemplates = streetAccesses
        .stream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)))
        .collect(Collectors.groupingBy(t2 -> t2.second))
        .values()
        .stream()
        .map(t2s -> t2s.stream().min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream)
        .flatMap(t2 -> IntStream.range(0, servicesRunning.length)
            .filter(i -> isFlexTripRunning(servicesRunning[i], t2.second))
            .mapToObj(i -> t2.second.getFlexAccessTemplates(
                t2.first,
                differenceFromStartOfTime[i],
                serviceDates[i], flexPathCalculator
            ))
            // TODO: Optimization: Could we filter here if earliestDepartureTime or latestArrivalTime is -1
            .flatMap(Function.identity()))
        .collect(Collectors.toList());
  }

  private void calculateFlexEgressTemplates() {
    if (this.flexEgressTemplates != null) { return; }

    this.flexEgressTemplates = streetEgresses
        .stream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)))
        .collect(Collectors.groupingBy(t2 -> t2.second))
        .values()
        .stream()
        .map(t2s -> t2s.stream().min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream)
        .flatMap(t2 -> IntStream.range(0, servicesRunning.length)
            .filter(i -> isFlexTripRunning(servicesRunning[i], t2.second))
            .mapToObj(i -> t2.second.getFlexEgressTemplates(
                t2.first,
                differenceFromStartOfTime[i],
                serviceDates[i], flexPathCalculator
            ))
            // TODO: Optimization: Could we filter here if earliestDepartureTime or latestArrivalTime is -1
            .flatMap(Function.identity()))
        .collect(Collectors.toList());
  }


  public Collection<Itinerary> getFlexOnlyItineraries() {
    Map<StopLocation, NearbyStop> egressTransferByStop = streetEgresses
        .stream()
        .collect(Collectors.toMap(nearbyStop -> nearbyStop.stop, Function.identity()));

    Set<StopLocation> egressStops = egressTransferByStop.keySet();

    calculateFlexAccessTemplates();
    calculateFlexEgressTemplates();

    Collection<Itinerary> itineraries = new ArrayList<>();

    for (FlexAccessTemplate template : this.flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();
      if (egressStops.contains(transferStop)) {
        NearbyStop egress = egressTransferByStop.get(transferStop);
        Itinerary itinerary = template.getDirectItinerary(egress, arriveBy, departureTime, startOfTime);
        if (itinerary != null) {
          itineraries.add(itinerary);
        }
      }
    }

    return itineraries;
  }

  public Collection<FlexAccessEgress> getFlexAccesses() {
    calculateFlexAccessTemplates();

    return this.flexAccessTemplates
        .stream()
        .flatMap(flexAccessEgressBase -> flexAccessEgressBase.getFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> getFlexEgresses() {
    calculateFlexEgressTemplates();

    return this.flexEgressTemplates
        .stream()
        .flatMap(flexAccessEgressBase -> flexAccessEgressBase.getFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  private boolean isFlexTripRunning(TIntSet services, FlexTrip flexTrip) {
    return services.contains(graph.getServiceCodes().get(flexTrip.getTrip().getServiceId()));
  }

}
