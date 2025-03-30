package org.opentripplanner.model.impl;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContextBuilder;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;

public class OtpTransitServiceImplTest {

  private static final FeedScopedId STATION_ID = TimetableRepositoryForTest.id("station");

  // The subject is used as read only; hence static is ok
  private static OtpTransitService subject;

  @BeforeAll
  public static void setup() throws IOException {
    GtfsContextBuilder contextBuilder = contextBuilder(FEED_ID, ConstantsForTests.SIMPLE_GTFS);
    OtpTransitServiceBuilder builder = contextBuilder.getTransitBuilder();

    // Supplement test data with at least one entity in all collections
    builder.getFeedInfos().add(FeedInfo.dummyForTest(FEED_ID));

    subject = builder.build();
  }

  @Test
  public void testGetAllAgencies() {
    Collection<Agency> agencies = subject.getAllAgencies();
    Agency agency = first(agencies);

    assertEquals(1, agencies.size());
    assertEquals("agency", agency.getId().getId());
    assertEquals("Fake Agency", agency.getName());
  }

  @Test
  public void testGetAllFeedInfos() {
    Collection<FeedInfo> feedInfos = subject.getAllFeedInfos();

    assertEquals(1, feedInfos.size());
    assertEquals("FeedInfo{F}", first(feedInfos).toString());
  }

  @Test
  public void testGetAllPathways() {
    Collection<Pathway> pathways = subject.getAllPathways();

    assertEquals(3, pathways.size());
    assertEquals("Pathway{F:pathways_1_1}", first(pathways).toString());
  }

  @Test
  public void testGetAllTransfers() {
    var result = removeFeedScope(
      subject.getAllTransfers().stream().map(Object::toString).sorted().collect(joining("\n"))
    );

    assertEquals(
      """
      ConstrainedTransfer{from: RouteTP{2, stop D}, to: RouteTP{5, stop I}, constraint: {guaranteed}}
      ConstrainedTransfer{from: StopTP{F}, to: StopTP{E}, constraint: {minTransferTime: 20m}}
      ConstrainedTransfer{from: StopTP{K}, to: StopTP{L}, constraint: {priority: RECOMMENDED}}
      ConstrainedTransfer{from: StopTP{K}, to: StopTP{M}, constraint: {priority: NOT_ALLOWED}}
      ConstrainedTransfer{from: StopTP{L}, to: StopTP{K}, constraint: {priority: RECOMMENDED}}
      ConstrainedTransfer{from: StopTP{M}, to: StopTP{K}, constraint: {priority: NOT_ALLOWED}}
      ConstrainedTransfer{from: TripTP{1.1, stopPos 1}, to: TripTP{2.2, stopPos 0}, constraint: {guaranteed}}""",
      result
    );
  }

  @Test
  public void testGetAllStations() {
    Collection<Station> stations = subject.siteRepository().listStations();

    assertEquals(1, stations.size());
    assertEquals("Station{F:station station}", first(stations).toString());
  }

  @Test
  public void testGetAllStops() {
    Collection<RegularStop> stops = subject.siteRepository().listRegularStops();

    assertEquals(25, stops.size());
    assertEquals("RegularStop{F:A A}", first(stops).toString());
  }

  @Test
  public void testGetAllStopTimes() {
    List<StopTime> stopTimes = new ArrayList<>();
    for (Trip trip : subject.getAllTrips()) {
      stopTimes.addAll(subject.getStopTimesForTrip(trip));
    }

    assertEquals(88, stopTimes.size());
    assertEquals(
      "StopTime(seq=1 stop=F:A trip=F:1.1 times=00:00:00-00:00:00)",
      first(stopTimes).toString()
    );
  }

  @Test
  public void testListTrips() {
    Collection<Trip> trips = subject.getAllTrips();

    assertEquals(34, trips.size());
    assertEquals("Trip{F:1.1 1}", first(trips).toString());
  }

  @Test
  public void testGetStopForId() {
    RegularStop stop = subject.siteRepository().getRegularStop(TimetableRepositoryForTest.id("P"));
    assertEquals("RegularStop{F:P P}", stop.toString());
  }

  @Test
  public void testGetStopsForStation() {
    List<StopLocation> stops = new ArrayList<>(
      subject.siteRepository().getStationById(STATION_ID).getChildStops()
    );
    assertEquals("[RegularStop{F:A A}]", stops.toString());
  }

  @Test
  public void testGetShapePointsForShapeId() {
    List<ShapePoint> shapePoints = subject.getShapePointsForShapeId(
      TimetableRepositoryForTest.id("5")
    );
    assertEquals(
      "[#1 (41,-72), #2 (41,-72), #3 (40,-72), #4 (41,-73), #5 (41,-74)]",
      shapePoints.stream().map(OtpTransitServiceImplTest::toString).toList().toString()
    );
  }

  @Test
  public void testGetStopTimesForTrip() {
    List<StopTime> stopTimes = subject.getStopTimesForTrip(first(subject.getAllTrips()));
    assertEquals(
      "[RegularStop{F:A A}, RegularStop{F:B B}, RegularStop{F:C C}]",
      stopTimes.stream().map(StopTime::getStop).toList().toString()
    );
  }

  @Test
  public void testGetAllServiceIds() {
    Collection<FeedScopedId> serviceIds = subject.getAllServiceIds();

    assertEquals(2, serviceIds.size());
    assertEquals("F:alldays", first(serviceIds).toString());
  }

  @Test
  public void testHasActiveTransit() {
    assertTrue(subject.hasActiveTransit());
  }

  private static String removeFeedScope(String text) {
    return text.replace("agency:", "").replace(FEED_ID + ":", "");
  }

  private static <T> List<T> sort(Collection<? extends T> c) {
    return c.stream().sorted(comparing(T::toString)).collect(toList());
  }

  private static <T> T first(Collection<? extends T> c) {
    return c.stream().min(comparing(T::toString)).orElseThrow();
  }

  private static String toString(ShapePoint sp) {
    int lat = (int) sp.getLat();
    int lon = (int) sp.getLon();
    return "#" + sp.getSequence() + " (" + lat + "," + lon + ")";
  }
}
