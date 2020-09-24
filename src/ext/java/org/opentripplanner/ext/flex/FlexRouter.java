package org.opentripplanner.ext.flex;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlexRouter {

  /* Transit data */
  private final Graph graph;
  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  private final FlexIndex flexIndex;
  private final FlexPathCalculator flexPathCalculator;

  /* Request data */
  private final ZonedDateTime startOfTime;
  private final int departureTime;
  private final boolean arriveBy;

  private final FlexServiceDate[] dates;

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
    this.flexIndex = graph.index.getFlexIndex();
    this.flexPathCalculator = new DirectFlexPathCalculator(graph);

    ZoneId tz = graph.getTimeZone().toZoneId();
    Instant searchInstant = request.getDateTime().toInstant();
    LocalDate searchDate = LocalDate.ofInstant(searchInstant, tz);
    this.startOfTime = DateMapper.asStartOfService(searchDate, tz);
    this.departureTime = DateMapper.secondsSinceStartOfTime(startOfTime, searchInstant);
    this.arriveBy = request.arriveBy;

    int totalDays = additionalPastSearchDays + 1 + additionalFutureSearchDays;

    this.dates = new FlexServiceDate[totalDays];

    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      LocalDate date = searchDate.plusDays(d);
      int index = d + additionalPastSearchDays;
      ServiceDate serviceDate = new ServiceDate(date);
      dates[index] = new FlexServiceDate(
          serviceDate,
          DateMapper.secondsSinceStartOfTime(startOfTime, date),
          graph.index.getServiceCodesRunningForDate().get(serviceDate)
      );
    }
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    Stream<T2<NearbyStop, FlexTrip>> reachableFlexTrips = getReachableFlexTrips(streetAccesses);

    this.flexAccessTemplates = reachableFlexTrips
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, boarding at the nearbyStop
            .flatMap(date -> t2.second.getFlexAccessTemplates(t2.first, date, flexPathCalculator)))
        .collect(Collectors.toList());
  }

  private void calculateFlexEgressTemplates() {
    if (this.flexEgressTemplates != null) { return; }

    Stream<T2<NearbyStop, FlexTrip>> reachableFlexTrips = getReachableFlexTrips(streetEgresses);

    this.flexEgressTemplates = reachableFlexTrips
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, alighting at the nearbyStop
            .flatMap(date -> t2.second.getFlexEgressTemplates(t2.first, date, flexPathCalculator)))
        .collect(Collectors.toList());;
  }


  public Collection<Itinerary> getFlexOnlyItineraries() {
    calculateFlexAccessTemplates();
    calculateFlexEgressTemplates();

    Map<StopLocation, NearbyStop> streetEgressByStop = streetEgresses
        .stream()
        .collect(Collectors.toMap(nearbyStop -> nearbyStop.stop, Function.identity()));

    Set<StopLocation> egressStops = streetEgressByStop.keySet();

    Collection<Itinerary> itineraries = new ArrayList<>();

    for (FlexAccessTemplate template : this.flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();
      if (egressStops.contains(transferStop)) {
        NearbyStop egress = streetEgressByStop.get(transferStop);
        Itinerary itinerary = template.createDirectItinerary(egress, arriveBy, departureTime, startOfTime);
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
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> getFlexEgresses() {
    calculateFlexEgressTemplates();

    return this.flexEgressTemplates
        .stream()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  private Stream<T2<NearbyStop, FlexTrip>> getReachableFlexTrips(Collection<NearbyStop> nearbyStops) {
    // Find all trips reachable from the nearbyStops
    Stream<T2<NearbyStop, FlexTrip>> flexTripsReachableFromNearbyStops = nearbyStops
        .stream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)));

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<T2<NearbyStop, FlexTrip>>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
        .collect(Collectors.groupingBy(t2 -> t2.second))
        .values();

    // Get the tuple with least walking time from each group
    Stream<T2<NearbyStop, FlexTrip>> nearestFlexibleTrips = groupedReachableFlexTrips
        .stream()
        .map(t2s -> t2s
            .stream()
            .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream);

    return nearestFlexibleTrips;
  }

}
