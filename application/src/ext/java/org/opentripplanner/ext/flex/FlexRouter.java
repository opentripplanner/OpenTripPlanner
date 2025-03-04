package org.opentripplanner.ext.flex;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.flex.flexpathcalculator.DirectFlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.StreetFlexPathCalculator;
import org.opentripplanner.ext.flex.template.DirectFlexPath;
import org.opentripplanner.ext.flex.template.FlexAccessEgressCallbackAdapter;
import org.opentripplanner.ext.flex.template.FlexAccessFactory;
import org.opentripplanner.ext.flex.template.FlexDirectPathFactory;
import org.opentripplanner.ext.flex.template.FlexEgressFactory;
import org.opentripplanner.ext.flex.template.FlexServiceDate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.booking.RoutingBookingInfo;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

public class FlexRouter {

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
  private final FlexAccessEgressCallbackAdapter callbackService;

  /* Request data */
  private final ZonedDateTime startOfTime;
  private final int requestedTime;
  private final int requestedBookingTime;
  private final List<FlexServiceDate> dates;

  public FlexRouter(
    Graph graph,
    TransitService transitService,
    FlexParameters flexParameters,
    Instant requestedTime,
    @Nullable Instant requestedBookingTime,
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
    this.callbackService = new CallbackAdapter();
    this.graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      transitService.getTimeZone(),
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    if (graph.hasStreets) {
      this.accessFlexPathCalculator = new StreetFlexPathCalculator(
        false,
        flexParameters.maxFlexTripDuration()
      );
      this.egressFlexPathCalculator = new StreetFlexPathCalculator(
        true,
        flexParameters.maxFlexTripDuration()
      );
    } else {
      // this is only really useful in tests. in real world scenarios you're unlikely to get useful
      // results if you don't have streets
      this.accessFlexPathCalculator = new DirectFlexPathCalculator();
      this.egressFlexPathCalculator = new DirectFlexPathCalculator();
    }

    ZoneId tz = transitService.getTimeZone();
    LocalDate searchDate = LocalDate.ofInstant(requestedTime, tz);
    this.startOfTime = ServiceDateUtils.asStartOfService(searchDate, tz);
    this.requestedTime = ServiceDateUtils.secondsSinceStartOfTime(startOfTime, requestedTime);
    this.requestedBookingTime = requestedBookingTime == null
      ? RoutingBookingInfo.NOT_SET
      : ServiceDateUtils.secondsSinceStartOfTime(startOfTime, requestedBookingTime);
    this.dates = createFlexServiceDates(
      transitService,
      additionalPastSearchDays,
      additionalFutureSearchDays,
      searchDate
    );
  }

  public List<Itinerary> createFlexOnlyItineraries(boolean arriveBy) {
    OTPRequestTimeoutException.checkForTimeout();

    var directFlexPaths = new FlexDirectPathFactory(
      callbackService,
      accessFlexPathCalculator,
      egressFlexPathCalculator,
      flexParameters.maxTransferDuration()
    ).calculateDirectFlexPaths(streetAccesses, streetEgresses, dates, requestedTime, arriveBy);

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

  public Collection<FlexAccessEgress> createFlexAccesses() {
    OTPRequestTimeoutException.checkForTimeout();

    return new FlexAccessFactory(
      callbackService,
      accessFlexPathCalculator,
      flexParameters.maxTransferDuration()
    ).createFlexAccesses(streetAccesses, dates);
  }

  public Collection<FlexAccessEgress> createFlexEgresses() {
    OTPRequestTimeoutException.checkForTimeout();
    return new FlexEgressFactory(
      callbackService,
      egressFlexPathCalculator,
      flexParameters.maxTransferDuration()
    ).createFlexEgresses(streetEgresses, dates);
  }

  private List<FlexServiceDate> createFlexServiceDates(
    TransitService transitService,
    int additionalPastSearchDays,
    int additionalFutureSearchDays,
    LocalDate searchDate
  ) {
    final List<FlexServiceDate> dates = new ArrayList<>();

    // TODO - This code id not DRY, the same logic is in RaptorRoutingRequestTransitDataCreator
    for (int d = -additionalPastSearchDays; d <= additionalFutureSearchDays; ++d) {
      LocalDate date = searchDate.plusDays(d);
      dates.add(
        new FlexServiceDate(
          date,
          ServiceDateUtils.secondsSinceStartOfTime(startOfTime, date),
          requestedBookingTime,
          transitService.getServiceCodesRunningForDate(date)
        )
      );
    }
    return List.copyOf(dates);
  }

  /**
   * This class work as an adaptor around OTP services. This allows us to pass in this instance
   * and not the implementations (graph, transitService, flexIndex). We can easily mock this in
   * unit-tests. This also serves as documentation of which services the flex access/egress
   * generation logic needs.
   */
  private class CallbackAdapter implements FlexAccessEgressCallbackAdapter {

    @Override
    public TransitStopVertex getStopVertexForStopId(FeedScopedId stopId) {
      return graph.getStopVertexForStopId(stopId);
    }

    @Override
    public Collection<PathTransfer> getTransfersFromStop(StopLocation stop) {
      return transitService.getFlexIndex().getTransfersFromStop(stop);
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
      int serviceCode = transitService.getServiceCode(trip.getTrip().getServiceId());
      return date.isTripServiceRunning(serviceCode);
    }
  }
}
