package org.opentripplanner.raptorlegacy._data.transit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.spi.DefaultSlackProvider;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.util.BitSetIterator;
import org.opentripplanner.raptorlegacy._data.RaptorTestConstants;
import org.opentripplanner.routing.algorithm.raptoradapter.api.DefaultTripPattern;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultRaptorTransfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.CostCalculatorFactory;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.GeneralizedCostParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.GeneralizedCostParametersBuilder;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripStopTime;
import org.opentripplanner.routing.algorithm.transferoptimization.services.TransferServiceAdaptor;

/**
 * @deprecated This was earlier part of Raptor and should not be used outside the Raptor
 *             module. Use the OTP model entities instead.
 */
@Deprecated
@SuppressWarnings("UnusedReturnValue")
public class TestTransitData
  implements RaptorTransitDataProvider<TestTripSchedule>, RaptorTestConstants {

  public static final TransferConstraint TX_GUARANTEED = TransferConstraint.of()
    .guaranteed()
    .build();
  public static final TransferConstraint TX_NOT_ALLOWED = TransferConstraint.of()
    .notAllowed()
    .build();
  public static final TransferConstraint TX_LONG_MIN_TIME = TransferConstraint.of()
    .minTransferTime(3600)
    .build();

  // Slack defaults: 1 minute for transfer-slack, 0 minutes for board- and alight-slack.
  public static final RaptorSlackProvider SLACK_PROVIDER = new DefaultSlackProvider(60, 0, 0);

  private final List<List<RaptorTransfer>> transfersFromStop = new ArrayList<>();
  private final List<List<RaptorTransfer>> transfersToStop = new ArrayList<>();
  private final List<Set<Integer>> routeIndexesByStopIndex = new ArrayList<>();
  private final List<TestRoute> routes = new ArrayList<>();
  private final List<ConstrainedTransfer> constrainedTransfers = new ArrayList<>();
  private final GeneralizedCostParametersBuilder costParamsBuilder = GeneralizedCostParameters.of();

  private final int[] stopBoardAlightTransferCosts = new int[NUM_STOPS];

  private RaptorSlackProvider slackProvider = SLACK_PROVIDER;

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersFromStop(int fromStop) {
    return transfersFromStop.get(fromStop).iterator();
  }

  @Override
  public Iterator<? extends RaptorTransfer> getTransfersToStop(int toStop) {
    return transfersToStop.get(toStop).iterator();
  }

  @Override
  public IntIterator routeIndexIterator(IntIterator stops) {
    BitSet routes = new BitSet();
    while (stops.hasNext()) {
      int stop = stops.next();
      for (int i : routeIndexesByStopIndex.get(stop)) {
        routes.set(i);
      }
    }
    return new BitSetIterator(routes);
  }

  @Override
  public RaptorRoute<TestTripSchedule> getRouteForIndex(int routeIndex) {
    return this.routes.get(routeIndex);
  }

  @Override
  public int numberOfStops() {
    return routeIndexesByStopIndex.size();
  }

  @Override
  public RaptorCostCalculator<TestTripSchedule> multiCriteriaCostCalculator() {
    return CostCalculatorFactory.createCostCalculator(
      costParamsBuilder.build(),
      stopBoardAlightTransferCosts()
    );
  }

  @Override
  public RaptorSlackProvider slackProvider() {
    return slackProvider;
  }

  public TestTransitData withSlackProvider(RaptorSlackProvider slackProvider) {
    this.slackProvider = slackProvider;
    return this;
  }

  @Override
  public RaptorPathConstrainedTransferSearch<TestTripSchedule> transferConstraintsSearch() {
    return new RaptorPathConstrainedTransferSearch<>() {
      @Nullable
      @Override
      public RaptorConstrainedTransfer findConstrainedTransfer(
        TestTripSchedule fromTrip,
        int fromStopPosition,
        TestTripSchedule toTrip,
        int toStopPosition
      ) {
        var list = routes
          .stream()
          .flatMap(r -> r.listTransferConstraintsForwardSearch().stream())
          .filter(tx -> tx.getSourceTrip().equals(fromTrip))
          .filter(tx -> tx.getSourceStopPos() == fromStopPosition)
          .filter(tx -> tx.getTrip().equals(toTrip))
          .filter(tx -> tx.getStopPositionInPattern() == toStopPosition)
          .toList();

        if (list.isEmpty()) {
          return null;
        }
        if (list.size() == 1) {
          return list.get(0);
        }
        throw new IllegalStateException("More than on transfers found: " + list);
      }
    };
  }

  @Override
  public RaptorStopNameResolver stopNameResolver() {
    // Index is translated: 1->'A', 2->'B', 3->'C' ...
    return this::stopIndexToName;
  }

  @Override
  public int getValidTransitDataStartTime() {
    return this.routes.stream()
      .mapToInt(route -> route.timetable().getTripSchedule(0).departure(0))
      .min()
      .orElseThrow();
  }

  @Override
  public int getValidTransitDataEndTime() {
    return this.routes.stream()
      .mapToInt(route -> {
        RaptorTimeTable<TestTripSchedule> timetable = route.timetable();
        RaptorTripPattern pattern = route.pattern();
        return timetable
          .getTripSchedule(timetable.numberOfTripSchedules() - 1)
          .departure(pattern.numberOfStopsInPattern() - 1);
      })
      .max()
      .orElseThrow();
  }

  @Override
  public RaptorConstrainedBoardingSearch<TestTripSchedule> transferConstraintsForwardSearch(
    int routeIndex
  ) {
    return getRoute(routeIndex).transferConstraintsForwardSearch();
  }

  @Override
  public RaptorConstrainedBoardingSearch<TestTripSchedule> transferConstraintsReverseSearch(
    int routeIndex
  ) {
    return getRoute(routeIndex).transferConstraintsReverseSearch();
  }

  public TestRoute getRoute(int index) {
    return routes.get(index);
  }

  public TestTransitData withRoute(TestRoute route) {
    this.routes.add(route);
    int routeIndex = this.routes.indexOf(route);
    ((TestTripPattern) route.pattern()).withPatternIndex(routeIndex);
    var pattern = route.pattern();
    for (int i = 0; i < pattern.numberOfStopsInPattern(); ++i) {
      int stopIndex = pattern.stopIndex(i);
      expandNumOfStops(stopIndex);
      routeIndexesByStopIndex.get(stopIndex).add(routeIndex);
    }
    return this;
  }

  public TestTransitData withRoutes(TestRoute... routes) {
    for (TestRoute route : routes) {
      withRoute(route);
    }
    return this;
  }

  public TestTransitData withTransfer(int fromStop, DefaultRaptorTransfer transfer) {
    expandNumOfStops(Math.max(fromStop, transfer.stop()));
    transfersFromStop.get(fromStop).add(transfer);
    transfersToStop.get(transfer.stop()).add(transfer.reverseOf(fromStop));
    return this;
  }

  public TestTransitData withGuaranteedTransfer(
    TestTripSchedule fromTrip,
    int fromStop,
    TestTripSchedule toTrip,
    int toStop
  ) {
    return withConstrainedTransfer(fromTrip, fromStop, toTrip, toStop, TX_GUARANTEED);
  }

  public void clearConstrainedTransfers() {
    constrainedTransfers.clear();
    for (TestRoute route : routes) {
      route.clearTransferConstraints();
    }
  }

  /**
   * Create constraint for a given transfer. If trip passes through the stop more than once
   * constraint will be placed on stop position for the first visit.
   * @param fromTrip initial trip
   * @param fromStop initial stop index
   * @param toTrip destination trip
   * @param toStop destination trip index
   * @param constraint constraint to set
   */
  public TestTransitData withConstrainedTransfer(
    TestTripSchedule fromTrip,
    int fromStop,
    TestTripSchedule toTrip,
    int toStop,
    TransferConstraint constraint
  ) {
    int fromStopPos = fromTrip.pattern().findStopPositionAfter(0, fromStop);
    int toStopPos = toTrip.pattern().findStopPositionAfter(0, toStop);

    for (TestRoute route : routes) {
      route.addTransferConstraint(fromTrip, fromStopPos, toTrip, toStopPos, constraint);
    }
    constrainedTransfers.add(
      new ConstrainedTransfer(
        null,
        new TestTransferPoint(fromStop, fromStopPos, fromTrip, false),
        new TestTransferPoint(toStop, toStopPos, toTrip, false),
        constraint
      )
    );
    return this;
  }

  public ConstrainedTransfer findConstrainedTransfer(
    TestTripSchedule fromTrip,
    int fromStop,
    int fromStopPosition,
    TestTripSchedule toTrip,
    int toStop,
    int toStopPosition
  ) {
    for (ConstrainedTransfer tx : constrainedTransfers) {
      if (
        ((TestTransferPoint) tx.getFrom()).matches(fromTrip, fromStop, fromStopPosition) &&
        ((TestTransferPoint) tx.getTo()).matches(toTrip, toStop, toStopPosition)
      ) {
        return tx;
      }
    }
    return null;
  }

  public TransferServiceAdaptor<TestTripSchedule> transferServiceAdaptor() {
    return new TransferServiceAdaptor<>(null, null) {
      @Override
      protected ConstrainedTransfer findTransfer(
        TripStopTime<TestTripSchedule> from,
        TestTripSchedule toTrip,
        int toStop,
        int toStopPosition
      ) {
        return findConstrainedTransfer(
          from.trip(),
          from.stop(),
          from.stopPosition(),
          toTrip,
          toStop,
          toStopPosition
        );
      }
    };
  }

  public List<DefaultTripPattern> getPatterns() {
    return routes.stream().map(TestRoute::pattern).toList();
  }

  /* private methods */

  private int[] stopBoardAlightTransferCosts() {
    // Not implemented, no test for this yet.
    return stopBoardAlightTransferCosts;
  }

  private void expandNumOfStops(int stopIndex) {
    for (int i = numberOfStops(); i <= stopIndex; ++i) {
      transfersFromStop.add(new ArrayList<>());
      transfersToStop.add(new ArrayList<>());
      routeIndexesByStopIndex.add(new HashSet<>());
    }
  }
}
