package org.opentripplanner.raptor.rangeraptor.transit;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.raptor.api.request.SearchParams;
import org.opentripplanner.raptor.api.view.ArrivalView;

public class PassThroughForwardRaptorTransitCalculator<T extends RaptorTripSchedule>
  extends ForwardRaptorTransitCalculator<T> {

  private final int requiredC2;

  public PassThroughForwardRaptorTransitCalculator(
    SearchParams s,
    RaptorTuningParameters t,
    int requiredC2
  ) {
    super(
      s.routerEarliestDepartureTime(),
      s.routerSearchWindowInSeconds(),
      s.latestArrivalTime(),
      t.iterationDepartureStepInSeconds()
    );
    this.requiredC2 = requiredC2;
  }

  @Override
  public Collection<String> validate(ArrivalView<T> destArrival) {
    var errors = super.validate(destArrival);

    if (destArrival.c2() != requiredC2) {
      errors.add("Invalid C2 value: " + destArrival.c2() + ".");
    }

    return errors;
  }
}
