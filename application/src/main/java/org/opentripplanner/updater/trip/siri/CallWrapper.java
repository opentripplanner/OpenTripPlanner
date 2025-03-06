package org.opentripplanner.updater.trip.siri;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import uk.org.siri.siri20.ArrivalBoardingActivityEnumeration;
import uk.org.siri.siri20.CallStatusEnumeration;
import uk.org.siri.siri20.DepartureBoardingActivityEnumeration;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.NaturalLanguageStringStructure;
import uk.org.siri.siri20.OccupancyEnumeration;
import uk.org.siri.siri20.RecordedCall;

/**
 * This class is a wrapper around either a {@link RecordedCall} or an {@link EstimatedCall}, making
 * it possible to iterate over both of the types at once.
 */
public interface CallWrapper {
  static CallWrapper of(EstimatedCall estimatedCall) {
    return new EstimatedCallWrapper(estimatedCall);
  }

  static CallWrapper of(RecordedCall recordedCall) {
    return new RecordedCallWrapper(recordedCall);
  }

  static List<CallWrapper> of(EstimatedVehicleJourney estimatedVehicleJourney) {
    List<CallWrapper> result = new ArrayList<>();

    if (estimatedVehicleJourney.getRecordedCalls() != null) {
      for (var recordedCall : estimatedVehicleJourney.getRecordedCalls().getRecordedCalls()) {
        result.add(new RecordedCallWrapper(recordedCall));
      }
    }

    if (estimatedVehicleJourney.getEstimatedCalls() != null) {
      for (var estimatedCall : estimatedVehicleJourney.getEstimatedCalls().getEstimatedCalls()) {
        result.add(new EstimatedCallWrapper(estimatedCall));
      }
    }

    return List.copyOf(result);
  }

  String getStopPointRef();
  Boolean isCancellation();
  Boolean isPredictionInaccurate();
  OccupancyEnumeration getOccupancy();
  List<NaturalLanguageStringStructure> getDestinationDisplaies();
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

  final class EstimatedCallWrapper implements CallWrapper {

    private final EstimatedCall call;

    private EstimatedCallWrapper(EstimatedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
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
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplaies() {
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

    private RecordedCallWrapper(RecordedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public String getStopPointRef() {
      return call.getStopPointRef() != null ? call.getStopPointRef().getValue() : null;
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
    public OccupancyEnumeration getOccupancy() {
      return call.getOccupancy();
    }

    @Override
    public List<NaturalLanguageStringStructure> getDestinationDisplaies() {
      return List.of();
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
