package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessEgressCallbackService;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.ext.flex.template.FlexServiceDate;
import org.opentripplanner.ext.flex.template.FlexTemplateFactory;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public class FlexRouter implements FlexAccessEgressCallbackService {

  /* Transit data */

  private final Graph graph;
  private final TransitService transitService;
  private final FlexParameters flexParameters;
  private final Collection<NearbyStop> streetAccesses;
  private final Collection<NearbyStop> streetEgresses;
  private final FlexIndex flexIndex;
  private final FlexPathCalculator accessFlexPathCalculator;
  private final FlexPathCalculator egressFlexPathCalculator;
  private final GraphPathToItineraryMapper graphPathToItineraryMapper;

  /* Request data */
  private final ZonedDateTime startOfTime;
  private final int departureTime;
  private final boolean arriveBy;
  private final List<FlexServiceDate> dates;

  public FlexRouter(
    Graph graph,
    TransitService transitService,
    FlexParameters flexParameters,
    Instant searchInstant,
    boolean arriveBy,
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    Collection<NearbyStop> streetAccesses,
    Collection<NearbyStop> egressTransfers
  ) {
    this.graph = graph;
    this.transitService = transitService;
    this.flexParameters = flexParameters;
    this.streetAccesses = streetAccesses;
    this.streetEgresses = egressTransfers;
    this.flexIndex = transitService.getFlexIndex();
    this.graphPathToItineraryMapper =
      new GraphPathToItineraryMapper(
        transitService.getTimeZone(),
        graph.streetNotesService,
        graph.ellipsoidToGeoidDifference
      );

    if (graph.hasStreets) {
      this.accessFlexPathCalculator =
        new StreetFlexPathCalculator(false, this.flexParameters.maxFlexTripDuration());
      this.egressFlexPathCalculator =
        new StreetFlexPathCalculator(true, this.flexParameters.maxFlexTripDuration());
    } else {
      // this is only really useful in tests. in real world scenarios you're unlikely to get useful
      // results if you don't have streets
      this.accessFlexPathCalculator = new DirectFlexPathCalculator();
      this.egressFlexPathCalculator = new DirectFlexPathCalculator();
    }

    ZoneId tz = transitService.getTimeZone();
    LocalDate searchDate = LocalDate.ofInstant(searchInstant, tz);
    this.startOfTime = ServiceDateUtils.asStartOfService(searchDate, tz);
    this.departureTime = ServiceDateUtils.secondsSinceStartOfTime(startOfTime, searchInstant);
    this.arriveBy = arriveBy;
    this.dates =
      createFlexServiceDates(
        transitService,
        additionalPastSearchDays,
        additionalFutureSearchDays,
        searchDate
      );
  }

  public Collection<Itinerary> createFlexOnlyItineraries() {
    OTPRequestTimeoutException.checkForTimeout();
    var flexAccessTemplates = calculateFlexAccessTemplates();
    var flexEgressTemplates = calculateFlexEgressTemplates();

    Multimap<StopLocation, NearbyStop> streetEgressByStop = HashMultimap.create();
    streetEgresses.forEach(it -> streetEgressByStop.put(it.stop, it));

    Collection<Itinerary> itineraries = new ArrayList<>();

    for (FlexAccessTemplate template : flexAccessTemplates) {
      StopLocation transferStop = template.getTransferStop();
      if (
        flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))
      ) {
        for (NearbyStop egress : streetEgressByStop.get(transferStop)) {
          var directFlexPath = template.createDirectGraphPath(egress, arriveBy, departureTime);
          if (directFlexPath.isPresent()) {
            var startTime = startOfTime.plusSeconds(directFlexPath.get().startTime());
            var itinerary = graphPathToItineraryMapper
              .generateItinerary(new GraphPath<>(directFlexPath.get().state()))
              .withTimeShiftToStartAt(startTime);

            if (itinerary != null) {
              itineraries.add(itinerary);
            }
          }
        }
      }
    }

    return itineraries;
  }

  public Collection<FlexAccessEgress> createFlexAccesses() {
    OTPRequestTimeoutException.checkForTimeout();
    var flexAccessTemplates = calculateFlexAccessTemplates();

    return flexAccessTemplates
      .stream()
      .flatMap(template -> template.createFlexAccessEgressStream(this))
      .toList();
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
    OTPRequestTimeoutException.checkForTimeout();
    var flexEgressTemplates = calculateFlexEgressTemplates();

    return flexEgressTemplates
      .stream()
      .flatMap(template -> template.createFlexAccessEgressStream(this))
      .toList();
  }

  @Override
  public TransitStopVertex getStopVertexForStopId(FeedScopedId stopId) {
    return graph.getStopVertexForStopId(stopId);
  }

  @Override
  public Collection<PathTransfer> getTransfersFromStop(StopLocation stop) {
    return transitService.getTransfersByStop(stop);
  }

  @Override
  public Collection<PathTransfer> getTransfersToStop(StopLocation stop) {
    return transitService.getFlexIndex().getTransfersToStop(stop);
  }

  @Override
  public boolean isDateActive(FlexServiceDate date, FlexTrip<?, ?> trip) {
    return date.isFlexTripRunning(trip, transitService);
  }

  private List<FlexAccessTemplate> calculateFlexAccessTemplates() {
    var templateFactory = FlexTemplateFactory.of(
      accessFlexPathCalculator,
      flexParameters.maxTransferDuration()
    );

    // Fetch the closest flexTrips reachable from the access stops
    return getClosestFlexTrips(streetAccesses, true)
      // For each date the router has data for
      .flatMap(it ->
        dates
          .stream()
          // Discard if service is not running on date
          .filter(date -> isDateActive(date, it.flexTrip()))
          // Create templates from trip, boarding at the nearbyStop
          .flatMap(date ->
            templateFactory.createAccessTemplates(date, it.flexTrip(), it.accessEgress()).stream()
          )
      )
      .toList();
  }

  private List<FlexEgressTemplate> calculateFlexEgressTemplates() {
    var templateFactory = FlexTemplateFactory.of(
      egressFlexPathCalculator,
      flexParameters.maxTransferDuration()
    );

    // Fetch the closest flexTrips reachable from the egress stops
    return getClosestFlexTrips(streetEgresses, false)
      // For each date the router has data for
      .flatMap(it ->
        dates
          .stream()
          // Discard if service is not running on date
          .filter(date -> isDateActive(date, it.flexTrip()))
          // Create templates from trips, alighting at the nearbyStop
          .flatMap(date ->
            templateFactory.createEgressTemplates(date, it.flexTrip(), it.accessEgress()).stream()
          )
      )
      .toList();
  }

  private Stream<AccessEgressAndNearbyStop> getClosestFlexTrips(
    Collection<NearbyStop> nearbyStops,
    boolean pickup
  ) {
    // Find all trips reachable from the nearbyStops
    Stream<AccessEgressAndNearbyStop> flexTripsReachableFromNearbyStops = nearbyStops
      .stream()
      .flatMap(accessEgress ->
        flexIndex
          .getFlexTripsByStop(accessEgress.stop)
          .stream()
          .filter(flexTrip ->
            pickup
              ? flexTrip.isBoardingPossible(accessEgress.stop)
              : flexTrip.isAlightingPossible(accessEgress.stop)
          )
          .map(flexTrip -> new AccessEgressAndNearbyStop(accessEgress, flexTrip))
      );

    // Group all (NearbyStop, FlexTrip) tuples by flexTrip
    Collection<List<AccessEgressAndNearbyStop>> groupedReachableFlexTrips = flexTripsReachableFromNearbyStops
      .collect(Collectors.groupingBy(AccessEgressAndNearbyStop::flexTrip))
      .values();

    // Get the tuple with least walking time from each group
    return groupedReachableFlexTrips
      .stream()
      .map(t2s ->
        t2s
          .stream()
          .min(Comparator.comparingLong(t2 -> t2.accessEgress().state.getElapsedTimeSeconds()))
      )
      .flatMap(Optional::stream);
  }

  private List<FlexServiceDate> createFlexServiceDates(
    TransitService transitService,
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    LocalDate searchDate
  ) {
    final List<FlexServiceDate> dates = new ArrayList<>();

    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      LocalDate date = searchDate.plusDays(d);
      dates.add(
        new FlexServiceDate(
          date,
          ServiceDateUtils.secondsSinceStartOfTime(startOfTime, date),
          transitService.getServiceCodesRunningForDate(date)
        )
      );
    }
    return List.copyOf(dates);
  }

  private record AccessEgressAndNearbyStop(NearbyStop accessEgress, FlexTrip<?, ?> flexTrip) {}
}
