package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.OccupancyEnumeration;
import uk.org.siri.siri20.RecordedCall;
import uk.org.siri.siri20.StopPointRef;

public class TimeTableHelperTest {

  private final String FEED_ID = "FEED_ID";

  private final String PATTERN_ID = "PATTERN_ID";
  private final String STOP_ID = "STOP_1";
  private final String STATION_ID = "STOP_PARENT_1";
  private final String STATION_NAME = "STATION_NAME_1";
  private final String LINE_ID = "LINE_1";
  private final String AGENCY_ID = "AGENCY_1";
  private final String AGENCY_NAME = "AGENCY_ONE";
  private final String LINE_SHORT_NAME = "LINE_ONE";

  private final FeedScopedId SCOPED_STATION_ID = new FeedScopedId(FEED_ID, STATION_ID);
  private final FeedScopedId SCOPED_STOP_ID = new FeedScopedId(FEED_ID, STOP_ID);
  private final FeedScopedId SCOPED_AGENCY_ID = new FeedScopedId(FEED_ID, AGENCY_ID);
  private final FeedScopedId SCOPED_LINE_ID = new FeedScopedId(FEED_ID, LINE_ID);
  private final FeedScopedId SCOPED_PATTERN_ID = new FeedScopedId(FEED_ID, PATTERN_ID);

  private TripPattern tripPattern;
  private Route route;
  private Agency agency;
  private StopPattern stopPattern;
  private StopLocation stopLocation;
  private RegularStop stop;
  private Station station;

  private Trip trip;
  private List<StopTime> stopTimes;
  private TripTimes tripTimes;

  @BeforeEach
  public void setUp() {
    stopLocation = RegularStop.of(SCOPED_STOP_ID).build();

    station =
      Station
        .of(SCOPED_STATION_ID)
        .withName(new NonLocalizedString(STATION_NAME))
        .withCoordinate(0.0, 0.0)
        .build();

    var stopTime = new StopTime();
    stop =
      RegularStop.of(SCOPED_STOP_ID).withCoordinate(0.0, 0.0).withParentStation(station).build();
    stopTime.setStop(stop);
    stopTimes = List.of(stopTime);
    stopPattern = new StopPattern(stopTimes);

    agency = Agency.of(SCOPED_AGENCY_ID).withName(AGENCY_NAME).withTimezone("CET").build();

    route =
      Route
        .of(SCOPED_LINE_ID)
        .withShortName(LINE_SHORT_NAME)
        .withAgency(agency)
        .withMode(TransitMode.FUNICULAR)
        .build();

    tripPattern =
      TripPattern.of(SCOPED_PATTERN_ID).withStopPattern(stopPattern).withRoute(route).build();

    trip = Trip.of(new FeedScopedId(FEED_ID, "TRIP_ID")).withRoute(route).build();
    var deduplicator = new Deduplicator();
    tripTimes = new TripTimes(trip, stopTimes, deduplicator);
  }

  @Test
  public void testCreateModifiedStops() {
    // Arrange
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    Function<FeedScopedId, StopLocation> stopFetcher = s -> this.stopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stops created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(SCOPED_STOP_ID, location.getId(), "StopLocation id should be set from input");
        assertEquals(stop, location);
      });
    }
  }

  @Test
  public void testCreateModifiedStopsWithChangedRecordedStop() {
    // Arrange
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    Function<FeedScopedId, StopLocation> stopFetcher = s -> this.stopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stops created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(SCOPED_STOP_ID, location.getId(), "StopLocation id should be set from input");
        assertEquals(stop, location);
      });
    }
  }

  @Test
  public void testChangedRecordCallStopDifferentStation() {
    // Arrange
    var newStopScopedId = new FeedScopedId(FEED_ID, "RECORDED_STOP");
    var newRecordedStopLocation = RegularStop.of(newStopScopedId).build();
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithRecordedCalls(
      newStopScopedId
    );
    Function<FeedScopedId, StopLocation> stopFetcher = s -> newRecordedStopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(SCOPED_STOP_ID, location.getId(), "StopLocation id should be set from input");
        assertEquals(stop, location);
      });
    }
  }

  @Test
  public void testChangedRecordCallStopSameStation() {
    // Arrange
    var newStopScopedId = new FeedScopedId(FEED_ID, "RECORDED_STOP");
    var newRecordedStopLocation = RegularStop
      .of(newStopScopedId)
      .withParentStation(station)
      .build();
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithRecordedCalls(
      newStopScopedId
    );
    Function<FeedScopedId, StopLocation> stopFetcher = s -> newRecordedStopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(
          newStopScopedId,
          location.getId(),
          "StopLocation id should be set from the recorded call"
        );
        assertEquals(newRecordedStopLocation, location);
      });
    }
  }

  @Test
  public void testChangedEstimatedCallStopSameStation() {
    // Arrange
    var newStopScopedId = new FeedScopedId(FEED_ID, "RECORDED_STOP");
    var newRecordedStopLocation = RegularStop
      .of(newStopScopedId)
      .withParentStation(station)
      .build();
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithEstimatedCalls(
      newStopScopedId
    );
    Function<FeedScopedId, StopLocation> stopFetcher = s -> newRecordedStopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(
          newStopScopedId,
          location.getId(),
          "StopLocation id should be set from the recorded call"
        );
        assertEquals(newRecordedStopLocation, location);
      });
    }
  }

  @Test
  public void testChangedEstimatedCallStopDifferentStation() {
    // Arrange
    var newStopScopedId = new FeedScopedId(FEED_ID, "RECORDED_STOP");
    var newRecordedStopLocation = RegularStop.of(newStopScopedId).build();
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithEstimatedCalls(
      newStopScopedId
    );
    Function<FeedScopedId, StopLocation> stopFetcher = s -> newRecordedStopLocation;

    // Act
    var result = TimetableHelper.createModifiedStopTimes(
      tripPattern,
      tripTimes,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopTime stopTime : result) {
      assertAll(() -> {
        StopLocation location = stopTime.getStop();
        assertEquals(SCOPED_STOP_ID, location.getId(), "StopLocation id should be set from input");
        assertEquals(stop, location);
      });
    }
  }

  @Test
  public void testApplyUpdates_MapPredictionInaccurate_EstimatedCall() {
    // Arrange
    var startOfService = ZonedDateTime.of(LocalDateTime.of(2022, 12, 9, 0, 0), ZoneId.of("CET"));
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithEstimatedCalls(
      SCOPED_STOP_ID,
      false,
      OccupancyEnumeration.SEATS_AVAILABLE,
      true
    );
    int callCounter = 0;

    for (var estimatedCall : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
      // Act
      TimetableHelper.applyUpdates(
        startOfService,
        stopTimes,
        tripTimes,
        callCounter,
        false,
        estimatedCall,
        null
      );

      // Assert

      assertStatuses(
        callCounter,
        OccupancyStatus.MANY_SEATS_AVAILABLE,
        PickDrop.SCHEDULED,
        false,
        true
      );
      callCounter++;
    }
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_EstimatedCall() {
    // Arrange
    var startOfService = ZonedDateTime.of(LocalDateTime.of(2022, 12, 9, 0, 0), ZoneId.of("CET"));
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithEstimatedCalls(
      SCOPED_STOP_ID,
      true,
      OccupancyEnumeration.FULL,
      true
    );
    int callCounter = 0;

    for (var estimatedCall : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
      // Act
      TimetableHelper.applyUpdates(
        startOfService,
        stopTimes,
        tripTimes,
        callCounter,
        false,
        estimatedCall,
        null
      );

      // Assert

      assertStatuses(callCounter, OccupancyStatus.FULL, PickDrop.CANCELLED, false, false);
      callCounter++;
    }
  }

  @Test
  public void testApplyUpdates_CancellationPriorityOverPredictionInaccurate_RecordedCall() {
    // Arrange
    var startOfService = ZonedDateTime.of(LocalDateTime.of(2022, 12, 9, 0, 0), ZoneId.of("CET"));
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithRecordedCalls(
      SCOPED_STOP_ID,
      true,
      OccupancyEnumeration.FULL,
      true,
      startOfService.plus(Duration.ofHours(1))
    );
    int callCounter = 0;

    for (var recordedCall : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
      // Act
      TimetableHelper.applyUpdates(
        startOfService,
        stopTimes,
        tripTimes,
        callCounter,
        false,
        recordedCall,
        null
      );

      // Assert

      assertStatuses(callCounter, OccupancyStatus.FULL, PickDrop.CANCELLED, false, false);
      callCounter++;
    }
  }

  @Test
  public void testApplyUpdates_PredictionInaccuratePriorityOverRecorded() {
    // Arrange
    var predictionInaccurate = true;
    var startOfService = ZonedDateTime.of(LocalDateTime.of(2022, 12, 9, 0, 0), ZoneId.of("CET"));
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithRecordedCalls(
      SCOPED_STOP_ID,
      predictionInaccurate,
      OccupancyEnumeration.FULL,
      false,
      startOfService.plus(Duration.ofHours(1))
    );
    int callCounter = 0;

    for (var recordedCall : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
      // Act
      TimetableHelper.applyUpdates(
        startOfService,
        stopTimes,
        tripTimes,
        callCounter,
        false,
        recordedCall,
        null
      );

      // Assert
      assertStatuses(
        callCounter,
        OccupancyStatus.FULL,
        PickDrop.SCHEDULED,
        false,
        predictionInaccurate
      );

      callCounter++;
    }
  }

  @Test
  public void testApplyUpdates_ActualTimeResultsInRecorded() {
    // Arrange
    var predictionInaccurate = false;
    var startOfService = ZonedDateTime.of(LocalDateTime.of(2022, 12, 9, 0, 0), ZoneId.of("CET"));
    EstimatedVehicleJourney estimatedVehicleJourney = getEstimatedVehicleJourneyWithRecordedCalls(
      SCOPED_STOP_ID,
      predictionInaccurate,
      OccupancyEnumeration.STANDING_AVAILABLE,
      false,
      startOfService.plus(Duration.ofHours(1))
    );
    int callCounter = 0;

    for (var recordedCall : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
      // Act
      TimetableHelper.applyUpdates(
        startOfService,
        stopTimes,
        tripTimes,
        callCounter,
        false,
        recordedCall,
        null
      );

      // Assert
      assertStatuses(
        callCounter,
        OccupancyStatus.STANDING_ROOM_ONLY,
        PickDrop.SCHEDULED,
        true,
        predictionInaccurate
      );

      callCounter++;
    }
  }

  private void assertStatuses(
    int index,
    OccupancyStatus occupancyStatus,
    PickDrop pickDrop,
    boolean recorded,
    boolean predictionInaccurate
  ) {
    assertAll(() -> {
      assertEquals(
        predictionInaccurate,
        tripTimes.isPredictionInaccurate(index),
        "Prediction inaccurate mapped incorrectly"
      );
      assertEquals(recorded, tripTimes.isRecordedStop(index), "Recorded status mapped incorrectly");
      assertEquals(
        occupancyStatus,
        tripTimes.getOccupancyStatus(index),
        "Occupancy should be mapped to " + occupancyStatus
      );
      assertEquals(pickDrop, stopTimes.get(index).getDropOffType(), "Pickdrop should be scheduled");
    });
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithRecordedCalls(
    FeedScopedId newStopScopedId
  ) {
    return getEstimatedVehicleJourneyWithRecordedCalls(newStopScopedId, false, null, false, null);
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithRecordedCalls(
    FeedScopedId newStopScopedId,
    boolean predictionInaccurate,
    OccupancyEnumeration occupancyEnumeration,
    boolean cancellation,
    ZonedDateTime actualTime
  ) {
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    var stopPointRef = new StopPointRef();
    stopPointRef.setValue(newStopScopedId.toString());

    var recordedCall = new RecordedCall();
    recordedCall.setStopPointRef(stopPointRef);
    recordedCall.setPredictionInaccurate(predictionInaccurate);
    recordedCall.setOccupancy(occupancyEnumeration);
    recordedCall.setCancellation(cancellation);
    recordedCall.setActualDepartureTime(actualTime);

    var recordedCalls = new EstimatedVehicleJourney.RecordedCalls();
    recordedCalls.getRecordedCalls().add(recordedCall);
    estimatedVehicleJourney.setRecordedCalls(recordedCalls);

    return estimatedVehicleJourney;
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithEstimatedCalls(
    FeedScopedId newStopScopedId
  ) {
    return getEstimatedVehicleJourneyWithEstimatedCalls(newStopScopedId, false, null, false);
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithEstimatedCalls(
    FeedScopedId newStopScopedId,
    boolean cancellation,
    OccupancyEnumeration occupancyEnumeration,
    boolean predictionInaccurate
  ) {
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    var stopPointRef = new StopPointRef();
    stopPointRef.setValue(newStopScopedId.toString());

    var estimatedCall = new EstimatedCall();
    estimatedCall.setStopPointRef(stopPointRef);
    estimatedCall.setCancellation(cancellation);
    estimatedCall.setOccupancy(occupancyEnumeration);
    estimatedCall.setPredictionInaccurate(predictionInaccurate);

    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall);
    estimatedVehicleJourney.setEstimatedCalls(estimatedCalls);

    return estimatedVehicleJourney;
  }

  @Test
  public void testCreateModifiedStopTimes() {
    //    TimetableHelper.createModifiedStopTimes()
  }

  @Test
  public void testNoRoutabilityChangeDropOff() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.ALIGHTING;
    var testResult = TimetableHelper.mapDropOffType(
      originalDropOffType,
      incomingArrivalBoardingActivity
    );

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - the dropOffType should not change"
    );
  }

  @Test
  public void testNoRoutabilityChangePickUp() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.BOARDING;
    var testResult = TimetableHelper.mapPickUpType(
      originalPickUpType,
      incomingDepartureBoardingActivity
    );

    assertTrue(
      testResult.isEmpty(),
      "There is no change in routability - the pickUpType should not change"
    );
  }

  @Test
  public void testChangeInRoutabilityChangePickUp() {
    var originalPickUpType = PickDrop.NONE;
    var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.BOARDING;
    var testResult = TimetableHelper.mapPickUpType(
      originalPickUpType,
      incomingDepartureBoardingActivity
    );

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the pickUpType should change"
    );
    assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangeDropOff() {
    var originalDropOffType = PickDrop.NONE;
    var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.ALIGHTING;
    var testResult = TimetableHelper.mapDropOffType(
      originalDropOffType,
      incomingArrivalBoardingActivity
    );

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.SCHEDULED, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangeDropOff_NoAlighting() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    var incomingArrivalBoardingActivity = ArrivalBoardingActivityEnumeration.NO_ALIGHTING;
    var testResult = TimetableHelper.mapDropOffType(
      originalDropOffType,
      incomingArrivalBoardingActivity
    );

    assertTrue(
      testResult.isPresent(),
      "There change in routability - the dropOffType should change"
    );
    assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
  }

  @Test
  public void testChangeInRoutabilityChangePickUp_NoBoarding() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    var incomingDepartureBoardingActivity = DepartureBoardingActivityEnumeration.NO_BOARDING;
    var testResult = TimetableHelper.mapPickUpType(
      originalPickUpType,
      incomingDepartureBoardingActivity
    );

    assertTrue(
      testResult.isPresent(),
      "There is change in routability - the pickUpType should change"
    );
    assertEquals(testResult.get(), PickDrop.NONE, "The DropOffType should be scheduled");
  }

  @Test
  public void testNullBoardingActivity() {
    var originalPickUpType = PickDrop.COORDINATE_WITH_DRIVER;
    DepartureBoardingActivityEnumeration incomingDepartureBoardingActivity = null;
    var testResult = TimetableHelper.mapPickUpType(
      originalPickUpType,
      incomingDepartureBoardingActivity
    );

    assertTrue(testResult.isEmpty(), "There should be an empty optional returned");
  }

  @Test
  public void testNullArrivalActivity() {
    var originalDropOffType = PickDrop.COORDINATE_WITH_DRIVER;
    ArrivalBoardingActivityEnumeration incomingDepartureArrivalActivity = null;
    var testResult = TimetableHelper.mapDropOffType(
      originalDropOffType,
      incomingDepartureArrivalActivity
    );

    assertTrue(testResult.isEmpty(), "There should be an empty optional returned");
  }
}
