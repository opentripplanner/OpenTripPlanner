package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.Arrays;
import java.util.List;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptorlegacy._data.transit.TestTransitData;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * Mock the TransferGenerator
 */
public class TransferGeneratorDummy {

  @SafeVarargs
  public static TransferGenerator<TestTripSchedule> dummyTransferGenerator(
    final List<TripToTripTransfer<TestTripSchedule>>... transfers
  ) {
    TestTransitData data = new TestTransitData();
    return new TransferGenerator<TestTripSchedule>(null, data.transitData().slackProvider(), data) {
      @Override
      public List<List<TripToTripTransfer<TestTripSchedule>>> findAllPossibleTransfers(
        List<TransitPathLeg<TestTripSchedule>> transitLegs
      ) {
        return Arrays.asList(transfers);
      }
    };
  }
}
