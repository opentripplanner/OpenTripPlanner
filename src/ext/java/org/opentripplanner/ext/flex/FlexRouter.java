package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.fares.FareService;
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
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FlexRouter {

  /* Transit data */
  private final Graph graph;
  private final FlexParameters config;
  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  private final FlexIndex flexIndex;
  private final FlexPathCalculator accessFlexPathCalculator;
  private final FlexPathCalculator egressFlexPathCalculator;

  /* Request data */
  private final ZonedDateTime startOfTime;
  private final int departureTime;
  private final boolean arriveBy;

  private final FlexServiceDate[] dates;

  /* State */
  private List<FlexAccessTemplate> flexAccessTemplates = null;
  private List<FlexEgressTemplate> flexEgressTemplates = null;

  public FlexRouter(
      Graph graph,
      FlexParameters config,
      Instant searchInstant,
      boolean arriveBy,
      int additionalPastSearchDays,
      int additionalFutureSearchDays,
      Collection<NearbyStop> streetAccesses,
      Collection<NearbyStop> egressTransfers
  ) {
    this.graph = graph;
    this.config = config;
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    this.flexIndex = graph.index.getFlexIndex();

    if(graph.hasStreets) {
      this.accessFlexPathCalculator = new StreetFlexPathCalculator(graph, false);
      this.egressFlexPathCalculator = new StreetFlexPathCalculator(graph, true);
    } else {
      // this is only really useful in tests. in real world scenarios you're unlikely to get useful
      // results if you don't have streets
      this.accessFlexPathCalculator = new DirectFlexPathCalculator(graph);
      this.egressFlexPathCalculator = new DirectFlexPathCalculator(graph);
    }


    ZoneId tz = graph.getTimeZone().toZoneId();
    LocalDate searchDate = LocalDate.ofInstant(searchInstant, tz);
    this.startOfTime = DateMapper.asStartOfService(searchDate, tz);
    this.departureTime = DateMapper.secondsSinceStartOfTime(startOfTime, searchInstant);
    this.arriveBy = arriveBy;

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

  public Collection<Itinerary> createFlexOnlyItineraries() {
    calculateFlexAccessTemplates();
    calculateFlexEgressTemplates();

    Multimap<StopLocation, NearbyStop> streetEgressByStop = HashMultimap.create();
    streetEgresses.forEach(it -> streetEgressByStop.put(it.stop, it));

    Collection<Itinerary> itineraries = new ArrayList<>();

    for (FlexAccessTemplate template : this.flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();
      if (this.flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))) {
        for(NearbyStop egress : streetEgressByStop.get(transferStop)) {
          Itinerary itinerary = template.createDirectItinerary(egress, arriveBy, departureTime, startOfTime);
          if (itinerary != null) {
            var fareService = graph.getService(FareService.class);
            if(fareService != null) {
              var fare = fareService.getCost(itinerary);
              itinerary.fare = fare;
            }
            itineraries.add(itinerary);
          }
        }
      }
    }

    return itineraries;
  }

  public Collection<FlexAccessEgress> createFlexAccesses() {
    calculateFlexAccessTemplates();

    return this.flexAccessTemplates
        .stream()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
    calculateFlexEgressTemplates();

    return this.flexEgressTemplates
        .stream()
        .flatMap(template -> template.createFlexAccessEgressStream(graph))
        .collect(Collectors.toList());
  }

  private void calculateFlexAccessTemplates() {
    if (this.flexAccessTemplates != null) { return; }

    // Fetch the closest flexTrips reachable from the access stops
    this.flexAccessTemplates = getClosestFlexTrips(streetAccesses, true)
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, boarding at the nearbyStop
            .flatMap(date -> t2.second.getFlexAccessTemplates(
                t2.first,
                date,
                accessFlexPathCalculator,
                config
            )))
        .collect(Collectors.toList());
  }

  private void calculateFlexEgressTemplates() {
    if (this.flexEgressTemplates != null) { return; }

    // Fetch the closest flexTrips reachable from the egress stops
    this.flexEgressTemplates = getClosestFlexTrips(streetEgresses, false)
        // For each date the router has data for
        .flatMap(t2 -> Arrays.stream(dates)
            // Discard if service is not running on date
            .filter(date -> date.isFlexTripRunning(t2.second, this.graph))
            // Create templates from trip, alighting at the nearbyStop
            .flatMap(date -> t2.second.getFlexEgressTemplates(
                t2.first, 
                date,
                egressFlexPathCalculator,
                config
            )))
        .collect(Collectors.toList());;
  }

  private Stream<T2<NearbyStop, FlexTrip>> getClosestFlexTrips(Collection<NearbyStop> nearbyStops, boolean pickup) {
    // Find all trips reachable from the nearbyStops
    Stream<T2<NearbyStop, FlexTrip>> flexTripsReachableFromNearbyStops = nearbyStops
        .stream()
        .flatMap(accessEgress -> flexIndex
            .getFlexTripsByStop(accessEgress.stop)
            .filter(flexTrip -> pickup ? flexTrip.isBoardingPossible(accessEgress) : flexTrip.isAlightingPossible(accessEgress))
            .map(flexTrip -> new T2<>(accessEgress, flexTrip)));

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<T2<NearbyStop, FlexTrip>>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
        .collect(Collectors.groupingBy(t2 -> t2.second))
        .values();

    // Get the tuple with least walking time from each group
    return groupedReachableFlexTrips
        .stream()
        .map(t2s -> t2s
            .stream()
            .min(Comparator.comparingLong(t2 -> t2.first.state.getElapsedTimeSeconds())))
        .flatMap(Optional::stream);
  }

}
