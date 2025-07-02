package org.opentripplanner.transit.model._data;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.GroupOfRoutesBuilder;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.RouteBuilder;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.network.TripPatternBuilder;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.AreaStopBuilder;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.RegularStopBuilder;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StationBuilder;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopTransferPriority;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripBuilder;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * Test utility class to help construct valid transit model objects.
 * <p>
 * TODO: This need cleanup - it has static factory methods. This is not safe, since
 *       all objects created will be created in the same context. All stops are created
 *       withing the context of a SiteRepository, mixing more than one model in a test is sharing
 *       state between tests. For now, it is just the stop index - but we want to
 *       use this to encapsulate the SiteRepository completely.
 */
public class TimetableRepositoryForTest {

  public static final String FEED_ID = "F";
  public static final String TIME_ZONE_ID = "Europe/Paris";
  public static final String OTHER_TIME_ZONE_ID = "America/Los_Angeles";
  public static final WgsCoordinate ANY_COORDINATE = new WgsCoordinate(60.0, 10.0);

  // This is used to create valid objects - do not use it for verification
  private static final Polygon ANY_POLYGON = GeometryUtils.getGeometryFactory()
    .createPolygon(
      new Coordinate[] {
        Coordinates.of(61.0, 10.0),
        Coordinates.of(61.0, 12.0),
        Coordinates.of(60.0, 11.0),
        Coordinates.of(61.0, 10.0),
      }
    );

  public static final Agency AGENCY = Agency.of(id("A1"))
    .withName("Agency Test")
    .withTimezone(TIME_ZONE_ID)
    .withUrl("https://www.agency.com")
    .build();

  public static final Agency OTHER_AGENCY = Agency.of(id("AX"))
    .withName("Other Agency Test")
    .withTimezone(TIME_ZONE_ID)
    .withUrl("https://www.otheragency.com")
    .build();
  public static final Agency OTHER_FEED_AGENCY = Agency.of(
    FeedScopedId.ofNullable("F2", "other.feed-agency")
  )
    .withName("Other feed agency")
    .withTimezone(TIME_ZONE_ID)
    .withUrl("https:/www.otherfeedagency.com")
    .build();

  private final SiteRepositoryBuilder siteRepositoryBuilder;

  public TimetableRepositoryForTest(SiteRepositoryBuilder siteRepositoryBuilder) {
    this.siteRepositoryBuilder = siteRepositoryBuilder;
  }

  public static TimetableRepositoryForTest of() {
    return new TimetableRepositoryForTest(SiteRepository.of());
  }

  public static FeedScopedId id(String id) {
    return new FeedScopedId(FEED_ID, id);
  }

  public static Agency agency(String name) {
    return AGENCY.copy().withId(id(name)).withName(name).build();
  }

  /** Create a valid Bus Route to use in unit tests */
  public static RouteBuilder route(String id) {
    return Route.of(id(id)).withAgency(AGENCY).withShortName("R" + id).withMode(TransitMode.BUS);
  }

  /**
   * Create groupOfRoutes entity to use in unit tests
   */
  public static GroupOfRoutesBuilder groupOfRoutes(String id) {
    return GroupOfRoutes.of(id(id)).withName("GOR" + id);
  }

  /** Create a valid Bus Route to use in unit tests */
  public static RouteBuilder route(FeedScopedId id) {
    return Route.of(id).withAgency(AGENCY).withShortName("R" + id).withMode(TransitMode.BUS);
  }

  public static TripPatternBuilder tripPattern(String id, Route route) {
    return TripPattern.of(id(id)).withRoute(route);
  }

  /** Create a valid Bus Route to use in unit tests */
  public static TripBuilder trip(String id) {
    return Trip.of(id(id)).withRoute(route("R" + id).build());
  }

  public static TripBuilder trip(String feedId, String tripId) {
    return Trip.of(FeedScopedId.ofNullable(feedId, tripId)).withRoute(route("R" + tripId).build());
  }

  public SiteRepositoryBuilder siteRepositoryBuilder() {
    return siteRepositoryBuilder;
  }

  /**
   * Create a stop with all required fields set.
   */
  public RegularStopBuilder stop(String idAndName) {
    return siteRepositoryBuilder
      .regularStop(id(idAndName))
      .withName(new NonLocalizedString(idAndName))
      .withCode(idAndName)
      .withCoordinate(ANY_COORDINATE);
  }

  public RegularStopBuilder stop(String idAndName, double lat, double lon) {
    return stop(idAndName).withCoordinate(lat, lon);
  }

  public StationBuilder station(String idAndName) {
    return Station.of(new FeedScopedId(FEED_ID, idAndName))
      .withName(new NonLocalizedString(idAndName))
      .withCode(idAndName)
      .withCoordinate(60.0, 10.0)
      .withDescription(new NonLocalizedString("Station " + idAndName))
      .withPriority(StopTransferPriority.ALLOWED);
  }

  public GroupStop groupStop(String idAndName, RegularStop... stops) {
    var builder = siteRepositoryBuilder
      .groupStop(id(idAndName))
      .withName(new NonLocalizedString(idAndName));

    Stream.of(stops).forEach(builder::addLocation);

    return builder.build();
  }

  public AreaStopBuilder areaStop(String idAndName) {
    return siteRepositoryBuilder
      .areaStop(id(idAndName))
      .withName(new NonLocalizedString(idAndName))
      .withGeometry(ANY_POLYGON);
  }

  public StopTime stopTime(Trip trip, int seq) {
    var stopTime = new StopTime();
    stopTime.setTrip(trip);
    stopTime.setStopSequence(seq);

    var stop = stop("stop-" + seq, 0, 0).build();
    stopTime.setStop(stop);

    return stopTime;
  }

  public StopTime stopTime(Trip trip, int seq, StopLocation stop) {
    var stopTime = new StopTime();
    stopTime.setTrip(trip);
    stopTime.setStopSequence(seq);
    stopTime.setStop(stop);

    return stopTime;
  }

  public StopTime stopTime(Trip trip, int seq, int time) {
    var stopTime = stopTime(trip, seq);
    stopTime.setArrivalTime(time);
    stopTime.setDepartureTime(time);
    stopTime.setStopHeadsign(I18NString.of("Stop headsign at stop %s".formatted(seq)));
    return stopTime;
  }

  public StopTime stopTime(Trip trip, int seq, int arrival, int departure) {
    var stopTime = stopTime(trip, seq);
    stopTime.setArrivalTime(arrival);
    stopTime.setDepartureTime(departure);
    stopTime.setStopHeadsign(I18NString.of("Stop headsign at stop %s".formatted(seq)));
    return stopTime;
  }

  public Place place(String name, Consumer<RegularStopBuilder> stopBuilder) {
    var stop = stop(name);
    stopBuilder.accept(stop);
    return Place.forStop(stop.build());
  }

  public Place place(String name, double lat, double lon) {
    return place(name, b -> b.withCoordinate(lat, lon));
  }

  /**
   * Generates a list of stop times of length {@code count} where each stop is 5 minutes after
   * the previous one.
   * <p>
   * The first stop has stop sequence 10, the following one has 20 and so on.
   */
  public List<StopTime> stopTimesEvery5Minutes(int count, Trip trip, String time) {
    var startTime = TimeUtils.time(time);
    return IntStream.range(0, count)
      .mapToObj(seq -> stopTime(trip, (seq + 1) * 10, startTime + (seq * 60 * 5)))
      .toList();
  }

  public StopPattern stopPattern(int numberOfStops) {
    var builder = StopPattern.create(numberOfStops);
    for (int i = 0; i < numberOfStops; i++) {
      builder.stops.with(i, stop("Stop_" + i).build());
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }

  public static StopPattern stopPattern(RegularStop... stops) {
    return stopPattern(Arrays.asList(stops));
  }

  public static StopPattern stopPattern(List<RegularStop> stops) {
    var builder = StopPattern.create(stops.size());
    for (int i = 0; i < stops.size(); i++) {
      builder.stops.with(i, stops.get(i));
      builder.pickups.with(i, PickDrop.SCHEDULED);
      builder.dropoffs.with(i, PickDrop.SCHEDULED);
    }
    return builder.build();
  }

  /**
   * Create {@link TripPatternBuilder} fully set up with the given mode.
   */
  public TripPatternBuilder pattern(TransitMode mode) {
    return tripPattern(mode.name(), route(mode.name()).withMode(mode).build()).withStopPattern(
      stopPattern(3)
    );
  }

  public UnscheduledTrip unscheduledTrip(String id, StopLocation... stops) {
    var stopTimes = Arrays.stream(stops)
      .map(s -> {
        var st = new StopTime();
        st.setStop(s);
        st.setFlexWindowStart(LocalTime.of(10, 0).toSecondOfDay());
        st.setFlexWindowEnd(LocalTime.of(18, 0).toSecondOfDay());

        return st;
      })
      .toList();
    return unscheduledTrip(id, stopTimes);
  }

  public UnscheduledTrip unscheduledTrip(String id, List<StopTime> stopTimes) {
    return UnscheduledTrip.of(id(id))
      .withTrip(trip("flex-trip").build())
      .withStopTimes(stopTimes)
      .build();
  }

  public ScheduledDeviatedTrip scheduledDeviatedTrip(String id, StopTime... stopTimes) {
    return ScheduledDeviatedTrip.of(id(id))
      .withTrip(trip("flex-trip").build())
      .withStopTimes(Arrays.asList(stopTimes))
      .build();
  }
}
