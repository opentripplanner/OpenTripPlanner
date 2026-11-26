package org.opentripplanner.netex.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.netex.NetexTestDataSupport.createCarPoolingService;
import static org.opentripplanner.netex.NetexTestDataSupport.createOperatingDay;
import static org.opentripplanner.netex.NetexTestDataSupport.createSingleJourney;
import static org.opentripplanner.netex.NetexTestDataSupport.createSingleJourneyPath;
import static org.opentripplanner.netex.NetexTestDataSupport.createVehicleMeetingPoint;
import static org.opentripplanner.netex.mapping.MappingSupport.ID_FACTORY;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.entur.netex2.model.CarPoolingService;
import org.entur.netex2.model.ContactStructure;
import org.entur.netex2.model.ServiceBookingArrangement_VersionStructure;
import org.entur.netex2.model.SingleJourney;
import org.entur.netex2.model.SingleJourneyPath;
import org.entur.netex2.model.TargetPassingTime;
import org.entur.netex2.model.VehicleMeetingPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssue;
import org.opentripplanner.graph_builder.issue.service.DefaultDataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.utils.time.TimeUtils;

class SingleJourneyPathMapperTest {

  private static final String SERVICE_ID = "RID:CarPoolingService:1";
  private static final String PATH_ID = "RID:SingleJourneyPath:1";
  private static final String HOME_ID = "RID:VehicleMeetingPoint:home";
  private static final String STATION_ID = "RID:VehicleMeetingPoint:station";
  private static final String JOURNEY_ID = "RID:SingleJourney:1";
  private static final String OTHER_JOURNEY_ID = "RID:SingleJourney:2";
  private static final String BOOKING_URL = "https://ridealong.example/booking";
  private static final LocalDate DAY = LocalDate.of(2026, 12, 1);
  private static final LocalDate OTHER_DAY = LocalDate.of(2026, 12, 2);
  private static final LocalTime T08_00 = LocalTime.of(8, 0);
  private static final LocalTime T08_15 = LocalTime.of(8, 15);

  private static final Route ROUTE = Route.of(ID_FACTORY.createId(SERVICE_ID))
    .withAgency(
      Agency.of(ID_FACTORY.createId("RID:Operator:1"))
        .withName("Ride Along")
        .withTimezone("Europe/Oslo")
        .build()
    )
    .withLongName(new NonLocalizedString("Ride Along car pool"))
    .withMode(TransitMode.CARPOOL)
    .build();

  private final NetexEntityIndex index = new NetexEntityIndex();
  private final DefaultDataImportIssueStore issueStore = new DefaultDataImportIssueStore();
  private final CalendarServiceBuilder calendarServiceBuilder = new CalendarServiceBuilder(
    ID_FACTORY
  );
  private final SingleJourneyPath path = createSingleJourneyPath(PATH_ID, HOME_ID, STATION_ID);
  private final CarPoolingService service = createCarPoolingService(
    SERVICE_ID,
    "Ride Along car pool",
    "RID:Operator:1"
  );

  @BeforeEach
  void setUp() {
    index.carPoolingServiceById.add(service);
    index.vehicleMeetingPointById.add(
      createVehicleMeetingPoint(HOME_ID, "Hill road 12", 59.95, 10.78)
    );
    index.vehicleMeetingPointById.add(
      createVehicleMeetingPoint(STATION_ID, "Central station", 59.91, 10.75)
    );
    index.operatingDayById.add(createOperatingDay(DAY.toString(), DAY));
    index.operatingDayById.add(createOperatingDay(OTHER_DAY.toString(), OTHER_DAY));
  }

  @Test
  void pathIsMappedToTripPatternWithTripForEachJourney() {
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));
    add(journey(OTHER_JOURNEY_ID, OTHER_DAY, LocalTime.of(9, 0), LocalTime.of(9, 15)));

    TripPattern tripPattern = mapTripPattern();

    assertEquals(ID_FACTORY.createId(PATH_ID), tripPattern.getId());
    assertEquals(ROUTE, tripPattern.getRoute());
    assertEquals(TransitMode.CARPOOL, tripPattern.getMode());
    assertEquals(
      List.of(HOME_ID, STATION_ID),
      tripPattern
        .getStops()
        .stream()
        .map(stop -> stop.getId().getId())
        .toList()
    );
    assertEquals(List.of(JOURNEY_ID, OTHER_JOURNEY_ID), tripIds(tripPattern));
  }

  @Test
  void journeyIsMappedToTripRunningOnItsOperatingDay() {
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));

    Trip trip = mapTripPattern().scheduledTripsAsStream().findFirst().orElseThrow();

    assertEquals(ID_FACTORY.createId(JOURNEY_ID), trip.getId());
    assertEquals(ROUTE, trip.getRoute());
    assertEquals(TransitMode.CARPOOL, trip.getMode());
    assertEquals("Central station", trip.getHeadsign().toString());
    assertEquals(
      List.of(ServiceCalendarDate.create(trip.getServiceId(), DAY)),
      List.copyOf(calendarServiceBuilder.createServiceCalendar())
    );
  }

  @Test
  void passingTimesAreMappedToStopTimes() {
    var journey = journey(JOURNEY_ID, DAY, T08_00, null);
    lastPassingTime(journey).setAimedArrivalTime(T08_15);
    add(journey);

    List<StopTime> stopTimes = mapStopTimes();

    assertEquals(2, stopTimes.size());
    assertEquals(TimeUtils.time("08:00"), stopTimes.getFirst().getArrivalTime());
    assertEquals(TimeUtils.time("08:00"), stopTimes.getFirst().getDepartureTime());
    assertEquals(TimeUtils.time("08:15"), stopTimes.getLast().getArrivalTime());
    assertEquals(TimeUtils.time("08:15"), stopTimes.getLast().getDepartureTime());
  }

  @Test
  void passingTimeWithDayOffsetIsOnTheNextDay() {
    var journey = journey(JOURNEY_ID, DAY, LocalTime.of(23, 50), LocalTime.of(0, 10));
    lastPassingTime(journey).setDepartureDayOffset(BigInteger.ONE);
    add(journey);

    assertEquals(TimeUtils.time("24:10"), mapStopTimes().getLast().getDepartureTime());
  }

  @Test
  void journeyWithTimeTooFarFromItsOperatingDayIsSkipped() {
    var journeyNextMonth = journey(OTHER_JOURNEY_ID, OTHER_DAY, T08_00, T08_15);
    lastPassingTime(journeyNextMonth).setDepartureDayOffset(BigInteger.valueOf(30));
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));
    add(journeyNextMonth);

    var result = mapPath().orElseThrow();

    assertEquals(List.of(JOURNEY_ID), tripIds(result.tripPattern()));
    assertEquals(1, result.stopTimesByTrip().size());
    assertEquals(List.of("TripTimeOutOfRange"), issueTypes());
  }

  @Test
  void pathWithoutJourneyWithValidTimesIsNotMapped() {
    var journeyNextMonth = journey(JOURNEY_ID, DAY, T08_00, T08_15);
    lastPassingTime(journeyNextMonth).setDepartureDayOffset(BigInteger.valueOf(30));
    add(journeyNextMonth);

    assertTrue(mapPath().isEmpty());
  }

  @Test
  void bookingArrangementOfServiceAppliesToEveryStop() {
    service.setServiceBookingArrangements(
      new ServiceBookingArrangement_VersionStructure().withBookingContact(
        new ContactStructure().withUrl(BOOKING_URL)
      )
    );
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));

    for (StopTime stopTime : mapStopTimes()) {
      assertEquals(BOOKING_URL, stopTime.getPickupBookingInfo().getContactInfo().getBookingUrl());
      assertEquals(BOOKING_URL, stopTime.getDropOffBookingInfo().getContactInfo().getBookingUrl());
    }
  }

  @Test
  void pathWithoutJourneysIsNotMapped() {
    assertTrue(mapPath().isEmpty());
  }

  @Test
  void pathThroughMeetingPointWithoutStopIsSkipped() {
    // The station has no location, and is not assigned to a quay
    index.vehicleMeetingPointById.add(new VehicleMeetingPoint().withId(STATION_ID));
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));

    assertTrue(mapPath().isEmpty());
    assertEquals(List.of("SingleJourneyPathStopNotFound"), issueTypes());
  }

  @Test
  void pathWithPointWithoutMeetingPointIsSkipped() {
    add(journey(JOURNEY_ID, DAY, T08_00, T08_15));
    path.getPointsInSequence().getVehicleMeetingPointInPath().getLast().setPointRef(null);

    assertTrue(mapPath().isEmpty());
    assertEquals(List.of("SingleJourneyPathStopNotFound"), issueTypes());
  }

  private SingleJourney journey(String id, LocalDate day, LocalTime... departureTimes) {
    return createSingleJourney(id, SERVICE_ID, day.toString(), path, departureTimes);
  }

  private static TargetPassingTime lastPassingTime(SingleJourney journey) {
    return journey.getDatedPassingTimes().getTargetPassingTime().getLast();
  }

  private void add(SingleJourney journey) {
    index.singleJourneyById.add(journey);
  }

  private Optional<SingleJourneyPathMapperResult> mapPath() {
    EntityById<Route> routesById = new DefaultEntityById<>();
    routesById.add(ROUTE);
    var mapper = new SingleJourneyPathMapper(
      issueStore,
      ID_FACTORY,
      routesById,
      SiteRepository.of(),
      index.readOnlyView(),
      calendarServiceBuilder,
      new Deduplicator()
    );
    return mapper.mapTripPattern(path);
  }

  private TripPattern mapTripPattern() {
    return mapPath().orElseThrow().tripPattern();
  }

  /** The stop times of the first trip. */
  private List<StopTime> mapStopTimes() {
    return mapPath().orElseThrow().stopTimesByTrip().values().iterator().next();
  }

  private static List<String> tripIds(TripPattern tripPattern) {
    return tripPattern
      .scheduledTripsAsStream()
      .map(trip -> trip.getId().getId())
      .toList();
  }

  private List<String> issueTypes() {
    return issueStore.listIssues().stream().map(DataImportIssue::getType).toList();
  }
}
