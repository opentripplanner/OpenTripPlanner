package org.opentripplanner.transit.model.timetable;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;
import org.opentripplanner.utils.time.TimeUtils;

public abstract class AbstractTripTimesBuilder<
  T extends TripTimes, B extends AbstractTripTimesBuilder<T, B>
> {

  protected static final int NOT_SET = -1;
  private static final BitSet EMPTY_BIT_SET = new BitSet(0);
  protected final DeduplicatorService deduplicator;
  protected int timeShift;
  protected int serviceCode;
  protected int[] arrivalTimes;
  protected int[] departureTimes;
  protected BitSet timepoints;
  protected Trip trip;
  protected List<BookingInfo> dropOffBookingInfos;
  protected List<BookingInfo> pickupBookingInfos;
  protected I18NString[] headsigns;
  protected String[][] headsignVias;
  protected int[] gtfsSequenceOfStopIndex;

  public AbstractTripTimesBuilder(
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

  abstract T build();

  public int timeShift() {
    return timeShift;
  }

  public B withTimeShift(int timeShift) {
    this.timeShift = timeShift;
    return instance();
  }

  private B instance() {
    return (B) this;
  }

  /**
   * Add the {@code delta} to the existing timeShift. This is useful when moving a trip from one
   * time-zone to another.
   */
  public B plusTimeShift(int delta) {
    this.timeShift += delta;
    return instance();
  }

  public int serviceCode() {
    return serviceCode;
  }

  public B withServiceCode(int serviceCode) {
    this.serviceCode = serviceCode;
    return instance();
  }

  public int[] arrivalTimes() {
    return arrivalTimes;
  }

  public B withArrivalTimes(int[] arrivalTimes) {
    this.arrivalTimes = deduplicator.deduplicateIntArray(arrivalTimes);
    return instance();
  }

  /** For unit testing, uses {@link TimeUtils#time(String)}. */
  public B withArrivalTimes(String arrivalTimes) {
    return withArrivalTimes(TimeUtils.times(arrivalTimes));
  }

  public int[] departureTimes() {
    return departureTimes;
  }

  public B withDepartureTimes(int[] departureTimes) {
    this.departureTimes = deduplicator.deduplicateIntArray(departureTimes);
    return instance();
  }

  /** For unit testing, uses {@link TimeUtils#time(String)}. */
  public B withDepartureTimes(String departureTimes) {
    return withDepartureTimes(TimeUtils.times(departureTimes));
  }

  public BitSet timepoints() {
    return timepoints == null ? EMPTY_BIT_SET : timepoints;
  }

  public B withTimepoints(BitSet timepoints) {
    this.timepoints = deduplicator.deduplicateBitSet(timepoints);
    return instance();
  }

  public Trip trip() {
    return trip;
  }

  public B withTrip(Trip trip) {
    this.trip = trip;
    return instance();
  }

  public List<BookingInfo> dropOffBookingInfos() {
    return dropOffBookingInfos == null ? List.of() : dropOffBookingInfos;
  }

  public B withDropOffBookingInfos(List<BookingInfo> dropOffBookingInfos) {
    this.dropOffBookingInfos = deduplicator.deduplicateImmutableList(
      BookingInfo.class,
      dropOffBookingInfos
    );
    return instance();
  }

  public List<BookingInfo> pickupBookingInfos() {
    return pickupBookingInfos == null ? List.of() : pickupBookingInfos;
  }

  public B withPickupBookingInfos(List<BookingInfo> pickupBookingInfos) {
    this.pickupBookingInfos = deduplicator.deduplicateImmutableList(
      BookingInfo.class,
      pickupBookingInfos
    );
    return instance();
  }

  public I18NString[] headsigns() {
    return headsigns;
  }

  public B withHeadsigns(I18NString[] headsigns) {
    this.headsigns = deduplicator.deduplicateObjectArray(I18NString.class, headsigns);
    return instance();
  }

  public String[][] headsignVias() {
    return headsignVias;
  }

  public B withHeadsignVias(String[][] headsignVias) {
    this.headsignVias = deduplicator.deduplicateString2DArray(headsignVias);
    return instance();
  }

  public int[] gtfsSequenceOfStopIndex() {
    return gtfsSequenceOfStopIndex;
  }

  public B withGtfsSequenceOfStopIndex(int[] gtfsSequenceOfStopIndex) {
    this.gtfsSequenceOfStopIndex = deduplicator.deduplicateIntArray(gtfsSequenceOfStopIndex);
    return instance();
  }

  /**
   * Times are always shifted to zero based on the first departure time. This is essential for
   * frequencies and deduplication.
   */
  protected void normalizeTimes() {
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
