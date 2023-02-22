package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;

class ModifiedTripBuilderTest {

  /* Transit model */
  private static final Station STATION_A = TransitModelForTest.station("A").build();
  private static final Station STATION_B = TransitModelForTest.station("B").build();
  private static final Station STATION_C = TransitModelForTest.station("C").build();

  private static final RegularStop STOP_A_1 = TransitModelForTest
    .stop("A_1")
    .withParentStation(STATION_A)
    .build();
  private static final RegularStop STOP_A_2 = TransitModelForTest
    .stop("A_2")
    .withParentStation(STATION_A)
    .build();
  private static final RegularStop STOP_B_1 = TransitModelForTest
    .stop("B_1")
    .withParentStation(STATION_B)
    .build();
  private static final RegularStop STOP_C_1 = TransitModelForTest
    .stop("C_1")
    .withParentStation(STATION_C)
    .build();
  private static final RegularStop STOP_D = TransitModelForTest.stop("D").build();

  private static final Route ROUTE = TransitModelForTest.route("ROUTE_ID").build();

  private static final TripPattern PATTERN = TransitModelForTest
    .tripPattern("PATTERN_ID", ROUTE)
    .withStopPattern(TransitModelForTest.stopPattern(STOP_A_1, STOP_B_1, STOP_C_1))
    .build();

  private static final FeedScopedId SERVICE_ID = TransitModelForTest.id("CAL_1");

  private static final Trip TRIP = TransitModelForTest
    .trip("TRIP")
    .withRoute(ROUTE)
    .withServiceId(SERVICE_ID)
    .build();

  private static final LocalDate SERVICE_DATE = LocalDate.of(2023, 2, 17);
  private final StopModel stopModel = StopModel
    .of()
    .withRegularStop(STOP_A_1)
    .withRegularStop(STOP_A_2)
    .withRegularStop(STOP_B_1)
    .withRegularStop(STOP_C_1)
    .withRegularStop(STOP_D)
    .build();

  private final Deduplicator deduplicator = new Deduplicator();
  private final TransitModel transitModel = new TransitModel(stopModel, deduplicator);
  private EntityResolver entityResolver;

  @BeforeEach
  void setUp() {
    // Add entities to transit model for the entity resolver
    transitModel.addTripPattern(PATTERN.getId(), PATTERN);

    // Crate a scheduled calendar, to have the SERVICE_DATE be within the transit feed coverage
    CalendarServiceData calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(
      SERVICE_ID,
      List.of(SERVICE_DATE.minusDays(1), SERVICE_DATE, SERVICE_DATE.plusDays(1))
    );
    transitModel.getServiceCodes().put(SERVICE_ID, 0);
    transitModel.updateCalendarServiceData(true, calendarServiceData, DataImportIssueStore.NOOP);

    // Create transit model index
    transitModel.index();

    // Create the entity resolver only after the model has been indexed
    entityResolver =
      new EntityResolver(new DefaultTransitService(transitModel), TransitModelForTest.FEED_ID);
  }

  @Test
  void testCreateStopPatternNoCalls() {
    // No calls should result in original pattern
    var result = ModifiedTripBuilder.createStopPattern(PATTERN, List.of(), entityResolver);

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertEquals(PATTERN.getStopPattern(), result);
  }

  @Test
  void testCreateStopPatternSingleCall() {
    // No change in stops should result in original pattern
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertEquals(PATTERN.getStopPattern(), result);
  }

  @Test
  void testCreateStopPatternSameStopCalls() {
    // No change in stops should result in original pattern
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertEquals(PATTERN.getStopPattern(), result);
  }

  @Test
  void testCreateStopPatternSameStationCalls() {
    // Change in stations should result in new pattern
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_2.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(PATTERN.getStopPattern(), result);

    var newPattern = PATTERN.copy().withStopPattern(result).build();
    assertEquals(STOP_A_2, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    for (int i = 0; i < newPattern.numberOfStops(); i++) {
      assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(i));
      assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(i));
    }
  }

  @Test
  void testCreateStopPatternDifferentStationCall() {
    // Stop on non-pattern stop, without parent station should be ignored
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_D.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertEquals(PATTERN.getStopPattern(), result);
  }

  @Test
  void testCreateStopPatternCancelledCall() {
    // Cancellation of a call should be reflected in PickDrop
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_B_1.getId().getId()).build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).withCancellation(true).build()
      ),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(PATTERN.getStopPattern(), result);

    var newPattern = PATTERN.copy().withStopPattern(result).build();
    assertEquals(STOP_A_1, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(0));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(0));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(1));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(1));

    assertEquals(PickDrop.CANCELLED, newPattern.getAlightType(2));
    assertEquals(PickDrop.CANCELLED, newPattern.getBoardType(2));
  }

  @Test
  void testCreateStopPatternNoBoardingCall() {
    // Cancellation of a call should be reflected in PickDrop
    var result = ModifiedTripBuilder.createStopPattern(
      PATTERN,
      List.of(
        TestCall.of().withStopPointRef(STOP_A_1.getId().getId()).build(),
        TestCall
          .of()
          .withStopPointRef(STOP_B_1.getId().getId())
          .withDepartureBoardingActivity(DepartureBoardingActivityEnumeration.NO_BOARDING)
          .build(),
        TestCall.of().withStopPointRef(STOP_C_1.getId().getId()).build()
      ),
      entityResolver
    );

    // Assert
    assertNotNull(result, "The stops should not be null");
    assertNotEquals(PATTERN.getStopPattern(), result);

    var newPattern = PATTERN.copy().withStopPattern(result).build();
    assertEquals(STOP_A_1, newPattern.getStop(0));
    assertEquals(STOP_B_1, newPattern.getStop(1));
    assertEquals(STOP_C_1, newPattern.getStop(2));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(0));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(0));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(1));
    assertEquals(PickDrop.NONE, newPattern.getBoardType(1));

    assertEquals(PickDrop.SCHEDULED, newPattern.getAlightType(2));
    assertEquals(PickDrop.SCHEDULED, newPattern.getBoardType(2));
  }
}
