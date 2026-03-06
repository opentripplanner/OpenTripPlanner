package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.updater.spi.UpdateErrorType;
import org.opentripplanner.utils.lang.StringUtils;
import uk.org.siri.siri21.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri21.CallStatusEnumeration;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.NaturalLanguageStringStructure;
import uk.org.siri.siri21.OccupancyEnumeration;
import uk.org.siri.siri21.RecordedCall;
import uk.org.siri.siri21.StopPointRefStructure;

/**
 * This class is a wrapper around either a {@link RecordedCall} or an {@link EstimatedCall}, making
 * it possible to iterate over both of the types at once.
 * <p>
 * Instances are created via the {@link #of(EstimatedVehicleJourney)} factory which validates and
 * sorts calls during parsing, making invalid {@code CallWrapper} instances unrepresentable.
 */
public interface CallWrapper {
  /**
   * Parse and validate all calls from an {@link EstimatedVehicleJourney}. Each call must have a
   * non-empty stop point ref and exactly one of Order or VisitNumber. All calls must use the same
   * strategy (all Order or all VisitNumber). The returned list is sorted by sort order.
   *
   * @return a successful sorted list of calls, or a failure with the appropriate error type
   */
  static Result<List<CallWrapper>, UpdateErrorType> of(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    List<CallWrapper> result = new ArrayList<>();
    boolean hasOrderCalls = false;
    boolean hasVisitNumberCalls = false;

    if (estimatedVehicleJourney.getRecordedCalls() != null) {
      for (var call : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
        var sortOrder = validateCall(
          call.getStopPointRef(),
          call.getOrder(),
          call.getVisitNumber()
        );
        if (sortOrder.isFailure()) {
          return sortOrder.toFailureResult();
        }
        hasOrderCalls |= call.getOrder() != null;
        hasVisitNumberCalls |= call.getVisitNumber() != null;
        result.add(new RecordedCallWrapper(call, sortOrder.successValue()));
      }
    }

    if (estimatedVehicleJourney.getEstimatedCalls() != null) {
      for (var call : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
        var sortOrder = validateCall(
          call.getStopPointRef(),
          call.getOrder(),
          call.getVisitNumber()
        );
        if (sortOrder.isFailure()) {
          return sortOrder.toFailureResult();
        }
        hasOrderCalls |= call.getOrder() != null;
        hasVisitNumberCalls |= call.getVisitNumber() != null;
        result.add(new EstimatedCallWrapper(call, sortOrder.successValue()));
      }
    }

    // we reject messages that contain both Order and VisitNumber since we do not see any obvious
    // use case that requires both, and making them mutually exclusive make the implementation
    // simpler. We can relax this validation rule later if valid use cases are identified.
    if (hasOrderCalls && hasVisitNumberCalls) {
      return Result.failure(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER);
    }

    result.sort(Comparator.comparingInt(CallWrapper::getSortOrder));

    return Result.success(List.copyOf(result));
  }

  /**
   * Validate a single call's stop point ref and resolve its sort order from Order/VisitNumber.
   */
  private static Result<Integer, UpdateErrorType> validateCall(
    StopPointRefStructure stopPointRef,
    java.math.BigInteger order,
    java.math.BigInteger visitNumber
  ) {
    var ref = stopPointRef != null ? stopPointRef.getValue() : null;
    if (StringUtils.hasNoValueOrNullAsString(ref)) {
      return Result.failure(UpdateErrorType.EMPTY_STOP_POINT_REF);
    }
    if (order == null && visitNumber == null) {
      return Result.failure(UpdateErrorType.MISSING_CALL_ORDER);
    }
    if (order != null && visitNumber != null) {
      return Result.failure(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER);
    }
    return Result.success(order != null ? order.intValueExact() : visitNumber.intValueExact());
  }

  String getStopPointRef();

  /**
   * Return the sort order of this call, resolved during parsing from either Order or VisitNumber.
   */
  int getSortOrder();

  Boolean isCancellation();
  Boolean isPredictionInaccurate();
  boolean isExtraCall();
  OccupancyEnumeration getOccupancy();
  List<NaturalLanguageStringStructure> getDestinationDisplays();
  ZonedDateTime getAimedArrivalTime();
  ZonedDateTime getExpectedArrivalTime();
  ZonedDateTime getActualArrivalTime();
  CallStatusEnumeration getArrivalStatus();
  ArrivalBoardingActivityEnumeration getArrivalBoardingActivity();
  ZonedDateTime getAimedDepartureTime();
  ZonedDateTime getExpectedDepartureTime();
  ZonedDateTime getActualDepartureTime();
  CallStatusEnumeration getDepartureStatus();
  DepartureBoardingActivityEnumeration getDepartureBoardingActivity();

  /// Whether the call is a RecordedCall or not
  boolean isRecorded();

  /// Whether the vehicle has arrived at the stop.
  default boolean hasArrived() {
    return isRecorded() || getArrivalStatus() == CallStatusEnumeration.ARRIVED;
  }

  /// Whether the vehicle has departed from the stop.
  default boolean hasDeparted() {
    return isRecorded() && getActualDepartureTime() != null;
  }

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
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplays() {
      return call.getDestinationDisplaies();
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
    public CallStatusEnumeration getArrivalStatus() {
      return call.getArrivalStatus();
    }

    @Override
    public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
      return call.getArrivalBoardingActivity();
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
    public CallStatusEnumeration getDepartureStatus() {
      return call.getDepartureStatus();
    }

    @Override
    public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
      return call.getDepartureBoardingActivity();
    }

    @Override
    public boolean isRecorded() {
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
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplays() {
      return call.getDestinationDisplaies();
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
    public CallStatusEnumeration getArrivalStatus() {
      return null;
    }

    @Override
    public ArrivalBoardingActivityEnumeration getArrivalBoardingActivity() {
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
      return call.getActualDepartureTime();
    }

    @Override
    public CallStatusEnumeration getDepartureStatus() {
      return null;
    }

    @Override
    public DepartureBoardingActivityEnumeration getDepartureBoardingActivity() {
      return null;
    }

    @Override
    public boolean isRecorded() {
      return true;
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
