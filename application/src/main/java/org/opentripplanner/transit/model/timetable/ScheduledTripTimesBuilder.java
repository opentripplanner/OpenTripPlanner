package org.opentripplanner.transit.model.timetable;

import java.util.BitSet;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.time.TimeUtils;

public class ScheduledTripTimesBuilder {

  private static final int NOT_SET = -1;
  private static final BitSet EMPTY_BIT_SET = new BitSet(0);

  private int timeShift;
  private int serviceCode;
  private int[] arrivalTimes;
  private int[] departureTimes;
  private BitSet timepoints;
  private Trip trip;
  private List<BookingInfo> dropOffBookingInfos;
  private List<BookingInfo> pickupBookingInfos;
  private I18NString[] headsigns;
  private String[][] headsignVias;
  private int[] gtfsSequenceOfStopIndex;
  private final DeduplicatorService deduplicator;

  ScheduledTripTimesBuilder(@Nullable DeduplicatorService deduplicator) {
    this(0, NOT_SET, null, null, null, null, null, null, null, null, null, deduplicator);
  }

  ScheduledTripTimesBuilder(
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
    this.timeShift = timeShift;
    this.serviceCode = serviceCode;
    this.arrivalTimes = arrivalTimes;
    this.departureTimes = departureTimes;
    this.timepoints = timepoints;
    this.trip = trip;
    this.dropOffBookingInfos = dropOffBookingInfos;
    this.pickupBookingInfos = pickupBookingInfos;
    this.headsigns = headsigns;
    this.headsignVias = headsignVias;
    this.gtfsSequenceOfStopIndex = gtfsSequenceOfStopIndex;
    this.deduplicator = deduplicator == null ? DeduplicatorService.NOOP : deduplicator;
  }

  public int timeShift() {
    return timeShift;
  }

  public ScheduledTripTimesBuilder withTimeShift(int timeShift) {
    this.timeShift = timeShift;
    return this;
  }

  /**
   * Add the {@code delta} to the existing timeShift. This is useful when moving a trip
   * from one time-zone to another.
   */
  public ScheduledTripTimesBuilder plusTimeShift(int delta) {
    this.timeShift += delta;
    return this;
  }

  public int serviceCode() {
    return serviceCode;
  }

  public ScheduledTripTimesBuilder withServiceCode(int serviceCode) {
    this.serviceCode = serviceCode;
    return this;
  }

  public int[] arrivalTimes() {
    return arrivalTimes;
  }

  public ScheduledTripTimesBuilder withArrivalTimes(int[] arrivalTimes) {
    this.arrivalTimes = deduplicator.deduplicateIntArray(arrivalTimes);
    return this;
  }

  /** For unit testing, uses {@link TimeUtils#time(java.lang.String)}. */
  public ScheduledTripTimesBuilder withArrivalTimes(String arrivalTimes) {
    return withArrivalTimes(TimeUtils.times(arrivalTimes));
  }

  public int[] departureTimes() {
    return departureTimes;
  }

  public ScheduledTripTimesBuilder withDepartureTimes(int[] departureTimes) {
    this.departureTimes = deduplicator.deduplicateIntArray(departureTimes);
    return this;
  }

  /** For unit testing, uses {@link TimeUtils#time(java.lang.String)}. */
  public ScheduledTripTimesBuilder withDepartureTimes(String departureTimes) {
    return withDepartureTimes(TimeUtils.times(departureTimes));
  }

  public BitSet timepoints() {
    return timepoints == null ? EMPTY_BIT_SET : timepoints;
  }

  public ScheduledTripTimesBuilder withTimepoints(BitSet timepoints) {
    this.timepoints = deduplicator.deduplicateBitSet(timepoints);
    return this;
  }

  public Trip trip() {
    return trip;
  }

  public ScheduledTripTimesBuilder withTrip(Trip trip) {
    this.trip = trip;
    return this;
  }

  public List<BookingInfo> dropOffBookingInfos() {
    return dropOffBookingInfos == null ? List.of() : dropOffBookingInfos;
  }

  public ScheduledTripTimesBuilder withDropOffBookingInfos(List<BookingInfo> dropOffBookingInfos) {
    this.dropOffBookingInfos = deduplicator.deduplicateImmutableList(
      BookingInfo.class,
      dropOffBookingInfos
    );
    return this;
  }

  public List<BookingInfo> pickupBookingInfos() {
    return pickupBookingInfos == null ? List.of() : pickupBookingInfos;
  }

  public ScheduledTripTimesBuilder withPickupBookingInfos(List<BookingInfo> pickupBookingInfos) {
    this.pickupBookingInfos = deduplicator.deduplicateImmutableList(
      BookingInfo.class,
      pickupBookingInfos
    );
    return this;
  }

  public I18NString[] headsigns() {
    return headsigns;
  }

  public ScheduledTripTimesBuilder withHeadsigns(I18NString[] headsigns) {
    this.headsigns = deduplicator.deduplicateObjectArray(I18NString.class, headsigns);
    return this;
  }

  public String[][] headsignVias() {
    return headsignVias;
  }

  public ScheduledTripTimesBuilder withHeadsignVias(String[][] headsignVias) {
    this.headsignVias = deduplicator.deduplicateString2DArray(headsignVias);
    return this;
  }

  public int[] gtfsSequenceOfStopIndex() {
    return gtfsSequenceOfStopIndex;
  }

  public ScheduledTripTimesBuilder withGtfsSequenceOfStopIndex(int[] gtfsSequenceOfStopIndex) {
    this.gtfsSequenceOfStopIndex = deduplicator.deduplicateIntArray(gtfsSequenceOfStopIndex);
    return this;
  }

  public ScheduledTripTimes build() {
    normalizeTimes();
    return new ScheduledTripTimes(this);
  }

  /**
   * Times are always shifted to zero based on the first departure time. This is essential for
   * frequencies and deduplication.
   */
  private void normalizeTimes() {
    if (departureTimes == null) {
      this.departureTimes = arrivalTimes;
    }
    if (arrivalTimes == null) {
      this.arrivalTimes = departureTimes;
    }

    int shift = departureTimes[0];
    if (shift == 0) {
      return;
    }
    this.departureTimes = timeShift(departureTimes, shift);
    if (arrivalTimes != departureTimes) {
      this.arrivalTimes = timeShift(arrivalTimes, shift);
    }
    this.timeShift += shift;
  }

  int[] timeShift(int[] a, int shift) {
    if (shift == 0) {
      return a;
    }
    for (int i = 0; i < a.length; i++) {
      a[i] -= shift;
    }
    return a;
  }
}
