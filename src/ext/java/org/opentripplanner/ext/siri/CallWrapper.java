package org.opentripplanner.ext.siri;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import uk.org.siri.siri20.EstimatedCall;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.RecordedCall;

/**
 * This class is a wrapper around either a {@link RecordedCall} or an {@link EstimatedCall}, making
 * it possible to iterate over both of the types at once.
 */
public sealed interface CallWrapper {
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

  ZonedDateTime getAimedDepartureTime();

  final class EstimatedCallWrapper implements CallWrapper {

    private final EstimatedCall call;

    private EstimatedCallWrapper(EstimatedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }
  }

  final class RecordedCallWrapper implements CallWrapper {

    private final RecordedCall call;

    private RecordedCallWrapper(RecordedCall estimatedCall) {
      this.call = estimatedCall;
    }

    @Override
    public ZonedDateTime getAimedDepartureTime() {
      return call.getAimedDepartureTime();
    }
  }
}
