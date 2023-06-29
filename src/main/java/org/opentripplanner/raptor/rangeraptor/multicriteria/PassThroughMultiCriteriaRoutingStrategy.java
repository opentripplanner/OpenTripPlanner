package org.opentripplanner.raptor.rangeraptor.multicriteria;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoints;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideFactory;
import org.opentripplanner.raptor.rangeraptor.support.TimeBasedBoardingSupport;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

// TODO: 2023-06-29 via pass through: MultiCriteriaRoutingStrategy was a final class before
//  can we do inheritance this way or is it against the design?
public class PassThroughMultiCriteriaRoutingStrategy<
  T extends RaptorTripSchedule, R extends PatternRide<T>
>
  extends MultiCriteriaRoutingStrategy<T, R> {

  private final PassThroughPoints passThroughPoints;

  public PassThroughMultiCriteriaRoutingStrategy(
    McRangeRaptorWorkerState<T> state,
    TimeBasedBoardingSupport<T> boardingSupport,
    PatternRideFactory<T, R> patternRideFactory,
    RaptorCostCalculator<T> generalizedCostCalculator,
    SlackProvider slackProvider,
    ParetoSet<R> patternRides,
    PassThroughPoints passThroughPoints
  ) {
    super(
      state,
      boardingSupport,
      patternRideFactory,
      generalizedCostCalculator,
      slackProvider,
      patternRides
    );
    this.passThroughPoints = passThroughPoints;
  }

  @Override
  public void alightOnlyRegularTransferExist(int stopIndex, int stopPos, int alightSlack) {
    for (R ride : patternRides) {
      int c2 = ride.c2();
      if (passThroughPoints.isPassThroughPoint(c2, stopIndex)) {
        ride = patternRideFactory.createPatternRide(ride, c2 + 1);
      }
      state.transitToStop(ride, stopIndex, ride.trip().arrival(stopPos), alightSlack);
    }
  }
}
