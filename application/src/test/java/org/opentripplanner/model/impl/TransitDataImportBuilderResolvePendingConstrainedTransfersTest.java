package org.opentripplanner.model.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transfer.constrained.model.DeferredConstrainedTransferPoint;
import org.opentripplanner.transfer.constrained.model.PendingConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.TransferConstraint;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.SiteRepository;

/**
 * Covers the stop-position lookup {@link TransitDataImportBuilder#build()} performs for a
 * {@link DeferredConstrainedTransferPoint.PendingTripPosition} - the same lookup {@code TransferMapperTest}
 * used to exercise before it moved here (see PR #7973 review, finding 2: resolving it any earlier
 * than {@code build()} risks the position no longer matching the trip's final stop-time
 * sequence).
 */
class TransitDataImportBuilderResolvePendingConstrainedTransfersTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final FeedScopedId SERVICE_ID = FeedScopedIdForTestFactory.id("weekdays");
  private static final RegularStop STOP_1 = TEST_MODEL.stop("Stop-1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("Stop-2").build();

  private static final LocalDateRange PERIOD = LocalDateRange.ofInclusiveEnd(
    LocalDate.of(2024, 1, 1),
    LocalDate.of(2024, 1, 31)
  );

  @Test
  void boardResolvesToFirstOccurrenceAndAlightToLastOccurrenceOfARepeatedStop() {
    var builder = new TransitDataImportBuilder(
      new SiteRepository(),
      DataImportIssueStore.NOOP,
      PERIOD
    );
    builder.tripCalendars().addWeeklyCalendar(SERVICE_ID, EnumSet.allOf(DayOfWeek.class), PERIOD);

    var route = TransitRepositoryForTest.route("R").build();
    Trip trip = TransitRepositoryForTest.trip("T")
      .withServiceId(SERVICE_ID)
      .withDirection(Direction.INBOUND)
      .withRoute(route)
      .build();
    builder.getTripsById().add(trip);

    // Stop-1 appears three times on this trip; only the first (board) and last (alight)
    // occurrence are valid transfer positions.
    builder
      .getStopTimesSortedByTrip()
      .addAll(
        List.of(
          stopTime(trip, STOP_1, 0, 0),
          stopTime(trip, STOP_2, 1, 100),
          stopTime(trip, STOP_1, 2, 200),
          stopTime(trip, STOP_2, 3, 300),
          stopTime(trip, STOP_1, 4, 400)
        )
      );

    var constraint = TransferConstraint.of().guaranteed(true).build();
    builder
      .getPendingTransfers()
      .add(
        new PendingConstrainedTransfer(
          null,
          new DeferredConstrainedTransferPoint.PendingTripPosition(trip, STOP_1, null, true),
          new DeferredConstrainedTransferPoint.PendingTripPosition(trip, STOP_1, null, false),
          constraint
        )
      );

    var dataImport = builder.build();
    var transfer = dataImport.getAllTransfers().iterator().next();

    assertEquals(0, ((TripTransferPoint) transfer.getFrom()).getStopPositionInPattern());
    assertEquals(4, ((TripTransferPoint) transfer.getTo()).getStopPositionInPattern());
  }

  private static StopTime stopTime(Trip trip, RegularStop stop, int stopSequence, int time) {
    StopTime st = new StopTime();
    st.setTrip(trip);
    st.setStop(stop);
    st.setStopSequence(stopSequence);
    st.setArrivalTime(time);
    st.setDepartureTime(time);
    st.setPickupType(PickDrop.SCHEDULED);
    st.setDropOffType(PickDrop.SCHEDULED);
    return st;
  }
}
