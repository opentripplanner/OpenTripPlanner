package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.NonLocalizedString;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
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
    stopPattern = new StopPattern(List.of(stopTime));

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
  }

  @Test
  public void testCreateModifiedStops() {
    // Arrange
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    Function<FeedScopedId, StopLocation> stopFetcher = s -> this.stopLocation;

    // Act
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stops created");
    for (StopLocation location : result) {
      assertAll(() -> {
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
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stops created");
    for (StopLocation location : result) {
      assertAll(() -> {
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
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopLocation location : result) {
      assertAll(() -> {
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
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopLocation location : result) {
      assertAll(() -> {
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
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopLocation location : result) {
      assertAll(() -> {
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
    var result = TimetableHelper.createModifiedStops(
      tripPattern,
      estimatedVehicleJourney,
      stopFetcher
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(0, result.size(), "There should be more than 0 stopLocations created");
    for (StopLocation location : result) {
      assertAll(() -> {
        assertEquals(SCOPED_STOP_ID, location.getId(), "StopLocation id should be set from input");
        assertEquals(stop, location);
      });
    }
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithRecordedCalls(
    FeedScopedId newStopScopedId
  ) {
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    var stopPointRef = new StopPointRef();
    stopPointRef.setValue(newStopScopedId.toString());

    var recordedCall = new RecordedCall();
    recordedCall.setStopPointRef(stopPointRef);

    var recordedCalls = new EstimatedVehicleJourney.RecordedCalls();
    recordedCalls.getRecordedCalls().add(recordedCall);
    estimatedVehicleJourney.setRecordedCalls(recordedCalls);

    return estimatedVehicleJourney;
  }

  private EstimatedVehicleJourney getEstimatedVehicleJourneyWithEstimatedCalls(
    FeedScopedId newStopScopedId
  ) {
    var estimatedVehicleJourney = new EstimatedVehicleJourney();
    var stopPointRef = new StopPointRef();
    stopPointRef.setValue(newStopScopedId.toString());

    var estimatedCall = new EstimatedCall();
    estimatedCall.setStopPointRef(stopPointRef);

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
