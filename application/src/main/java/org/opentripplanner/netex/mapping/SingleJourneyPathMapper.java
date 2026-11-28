package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.math.BigInteger;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.entur.netex2.model.CarPoolingService;
import org.entur.netex2.model.OperatingDay;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.SingleJourneyPath;
import org.entur.netex2.model.TargetPassingTime;
import org.entur.netex2.model.VehicleMeetingPointInPath;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.deduplicator.DeduplicatorService;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.support.ServiceJourneyHelper;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;

/**
 * Maps a NeTEx SingleJourneyPath, and the SingleJourneys following it, to an OTP TripPattern. Each
 * journey becomes a Trip which runs on the operating day of the journey. The route of the trip is
 * the car pooling service of the journey, see {@link CarPoolingServiceMapper}.
 */
class SingleJourneyPathMapper {

  private final DataImportIssueStore issueStore;
  private final FeedScopedIdFactory idFactory;
  private final EntityById<Route> routesById;
  private final ReadOnlyHierarchicalMapById<OperatingDay> operatingDayById;
  private final ReadOnlyHierarchicalMapById<CarPoolingService> carPoolingServiceById;
  private final VehicleMeetingPointMapper vehicleMeetingPointMapper;
  private final BookingInfoMapper bookingInfoMapper;
  private final CalendarServiceBuilder calendarServiceBuilder;
  private final DeduplicatorService deduplicator;
  private final Multimap<String, SingleJourney> journeysByPathId = ArrayListMultimap.create();
  private final Map<String, BookingInfo> bookingInfoByServiceId = new HashMap<>();

  SingleJourneyPathMapper(
    DataImportIssueStore issueStore,
    FeedScopedIdFactory idFactory,
    EntityById<Route> routesById,
    SiteRepositoryBuilder siteRepositoryBuilder,
    NetexEntityIndexReadOnlyView netexIndex,
    CalendarServiceBuilder calendarServiceBuilder,
    DeduplicatorService deduplicator
  ) {
    this.issueStore = issueStore;
    this.idFactory = idFactory;
    this.routesById = routesById;
    this.operatingDayById = netexIndex.getOperatingDayById();
    this.carPoolingServiceById = netexIndex.getCarPoolingServiceById();
    this.vehicleMeetingPointMapper = new VehicleMeetingPointMapper(
      idFactory,
      siteRepositoryBuilder,
      netexIndex.getVehicleMeetingPointById(),
      netexIndex.getQuayIdByVehicleMeetingPointRef()
    );
    this.bookingInfoMapper = new BookingInfoMapper(issueStore);
    this.calendarServiceBuilder = calendarServiceBuilder;
    this.deduplicator = deduplicator;

    for (SingleJourney journey : netexIndex.getSingleJourneyById().localValues()) {
      journeysByPathId.put(journey.getSingleJourneyPathRef().getRef(), journey);
    }
  }

  /**
   * @return empty if the path has no journeys, or if it can not be mapped.
   */
  Optional<SingleJourneyPathMapperResult> mapTripPattern(SingleJourneyPath path) {
    Collection<SingleJourney> journeys = journeysByPathId.get(path.getId());
    if (journeys.isEmpty()) {
      return Optional.empty();
    }
    List<RegularStop> stops = mapStops(path);
    if (stops == null) {
      return Optional.empty();
    }

    // The validation has made sure all journeys of a path belong to the same car pooling service
    String serviceId = journeys.iterator().next().getCommonVehicleServiceRef().getValue().getRef();
    Route route = routesById.get(idFactory.createId(serviceId));
    BookingInfo bookingInfo = mapBookingInfo(serviceId);

    Map<Trip, List<StopTime>> stopTimesByTrip = new LinkedHashMap<>();
    List<TripTimes<?>> tripTimes = new ArrayList<>();

    for (SingleJourney journey : journeys) {
      Trip trip = mapTrip(journey, route, stops.getLast());
      List<StopTime> stopTimes = mapStopTimes(journey, trip, stops, bookingInfo);
      try {
        tripTimes.add(TripTimesFactory.tripTimes(trip, stopTimes, deduplicator));
        stopTimesByTrip.put(trip, stopTimes);
      } catch (DataValidationException e) {
        // The times of the journey are too far from its operating day
        issueStore.add(e.error());
      }
    }
    if (tripTimes.isEmpty()) {
      return Optional.empty();
    }

    // All trips have the same stops, so the stop pattern can be created from any of them
    List<StopTime> anyStopTimes = stopTimesByTrip.values().iterator().next();
    var tripPattern = TripPattern.of(idFactory.createId(path.getId()))
      .withRoute(route)
      .withStopPattern(new StopPattern(anyStopTimes))
      .withName(
        path.getName() == null ? "" : MultilingualStringMapper.getStringValue(path.getName())
      )
      .withScheduledTimeTableBuilder(builder -> builder.addAllTripTimes(tripTimes))
      .build();

    return Optional.of(new SingleJourneyPathMapperResult(tripPattern, stopTimesByTrip));
  }

  /**
   * @return {@code null} if one of the meeting points in the path has no stop.
   */
  @Nullable
  private List<RegularStop> mapStops(SingleJourneyPath path) {
    List<RegularStop> stops = new ArrayList<>();
    var points = path.getPointsInSequence().getVehicleMeetingPointInPath();

    for (VehicleMeetingPointInPath point : points) {
      var pointRef = point.getPointRef();
      String meetingPointId = pointRef == null ? null : pointRef.getValue().getRef();
      RegularStop stop = vehicleMeetingPointMapper.mapToStop(meetingPointId);

      if (stop == null) {
        issueStore.add(
          "SingleJourneyPathStopNotFound",
          "SingleJourneyPath %s is skipped. There is no stop for VehicleMeetingPoint %s, it needs " +
            "a valid location or an assignment to an existing quay.",
          path.getId(),
          meetingPointId
        );
        return null;
      }
      stops.add(stop);
    }
    return stops;
  }

  private Trip mapTrip(SingleJourney journey, Route route, RegularStop destination) {
    OperatingDay operatingDay = operatingDayById.lookup(journey.getOperatingDayRef().getRef());
    FeedScopedId calendarId = calendarServiceBuilder.registerDatesAndGetServiceId(
      Set.of(operatingDay.getCalendarDate())
    );

    return Trip.of(idFactory.createId(journey.getId()))
      .withRoute(route)
      .withServiceId(calendarId)
      .withHeadsign(destination.getName())
      .build();
  }

  /**
   * The validation has made sure there is a passing time for each stop, in the same order. The
   * booking arrangement of the car pooling service applies to every stop.
   */
  private static List<StopTime> mapStopTimes(
    SingleJourney journey,
    Trip trip,
    List<RegularStop> stops,
    @Nullable BookingInfo bookingInfo
  ) {
    List<TargetPassingTime> passingTimes = journey.getDatedPassingTimes().getTargetPassingTime();
    List<StopTime> stopTimes = new ArrayList<>();

    for (int i = 0; i < stops.size(); i++) {
      TargetPassingTime passingTime = passingTimes.get(i);
      Integer arrivalTime = mapTime(
        passingTime.getAimedArrivalTime(),
        passingTime.getArrivalDayOffset()
      );
      Integer departureTime = mapTime(
        passingTime.getAimedDepartureTime(),
        passingTime.getDepartureDayOffset()
      );

      StopTime stopTime = new StopTime();
      stopTime.setTrip(trip);
      stopTime.setStopSequence(i);
      stopTime.setStop(stops.get(i));
      // A stop may have only one of the times, like the first and the last stop
      stopTime.setArrivalTime(arrivalTime == null ? departureTime : arrivalTime);
      stopTime.setDepartureTime(departureTime == null ? arrivalTime : departureTime);
      stopTime.setPickupBookingInfo(bookingInfo);
      stopTime.setDropOffBookingInfo(bookingInfo);
      stopTimes.add(stopTime);
    }
    return stopTimes;
  }

  /** The booking arrangement of a service is mapped once, it is the same for all its journeys. */
  @Nullable
  private BookingInfo mapBookingInfo(String serviceId) {
    if (!bookingInfoByServiceId.containsKey(serviceId)) {
      BookingInfo bookingInfo = bookingInfoMapper.map(carPoolingServiceById.lookup(serviceId));
      bookingInfoByServiceId.put(serviceId, bookingInfo);
    }
    return bookingInfoByServiceId.get(serviceId);
  }

  @Nullable
  private static Integer mapTime(@Nullable LocalTime time, @Nullable BigInteger dayOffset) {
    return time == null ? null : ServiceJourneyHelper.elapsedTimeSinceMidnight(time, dayOffset);
  }
}
