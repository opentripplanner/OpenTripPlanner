package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static org.opentripplanner.routing.algorithm.transferoptimization.services.TransferGeneratorDummy.dummyTransferGenerator;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.TestPathBuilder;
import org.opentripplanner.raptor._data.transit.TestTransitData;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultCostCalculator;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;
import org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter.MinCostPathTailFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.services.OptimizePathDomainService;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TestTransferBuilder;

class TestUtils implements RaptorTestConstants {

  private static final int BOARD_COST_SEC = 0;
  private static final int TRANSFER_COST_SEC = 0;
  private static final double WAIT_RELUCTANCE = 1.0;

  private static final RaptorCostCalculator<TestTripSchedule> COST_CALCULATOR = new DefaultCostCalculator<>(
    BOARD_COST_SEC,
    TRANSFER_COST_SEC,
    WAIT_RELUCTANCE,
    null,
    null
  );

  static TestPathBuilder pathBuilder() {
    return new TestPathBuilder(TestTransitData.SLACK_PROVIDER, COST_CALCULATOR);
  }

  static TripToTripTransfer<TestTripSchedule> tx(
    TestTripSchedule fromTrip,
    int fromStop,
    TestTripSchedule toTrip,
    int toStop,
    WalkDurationForStopCombinations txCost
  ) {
    return tx(fromTrip, fromStop, toTrip, toStop, txCost.walkDuration(fromStop, toStop));
  }

  static TripToTripTransfer<TestTripSchedule> tx(
    TestTripSchedule fromTrip,
    int fromStop,
    TestTripSchedule toTrip,
    int toStop,
    int txCost
  ) {
    return TestTransferBuilder.tx(fromTrip, fromStop, toTrip, toStop).walk(txCost).build();
  }

  static OptimizePathDomainService<TestTripSchedule> domainService(
    List<PassThroughPoint> passThroughPoints,
    final List<TripToTripTransfer<TestTripSchedule>>... transfers
  ) {
    var filter = new MinCostPathTailFilterFactory<TestTripSchedule>(false, false).createFilter();
    filter = new PassThroughPathTailFilter<>(filter, passThroughPoints);
    var generator = dummyTransferGenerator(transfers);

    return new OptimizePathDomainService<>(
      generator,
      COST_CALCULATOR,
      TestTransitData.SLACK_PROVIDER,
      null,
      null,
      0.0,
      filter,
      (new RaptorTestConstants() {})::stopIndexToName
    );
  }

  static <T> T first(Collection<T> c) {
    return c.stream().findFirst().orElseThrow();
  }

  /**
   * Remove stuff we do not care about, like the priority cost and times.
   */
  static String pathFocus(String resultString) {
    return resultString.replaceAll(" Tₚ[\\d_]+]", "]").replaceAll(" \\d{2}:\\d{2}", "");
  }
}
