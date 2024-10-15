package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Arrays;
import java.util.List;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * Mock the TransferGenerator
 */
public class TransferGeneratorDummy {

  @SafeVarargs
  public static TransferGenerator<TestTripSchedule> dummyTransferGenerator(
    final List<TripToTripTransfer<TestTripSchedule>>... transfers
  ) {
    return new TransferGenerator<>(null, new TestTransitData()) {
      @Override
      public List<List<TripToTripTransfer<TestTripSchedule>>> findAllPossibleTransfers(
        List<TransitPathLeg<TestTripSchedule>> transitLegs
      ) {
        return Arrays.asList(transfers);
      }
    };
  }
}
