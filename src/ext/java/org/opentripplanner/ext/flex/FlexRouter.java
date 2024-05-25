package org.opentripplanner.ext.flex;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.DirectFlexPath;
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

    var directFlexPaths = calculateDirectFlexPaths();
    var itineraries = new ArrayList<Itinerary>();

    for (DirectFlexPath it : directFlexPaths) {
      var startTime = startOfTime.plusSeconds(it.startTime());
      var itinerary = graphPathToItineraryMapper
        .generateItinerary(new GraphPath<>(it.state()))
        .withTimeShiftToStartAt(startTime);

      if (itinerary != null) {
        itineraries.add(itinerary);
      }
    }
    return itineraries;
  }

  private Collection<DirectFlexPath> calculateDirectFlexPaths() {
    Collection<DirectFlexPath> directFlexPaths = new ArrayList<>();

    var flexAccessTemplates = calculateFlexAccessTemplates();
    var flexEgressTemplates = calculateFlexEgressTemplates();

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
      //      Problem: Any asymmetrical restriction witch apply/do not apply to the egress,
      //               but do not apply/apply to the access, like booking-notice.
      if (
        flexEgressTemplates.stream().anyMatch(t -> t.getAccessEgressStop().equals(transferStop))
      ) {
        for (NearbyStop egress : streetEgressByStop.get(transferStop)) {
          template
            .createDirectGraphPath(egress, arriveBy, departureTime)
            .ifPresent(directFlexPaths::add);
        }
      }
    }

    return directFlexPaths;
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
  public Collection<FlexTrip<?, ?>> getFlexTripsByStop(StopLocation stopLocation) {
    return flexIndex.getFlexTripsByStop(stopLocation);
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

    var result = new ArrayList<FlexAccessTemplate>();
    var closestFlexTrips = getClosestFlexTrips(this, streetAccesses, dates, true);

    for (var it : closestFlexTrips) {
      for (var date : it.activeDates) {
        result.addAll(templateFactory.createAccessTemplates(date, it.flexTrip(), it.nearbyStop()));
      }
    }
    return result;
  }

  private List<FlexEgressTemplate> calculateFlexEgressTemplates() {
    var templateFactory = FlexTemplateFactory.of(
      egressFlexPathCalculator,
      flexParameters.maxTransferDuration()
    );

    var result = new ArrayList<FlexEgressTemplate>();
    var closestFlexTrips = getClosestFlexTrips(this, streetEgresses, dates, false);

    for (var it : closestFlexTrips) {
      for (var date : it.activeDates) {
        result.addAll(templateFactory.createEgressTemplates(date, it.flexTrip, it.nearbyStop()));
      }
    }
    return result;
  }

  /** This method is static, so we can move it to the FlexTemplateFactory later. */
  private static Collection<ClosestTrip> getClosestFlexTrips(
    FlexAccessEgressCallbackService callbackService,
    Collection<NearbyStop> nearbyStops,
    List<FlexServiceDate> dates,
    boolean pickup
  ) {
    Map<FlexTrip<?, ?>, ClosestTrip> map = new HashMap<>();
    // Find all trips reachable from the nearbyStops
    for (NearbyStop nearbyStop : nearbyStops) {
      var stop = nearbyStop.stop;
      for (var trip : callbackService.getFlexTripsByStop(stop)) {
        int stopPos = pickup ? trip.findBoardIndex(stop) : trip.findAlightIndex(stop);
        if (stopPos != FlexTrip.STOP_INDEX_NOT_FOUND) {
          var existing = map.get(trip);
          if (existing == null || nearbyStop.isBetter(existing.nearbyStop())) {
            map.put(trip, new ClosestTrip(nearbyStop, trip, stopPos));
          }
        }
      }
    }

    // Add active dates
    for (Map.Entry<FlexTrip<?, ?>, ClosestTrip> e : map.entrySet()) {
      var closestTrip = e.getValue();
      // Include dates where the service is running
      dates
        .stream()
        .filter(date -> callbackService.isDateActive(date, e.getKey()))
        .forEach(closestTrip::addDate);
    }
    // Filter inactive trips and return
    return map.values().stream().filter(ClosestTrip::hasActiveDates).toList();
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

  /**
   * The combination of the closest stop and trip with active dates where the trip is in service.
   *
   * @param activeDates This is a mutable list, when building an instance the
   *                    {@link #addDate(FlexServiceDate)} can be used to add dates to the list.
   */
  private record ClosestTrip(
    NearbyStop nearbyStop,
    FlexTrip<?, ?> flexTrip,
    int stopPos,
    List<FlexServiceDate> activeDates
  ) {
    public ClosestTrip(NearbyStop nearbyStop, FlexTrip<?, ?> flexTrip, int stopPos) {
      this(nearbyStop, flexTrip, stopPos, new ArrayList<>());
    }

    public void addDate(FlexServiceDate date) {
      activeDates.add(date);
    }

    public boolean hasActiveDates() {
      return !activeDates.isEmpty();
    }
  }
}
