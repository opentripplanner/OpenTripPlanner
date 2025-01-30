package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner._support.time.ZoneIds.BERLIN;
import static org.opentripplanner.model.plan.PlanTestConstants.D10m;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_01;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_15;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_30;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_50;
import static org.opentripplanner.model.plan.TestItineraryBuilder.newItinerary;
import static org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle.StopStatus.IN_TRANSIT_TO;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.transit.model.basic.TransitMode.BUS;
import static org.opentripplanner.transit.model.basic.TransitMode.FERRY;
import static org.opentripplanner.transit.model.timetable.OccupancyStatus.FEW_SEATS_AVAILABLE;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.text.I18NStrings;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.ext.fares.FaresToItineraryMapper;
import org.opentripplanner.ext.fares.impl.DefaultFareService;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.RealTimeTripUpdate;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.RelativeDirection;
import org.opentripplanner.model.plan.ScheduledTransitLeg;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.model.plan.WalkStepBuilder;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.service.vehicleparking.VehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingRepository;
import org.opentripplanner.service.vehicleparking.internal.DefaultVehicleParkingService;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalService;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalVehicle;
import org.opentripplanner.standalone.config.framework.json.JsonSupport;
import org.opentripplanner.test.support.FilePatternSource;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractBuilder;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.BikeAccess;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.ListUtils;

class GraphQLIntegrationTest {

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final Place A = TEST_MODEL.place("A", 5.0, 8.0);
  private static final Place B = TEST_MODEL.place("B", 6.0, 8.5);
  private static final Place C = TEST_MODEL.place("C", 7.0, 9.0);
  private static final Place D = TEST_MODEL.place("D", 8.0, 9.5);
  private static final Place E = TEST_MODEL.place("E", 9.0, 10.0);
  private static final Place F = TEST_MODEL.place("F", 9.0, 10.5);
  private static final Place G = TEST_MODEL.place("G", 9.5, 11.0);
  private static final Place H = TEST_MODEL.place("H", 10.0, 11.5);

  private static final List<RegularStop> STOP_LOCATIONS = Stream
    .of(A, B, C, D, E, F, G, H)
    .map(p -> (RegularStop) p.stop)
    .toList();
  private static final Route ROUTE = TimetableRepositoryForTest.route("a-route").build();
  private static final String ADDED_TRIP_ID = "ADDED_TRIP";
  private static final String REPLACEMENT_TRIP_ID = "REPLACEMENT_TRIP";
  public static final ZoneId TIME_ZONE = ZoneIds.BERLIN;
  public static final String FEED_ID = TimetableRepositoryForTest.FEED_ID;

  private static final VehicleRentalStation VEHICLE_RENTAL_STATION = new TestVehicleRentalStationBuilder()
    .withVehicles(10)
    .withSpaces(10)
    .withVehicleTypeBicycle(5, 7)
    .withVehicleTypeElectricBicycle(5, 3)
    .withSystem("Network-1", "https://foo.bar")
    .build();

  private static final VehicleRentalVehicle RENTAL_VEHICLE_1 = new TestFreeFloatingRentalVehicleBuilder()
    .withSystem("Network-1", "https://foo.bar")
    .build();

  private static final VehicleRentalVehicle RENTAL_VEHICLE_2 = new TestFreeFloatingRentalVehicleBuilder()
    .withSystem("Network-2", "https://foo.bar.baz")
    .withNetwork("Network-2")
    .withCurrentRangeMeters(null)
    .withCurrentFuelPercent(null)
    .build();

  static final Graph GRAPH = new Graph();

  static final Instant ALERT_START_TIME = OffsetDateTime
    .parse("2023-02-15T12:03:28+01:00")
    .toInstant();
  static final Instant ALERT_END_TIME = ALERT_START_TIME.plus(1, ChronoUnit.DAYS);
  private static final int TEN_MINUTES = 10 * 60;

  private static final LocalDate SERVICE_DATE = LocalDate.of(2024, 1, 1);
  private static final int SERVICE_CODE = 0;

  private static GraphQLRequestContext context;

  private static final Deduplicator DEDUPLICATOR = new Deduplicator();
  private static final VehicleParkingRepository parkingRepository = new DefaultVehicleParkingRepository();

  @BeforeAll
  static void setup() {
    parkingRepository.updateVehicleParking(
      List.of(
        VehicleParking
          .builder()
          .id(id("parking-1"))
          .coordinate(WgsCoordinate.GREENWICH)
          .name(NonLocalizedString.ofNullable("parking"))
          .build()
      ),
      List.of()
    );

    var siteRepositoryBuilder = TEST_MODEL.siteRepositoryBuilder();
    STOP_LOCATIONS.forEach(siteRepositoryBuilder::withRegularStop);
    var siteRepository = siteRepositoryBuilder.build();
    var timetableRepository = new TimetableRepository(siteRepository, DEDUPLICATOR);

    var cal_id = TimetableRepositoryForTest.id("CAL_1");
    var trip = TimetableRepositoryForTest
      .trip("123")
      .withHeadsign(I18NString.of("Trip Headsign"))
      .withServiceId(cal_id)
      .build();
    var stopTimes = TEST_MODEL.stopTimesEvery5Minutes(3, trip, "11:00");
    var tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, DEDUPLICATOR);
    var trip2 = TimetableRepositoryForTest
      .trip("321Canceled")
      .withHeadsign(I18NString.of("Trip Headsign"))
      .withServiceId(cal_id)
      .build();
    var stopTimes2 = TEST_MODEL.stopTimesEvery5Minutes(3, trip2, "11:30");
    var tripTimes2 = TripTimesFactory.tripTimes(trip2, stopTimes2, DEDUPLICATOR);

    var tripToBeReplaced = TimetableRepositoryForTest
      .trip(REPLACEMENT_TRIP_ID)
      .withServiceId(cal_id)
      .build();
    final TripPattern pattern = TEST_MODEL
      .pattern(BUS)
      .withScheduledTimeTableBuilder(builder ->
        builder
          .addTripTimes(tripTimes)
          .addTripTimes(tripTimes2)
          .addTripTimes(
            TripTimesFactory.tripTimes(
              tripToBeReplaced,
              TEST_MODEL.stopTimesEvery5Minutes(3, tripToBeReplaced, "11:30"),
              DEDUPLICATOR
            )
          )
      )
      .build();

    timetableRepository.addTripPattern(id("pattern-1"), pattern);

    var feedInfo = FeedInfo.dummyForTest(FEED_ID);
    timetableRepository.addFeedInfo(feedInfo);

    var agency = Agency
      .of(new FeedScopedId(FEED_ID, "agency-xx"))
      .withName("speedtransit")
      .withUrl("www.otp-foo.bar")
      .withTimezone("Europe/Berlin")
      .build();
    timetableRepository.addAgency(agency);

    timetableRepository.initTimeZone(BERLIN);
    timetableRepository.index();
    var routes = Arrays
      .stream(TransitMode.values())
      .sorted(Comparator.comparing(Enum::name))
      .map(m ->
        TimetableRepositoryForTest
          .route(m.name())
          .withMode(m)
          .withLongName(I18NString.of("Long name for %s".formatted(m)))
          .withGtfsSortOrder(sortOrder(m))
          .withBikesAllowed(bikesAllowed(m))
          .build()
      )
      .toList();

    var busRoute = routes.stream().filter(r -> r.getMode().equals(BUS)).findFirst().get();
    // Crate a calendar (needed for testing cancelled trips)
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    var firstDate = LocalDate.of(2024, 8, 8);
    var secondDate = LocalDate.of(2024, 8, 9);
    calendarServiceData.putServiceDatesForServiceId(
      cal_id,
      List.of(firstDate, secondDate, SERVICE_DATE)
    );
    timetableRepository.getServiceCodes().put(cal_id, SERVICE_CODE);
    timetableRepository.updateCalendarServiceData(
      true,
      calendarServiceData,
      DataImportIssueStore.NOOP
    );
    timetableRepository.index();
    TimetableSnapshot timetableSnapshot = new TimetableSnapshot();
    tripTimes2.cancelTrip();
    timetableSnapshot.update(new RealTimeTripUpdate(pattern, tripTimes2, secondDate));

    TransitEditorService transitService = new DefaultTransitService(timetableRepository) {
      private final TransitAlertService alertService = new TransitAlertServiceImpl(
        timetableRepository
      );

      @Override
      public List<TransitMode> findTransitModes(StopLocation stop) {
        return List.of(BUS, FERRY);
      }

      @Override
      public TransitAlertService getTransitAlertService() {
        return alertService;
      }

      @Override
      public Set<Route> findRoutes(StopLocation stop) {
        return Set.of(ROUTE);
      }
    };
    routes.forEach(transitService::addRoutes);

    final Trip addedTrip = Trip
      .of(new FeedScopedId(FEED_ID, ADDED_TRIP_ID))
      .withRoute(busRoute)
      .build();

    for (var t : List.of(addedTrip, tripToBeReplaced)) {
      var realTimeTripTimes = TripTimesFactory.tripTimes(
        t,
        TEST_MODEL.stopTimesEvery5Minutes(4, t, "00:00"),
        new Deduplicator()
      );
      realTimeTripTimes.setServiceCode(SERVICE_CODE);
      timetableSnapshot.update(
        new RealTimeTripUpdate(
          TripPattern
            .of(new FeedScopedId(FEED_ID, "ADDED_TRIP_PATTERN"))
            .withRoute(t.getRoute())
            .withStopPattern(
              TimetableRepositoryForTest.stopPattern(
                (RegularStop) A.stop,
                (RegularStop) B.stop,
                (RegularStop) C.stop,
                (RegularStop) D.stop
              )
            )
            .withCreatedByRealtimeUpdater(true)
            .build(),
          realTimeTripTimes,
          SERVICE_DATE,
          null,
          t == addedTrip,
          false
        )
      );
    }

    var snapshot = timetableSnapshot.commit();
    timetableRepository.initTimetableSnapshotProvider(() -> snapshot);

    var step1 = walkStep("street")
      .withRelativeDirection(RelativeDirection.DEPART)
      .withAbsoluteDirection(20)
      .build();
    var step2 = walkStep("elevator").withRelativeDirection(RelativeDirection.ELEVATOR).build();
    FeedScopedId entranceId = new FeedScopedId("osm", "123");
    Entrance entrance = Entrance
      .of(entranceId)
      .withCoordinate(new WgsCoordinate(60, 80))
      .withCode("A")
      .withWheelchairAccessibility(Accessibility.POSSIBLE)
      .build();
    var step3 = walkStep("entrance")
      .withRelativeDirection(RelativeDirection.ENTER_OR_EXIT_STATION)
      .withEntrance(entrance)
      .build();

    Itinerary i1 = newItinerary(A, T11_00)
      .walk(20, B, List.of(step1, step2, step3))
      .bus(busRoute, 122, T11_01, T11_15, C)
      .rail(439, T11_30, T11_50, D)
      .carHail(D10m, E)
      .build();

    add10MinuteDelay(i1);

    var busLeg = i1.getTransitLeg(1);
    var railLeg = (ScheduledTransitLeg) i1.getTransitLeg(2);

    var fares = new ItineraryFares();

    var dayPass = fareProduct("day-pass");
    fares.addItineraryProducts(List.of(dayPass));

    var singleTicket = fareProduct("single-ticket");
    fares.addFareProduct(railLeg, singleTicket);
    fares.addFareProduct(busLeg, singleTicket);
    i1.setFare(fares);

    i1.setFare(fares);
    FaresToItineraryMapper.addFaresToLegs(fares, i1);

    i1.setAccessibilityScore(0.5f);

    railLeg.withAccessibilityScore(.3f);

    var entitySelector = new EntitySelector.Stop(A.stop.getId());
    var alert = TransitAlert
      .of(id("an-alert"))
      .withHeaderText(I18NString.of("A header"))
      .withDescriptionText(I18NString.of("A description"))
      .withUrl(I18NString.of("https://example.com"))
      .withCause(AlertCause.MAINTENANCE)
      .withEffect(AlertEffect.REDUCED_SERVICE)
      .withSeverity(AlertSeverity.VERY_SEVERE)
      .addEntity(entitySelector)
      .addTimePeriod(
        new TimePeriod(ALERT_START_TIME.getEpochSecond(), ALERT_END_TIME.getEpochSecond())
      )
      .build();

    railLeg.addAlert(alert);

    var emissions = new Emissions(new Grams(123.0));
    i1.setEmissionsPerPerson(emissions);

    var alerts = ListUtils.combine(List.of(alert), getTransitAlert(entitySelector));
    transitService.getTransitAlertService().setAlerts(alerts);

    var realtimeVehicleService = new DefaultRealtimeVehicleService(transitService);
    var occypancyVehicle = RealtimeVehicle
      .builder()
      .withTrip(trip)
      .withTime(Instant.MAX)
      .withVehicleId(id("vehicle-1"))
      .withOccupancyStatus(FEW_SEATS_AVAILABLE)
      .build();
    var positionVehicle = RealtimeVehicle
      .builder()
      .withTrip(trip)
      .withTime(Instant.MIN)
      .withVehicleId(id("vehicle-2"))
      .withLabel("vehicle2")
      .withCoordinates(new WgsCoordinate(60.0, 80.0))
      .withHeading(80.0)
      .withSpeed(10.2)
      .withStop(pattern.getStop(0))
      .withStopStatus(IN_TRANSIT_TO)
      .build();
    realtimeVehicleService.setRealtimeVehicles(pattern, List.of(occypancyVehicle, positionVehicle));

    DefaultVehicleRentalService defaultVehicleRentalService = new DefaultVehicleRentalService();
    defaultVehicleRentalService.addVehicleRentalStation(VEHICLE_RENTAL_STATION);
    defaultVehicleRentalService.addVehicleRentalStation(RENTAL_VEHICLE_1);
    defaultVehicleRentalService.addVehicleRentalStation(RENTAL_VEHICLE_2);

    context =
      new GraphQLRequestContext(
        new TestRoutingService(List.of(i1)),
        transitService,
        new DefaultFareService(),
        defaultVehicleRentalService,
        new DefaultVehicleParkingService(parkingRepository),
        realtimeVehicleService,
        finder,
        new RouteRequest()
      );
  }

  private static BikeAccess bikesAllowed(TransitMode m) {
    return switch (m.ordinal() % 3) {
      case 0 -> BikeAccess.ALLOWED;
      case 1 -> BikeAccess.NOT_ALLOWED;
      default -> BikeAccess.UNKNOWN;
    };
  }

  /**
   * We want to provide a variety of numbers and null, so we cover all cases in the test output.
   */
  private static Integer sortOrder(TransitMode m) {
    if (m.ordinal() == 0) {
      return null;
    } else {
      return m.ordinal();
    }
  }

  private static void add10MinuteDelay(Itinerary i1) {
    i1.transformTransitLegs(tl -> {
      if (tl instanceof ScheduledTransitLeg stl) {
        var rtt = (RealTimeTripTimes) stl.getTripTimes();

        for (var i = 0; i < rtt.getNumStops(); i++) {
          rtt.updateArrivalTime(i, rtt.getArrivalTime(i) + TEN_MINUTES);
          rtt.updateDepartureTime(i, rtt.getDepartureTime(i) + TEN_MINUTES);
        }
      }
      return tl;
    });
  }

  @FilePatternSource(
    pattern = "application/src/test/resources/org/opentripplanner/apis/gtfs/queries/*.graphql"
  )
  @ParameterizedTest(name = "Check GraphQL query in {0}")
  void graphQL(Path path) throws IOException {
    var query = Files.readString(path);
    var response = GtfsGraphQLIndex.getGraphQLResponse(
      query,
      null,
      null,
      2000,
      2000,
      Locale.ENGLISH,
      context
    );
    var actualJson = responseBody(response);
    assertEquals(200, response.getStatus());

    Path expectationFile = getExpectation(path);

    if (!expectationFile.toFile().exists()) {
      Files.writeString(
        expectationFile,
        JsonSupport.prettyPrint(actualJson),
        StandardOpenOption.CREATE_NEW
      );
      fail(
        "No expectations file for %s so generated it. Please check the content.".formatted(path)
      );
    }

    var expectedJson = Files.readString(expectationFile);
    assertEqualJson(expectedJson, actualJson);
  }

  private static List<TransitAlert> getTransitAlert(EntitySelector.Stop entitySelector) {
    var alertWithoutDescription = TransitAlert
      .of(id("no-description"))
      .withHeaderText(I18NStrings.TRANSLATED_STRING_1)
      .addEntity(entitySelector);

    var alertWithoutHeader = TransitAlert
      .of(id("no-header"))
      .withDescriptionText(I18NStrings.TRANSLATED_STRING_2)
      .addEntity(entitySelector);
    var alertWithNothing = TransitAlert
      .of(id("neither-header-nor-description"))
      .addEntity(entitySelector);

    return Stream
      .of(alertWithoutDescription, alertWithoutHeader, alertWithNothing)
      .map(AbstractBuilder::build)
      .toList();
  }

  private static WalkStepBuilder walkStep(String name) {
    return WalkStep
      .builder()
      .withDirectionText(I18NString.of(name))
      .withStartLocation(WgsCoordinate.GREENWICH)
      .withAngle(10);
  }

  private static FareProduct fareProduct(String name) {
    return new FareProduct(
      id(name),
      name,
      Money.euros(10),
      null,
      new RiderCategory(id("senior-citizens"), "Senior citizens", null),
      new FareMedium(id("oyster"), "TfL Oyster Card")
    );
  }

  /**
   * Locate 'expectations' relative to the given query input file. The 'expectations' and 'queries'
   * subdirectories are expected to be in the same directory.
   */
  private static Path getExpectation(Path path) {
    return path
      .getParent()
      .getParent()
      .resolve("expectations")
      .resolve(path.getFileName().toString().replace(".graphql", ".json"));
  }

  private static String responseBody(Response response) {
    if (response instanceof OutboundJaxrsResponse outbound) {
      return (String) outbound.getContext().getEntity();
    }
    fail("expected an outbound response but got %s".formatted(response.getClass().getSimpleName()));
    return null;
  }

  private static final GraphFinder finder = new GraphFinder() {
    @Override
    public List<NearbyStop> findClosestStops(Coordinate coordinate, double radiusMeters) {
      return null;
    }

    @Override
    public List<PlaceAtDistance> findClosestPlaces(
      double lat,
      double lon,
      double radiusMeters,
      int maxResults,
      List<TransitMode> filterByModes,
      List<PlaceType> filterByPlaceTypes,
      List<FeedScopedId> filterByStops,
      List<FeedScopedId> filterByStations,
      List<FeedScopedId> filterByRoutes,
      List<String> filterByBikeRentalStations,
      List<String> filterByNetwork,
      TransitService transitService
    ) {
      var stop = TimetableRepositoryForTest.of().stop("A").build();
      return List.of(
        new PlaceAtDistance(stop, 0),
        new PlaceAtDistance(VEHICLE_RENTAL_STATION, 30),
        new PlaceAtDistance(RENTAL_VEHICLE_1, 50)
      );
    }
  };
}
