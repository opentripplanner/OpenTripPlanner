package org.opentripplanner.transit.model.timetable;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

public class FlexibleTripTimesBuilder
  extends AbstractTripTimesBuilder<FlexibleTripTimes, FlexibleTripTimesBuilder> {

  FlexibleTripTimesBuilder(@Nullable DeduplicatorService deduplicator) {
    this(0, NOT_SET, null, null, null, null, null, null, null, null, null, deduplicator);
  }

  FlexibleTripTimesBuilder(
    int timeShift,
    int serviceCode,
    int[] arrivalTimes,
    int[] departureTimes,
    BitSet timepoints,
    Trip trip,
    List<BookingInfo> dropOffBookingInfos,
    List<BookingInfo> pickupBookingInfos,
    I18NString[] headsigns,
    String[][] headsignVias,
    int[] gtfsSequenceOfStopIndex,
    DeduplicatorService deduplicator
  ) {
    super(
      timeShift,
      serviceCode,
      arrivalTimes,
      departureTimes,
      timepoints,
      trip,
      dropOffBookingInfos,
      pickupBookingInfos,
      headsigns,
      headsignVias,
      gtfsSequenceOfStopIndex,
      deduplicator
    );
  }

  @Override
  public FlexibleTripTimes build() {
    normalizeTimes();
    return new FlexibleTripTimes(this);
  }
}
