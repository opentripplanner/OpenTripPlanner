package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.trip.siri.support.NaturalLanguageStringHelper.getFirstStringFromList;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.trip.siri.mapping.OccupancyMapper;
import org.opentripplanner.utils.lang.StringUtils;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.RecordedCall;
import uk.org.siri.siri21.StopPointRefStructure;

/**
 * This class is a wrapper around either a {@link RecordedCall} or an {@link EstimatedCall}, making
 * it possible to iterate over both of the types at once.
 * <p>
 * Instances are created via the {@link #of(EstimatedVehicleJourney)} factory which validates and
 * sorts calls during parsing, making invalid {@code CallWrapper} instances unrepresentable.
 * <p>
 * The SIRI/JAXB enumerations of the underlying call are not exposed; the wrapper maps them to the
 * corresponding OTP types ({@link OccupancyStatus}, {@link PickDrop}).
 */
public interface CallWrapper {
  /**
   * Parse and validate all calls from an {@link EstimatedVehicleJourney}. Each call must have a
   * non-empty stop point ref and at least one of Order or VisitNumber (Order is preferred when both
   * are present). All calls must use the same strategy (all Order or all VisitNumber). The returned
   * list is sorted by sort order.
   *
   * @return a successful sorted list of calls, or a failure with the appropriate error type
   */
  static List<CallWrapper> of(EstimatedVehicleJourney estimatedVehicleJourney)
    throws UpdateException {
    List<CallWrapper> result = new ArrayList<>();
    boolean hasCallWithOrder = false;
    boolean hasCallWithoutOrder = false;

    if (estimatedVehicleJourney.getRecordedCalls() != null) {
      for (var call : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
        var sortOrder = validateCall(
          call.getStopPointRef(),
          call.getOrder(),
          call.getVisitNumber()
        );
        var hasOrder = call.getOrder() != null;
        hasCallWithOrder |= hasOrder;
        hasCallWithoutOrder |= !hasOrder;
        result.add(new RecordedCallWrapper(call, sortOrder));
      }
    }

    if (estimatedVehicleJourney.getEstimatedCalls() != null) {
      for (var call : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
        var sortOrder = validateCall(
          call.getStopPointRef(),
          call.getOrder(),
          call.getVisitNumber()
        );
        var hasOrder = call.getOrder() != null;
        hasCallWithOrder |= hasOrder;
        hasCallWithoutOrder |= !hasOrder;
        result.add(new EstimatedCallWrapper(call, sortOrder));
      }
    }

    if (hasCallWithOrder && hasCallWithoutOrder) {
      throw UpdateException.of(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER);
    }

    result.sort(Comparator.comparingInt(CallWrapper::getSortOrder));
    return List.copyOf(result);
  }

  /**
   * Validate a single call's stop point ref and resolve its sort order from Order/VisitNumber.
   */
  private static int validateCall(
    StopPointRefStructure stopPointRef,
    java.math.BigInteger order,
    java.math.BigInteger visitNumber
  ) throws UpdateException {
    var ref = stopPointRef != null ? stopPointRef.getValue() : null;
    if (StringUtils.hasNoValueOrNullAsString(ref)) {
      throw UpdateException.of(UpdateErrorType.EMPTY_STOP_POINT_REF);
    }
    if (order == null && visitNumber == null) {
      throw UpdateException.of(UpdateErrorType.MISSING_CALL_ORDER);
    }
    return order != null ? order.intValueExact() : visitNumber.intValueExact();
  }

  String getStopPointRef();

  /**
   * Return the sort order of this call, resolved during parsing from either Order or VisitNumber.
   */
  int getSortOrder();

  Boolean isCancellation();
  Boolean isPredictionInaccurate();
  boolean isExtraCall();

  /**
   * The occupancy of this call, or {@code null} when not set.
   */
  OccupancyStatus getOccupancy();

  /**
   * The destination display (headsign) of this call, or an empty string if not set.
   */
  String destinationDisplay();

  ZonedDateTime getAimedArrivalTime();
  ZonedDateTime getExpectedArrivalTime();
  ZonedDateTime getActualArrivalTime();
  ZonedDateTime getAimedDepartureTime();
  ZonedDateTime getExpectedDepartureTime();
  ZonedDateTime getActualDepartureTime();

  /**
   * The drop-off (arrival) change of this call, to be resolved against the scheduled value.
   */
  PickDropChange dropOff();

  /**
   * The pick-up (departure) change of this call, to be resolved against the scheduled value.
   */
  PickDropChange pickUp();

  /// Whether the call is a RecordedCall or not
  boolean isRecorded();

  /// Whether the vehicle has arrived at the stop.
  boolean hasArrived();

  /// Whether the vehicle has departed from the stop.
  boolean hasDeparted();

  final class EstimatedCallWrapper implements CallWrapper {

    private final EstimatedCall call;
    private final int sortOrder;

    private EstimatedCallWrapper(EstimatedCall estimatedCall, int sortOrder) {
      this.call = estimatedCall;
      this.sortOrder = sortOrder;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
    }

    @Override
    public int getSortOrder() {
      return sortOrder;
    }

    @Override
    public Boolean isCancellation() {
      return call.isCancellation();
    }

    @Override
    public Boolean isPredictionInaccurate() {
      return call.isPredictionInaccurate();
    }

    @Override
    public boolean isExtraCall() {
      return Boolean.TRUE.equals(call.isExtraCall());
    }

    @Override
    public OccupancyStatus getOccupancy() {
      return call.getOccupancy() == null
        ? null
        : OccupancyMapper.mapOccupancyStatus(call.getOccupancy());
    }

    @Override
    public String destinationDisplay() {
      return getFirstStringFromList(call.getDestinationDisplaies());
    }

    @Override
    public ZonedDateTime getAimedArrivalTime() {
      return call.getAimedArrivalTime();
    }

    @Override
    public ZonedDateTime getExpectedArrivalTime() {
      return call.getExpectedArrivalTime();
    }

    @Override
    public ZonedDateTime getActualArrivalTime() {
      return null;
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }

    @Override
    public ZonedDateTime getExpectedDepartureTime() {
      return call.getExpectedDepartureTime();
    }

    @Override
    public ZonedDateTime getActualDepartureTime() {
      return null;
    }

    @Override
    public PickDropChange dropOff() {
      return PickDropChange.ofArrival(
        isCancellation(),
        call.getArrivalStatus(),
        call.getArrivalBoardingActivity()
      );
    }

    @Override
    public PickDropChange pickUp() {
      return PickDropChange.ofDeparture(
        isCancellation(),
        call.getDepartureStatus(),
        call.getDepartureBoardingActivity()
      );
    }

    @Override
    public boolean isRecorded() {
      return false;
    }

    @Override
    public boolean hasArrived() {
      return call.getArrivalStatus() == CallStatusEnumeration.ARRIVED;
    }

    @Override
    public boolean hasDeparted() {
      return false;
    }

    @Override
    public int hashCode() {
      return call.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof EstimatedCallWrapper estimatedCallWrapper)) {
        return false;
      }
      return call.equals(estimatedCallWrapper.call);
    }
  }

  final class RecordedCallWrapper implements CallWrapper {

    private final RecordedCall call;
    private final int sortOrder;

    private RecordedCallWrapper(RecordedCall recordedCall, int sortOrder) {
      this.call = recordedCall;
      this.sortOrder = sortOrder;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
    }

    @Override
    public int getSortOrder() {
      return sortOrder;
    }

    @Override
    public Boolean isCancellation() {
      return call.isCancellation();
    }

    @Override
    public Boolean isPredictionInaccurate() {
      return call.isPredictionInaccurate();
    }

    @Override
    public boolean isExtraCall() {
      return Boolean.TRUE.equals(call.isExtraCall());
    }

    @Override
    public OccupancyStatus getOccupancy() {
      return call.getOccupancy() == null
        ? null
        : OccupancyMapper.mapOccupancyStatus(call.getOccupancy());
    }

    @Override
    public String destinationDisplay() {
      return getFirstStringFromList(call.getDestinationDisplaies());
    }

    @Override
    public ZonedDateTime getAimedArrivalTime() {
      return call.getAimedArrivalTime();
    }

    @Override
    public ZonedDateTime getExpectedArrivalTime() {
      return call.getExpectedArrivalTime();
    }

    @Override
    public ZonedDateTime getActualArrivalTime() {
      return call.getActualArrivalTime();
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }

    @Override
    public ZonedDateTime getExpectedDepartureTime() {
      return call.getExpectedDepartureTime();
    }

    @Override
    public ZonedDateTime getActualDepartureTime() {
      return call.getActualDepartureTime();
    }

    @Override
    public PickDropChange dropOff() {
      return PickDropChange.ofArrival(isCancellation(), null, null);
    }

    @Override
    public PickDropChange pickUp() {
      return PickDropChange.ofDeparture(isCancellation(), null, null);
    }

    @Override
    public boolean isRecorded() {
      return true;
    }

    @Override
    public boolean hasArrived() {
      return true;
    }

    @Override
    public boolean hasDeparted() {
      return call.getActualDepartureTime() != null;
    }

    @Override
    public int hashCode() {
      return call.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof RecordedCallWrapper recordedCallWrapper)) {
        return false;
      }
      return call.equals(recordedCallWrapper.call);
    }
  }
}
