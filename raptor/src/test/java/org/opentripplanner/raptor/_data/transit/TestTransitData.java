package org.opentripplanner.raptor._data.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorStopNameResolver;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTripPattern;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.raptor.spi.IntIterator;
import org.opentripplanner.raptor.spi.RaptorConstrainedBoardingSearch;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorPathConstrainedTransferSearch;
import org.opentripplanner.raptor.spi.RaptorRoute;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTimeTable;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.TestSlackProvider;
import org.opentripplanner.raptor.util.BitSetIterator;

@SuppressWarnings("UnusedReturnValue")
public class TestTransitData
  implements RaptorTransitDataProvider<TestTripSchedule>, RaptorTestConstants {

  private static final Pattern ROUTE_NAME_PATTERN = Pattern.compile("[-\\w]+");

  public static final TestTransferConstraint TX_GUARANTEED = TestTransferConstraint.guaranteed();
  public static final TestTransferConstraint TX_NOT_ALLOWED = TestTransferConstraint.notAllowed();

  // Slack defaults: 1 minute for transfer-slack, 0 minutes for board- and alight-slack.
  public static final RaptorSlackProvider SLACK_PROVIDER = new TestSlackProvider(60, 0, 0);

  private final List<List<RaptorTransfer>> transfersFromStop = new ArrayList<>();
  private final List<List<RaptorTransfer>> transfersToStop = new ArrayList<>();
  private final List<Set<Integer>> routeIndexesByStopIndex = new ArrayList<>();
  private final List<TestRoute> routes = new ArrayList<>();
  private final List<TestConstrainedTransfer> constrainedTransfers = new ArrayList<>();
  private int boardCostSec = 600;
  private int transferCostSec = 0;
  private double waitReluctance = 1.0;

  private final int[] stopBoardAlightTransferCosts = new int[NUM_STOPS];

  private final RaptorRequestBuilder<TestTripSchedule> requestBuilder =
    new RaptorRequestBuilder<>();

  private RaptorSlackProvider slackProvider = SLACK_PROVIDER;

  /// Create an new instance and call {@link #withTimetables(String)}
  public static TestTransitData of(String routeTimetables) {
    return new TestTransitData().withTimetables(routeTimetables);
  }

  public TestTransitData access(String... accessList) {
    access(Arrays.stream(accessList).map(TestAccessEgress::of).toArray(TestAccessEgress[]::new));
    return this;
  }

  public TestTransitData access(RaptorAccessEgress... accessList) {
    requestBuilder.searchParams().addAccessPaths(accessList);
    return this;
  }

  public TestTransitData egress(String... egressList) {
    egress(
      Arrays.stream(egressList)
        .map(TestTransitData::flipEgress)
        .map(TestAccessEgress::of)
        .toArray(TestAccessEgress[]::new)
    );
    return this;
  }

  public TestTransitData egress(RaptorAccessEgress... egressList) {
    requestBuilder.searchParams().addEgressPaths(egressList);
    return this;
  }

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
    return new TestCostCalculator(
      boardCostSec,
      transferCostSec,
      waitReluctance,
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
          .filter(tx -> tx.match(fromTrip, fromStopPosition, toTrip, toStopPosition))
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
    return RaptorTestConstants::stopIndexToName;
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

  public RaptorRequestBuilder<TestTripSchedule> requestBuilder() {
    return requestBuilder;
  }

  public TestRoute getRoute(int index) {
    return routes.get(index);
  }

  public void debugToStdErr(RaptorRequestBuilder<TestTripSchedule> request, boolean dryRun) {
    var debug = request.debug();

    if (debug.stops().isEmpty()) {
      debug.withStops(stopsVisited());
    }
    var logger = new SystemErrDebugLogger(true, dryRun);

    debug
      .withStopArrivalListener(logger::stopArrivalLister)
      .withPatternRideDebugListener(logger::patternRideLister)
      .withPathFilteringListener(logger::pathFilteringListener)
      .withLogger(logger);
  }

  /// Build a test data with multiple routes. The route name is generated(R1, R2 ...) if not
  /// provided. See  {@link TestRoute#withTimetable(String)} for timetable format.
  ///
  /// For exampe, creating R1 and Route-55:
  /// ```
  /// A      B      C      F
  /// 10:00  10:20  10:25  10:45
  /// -- Comments are allowed on route separator lines..
  /// Route-55
  /// B     C
  /// 10:05 11:05
  /// 11:05 12:05
  ///```
  public TestTransitData withTimetables(String routeTimetables) {
    int routeIndex = 0;
    for (String timetable : routeTimetables.split("\s*--.*\n")) {
      if (!timetable.isBlank()) {
        var firstLine = timetable.lines().findFirst().orElseThrow();
        if (ROUTE_NAME_PATTERN.matcher(firstLine.trim()).matches()) {
          withTimetable(firstLine, timetable.substring(firstLine.length() + 1).trim());
        } else {
          withTimetable("R" + (++routeIndex), timetable.trim());
        }
      }
    }
    return this;
  }

  public TestTransitData withTimetable(String routeName, String timetable) {
    if (!timetable.isBlank()) {
      withRoute(TestRoute.route(routeName).timetable(timetable.trim()));
    }
    return this;
  }

  public TestTransitData withRoute(TestRoute route) {
    this.routes.add(route);
    int routeIndex = this.routes.indexOf(route);
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

  public TestTransitData withTransfer(int fromStop, TestTransfer transfer) {
    expandNumOfStops(Math.max(fromStop, transfer.stop()));
    transfersFromStop.get(fromStop).add(transfer);
    transfersToStop.get(transfer.stop()).add(transfer.reverse(fromStop));
    return this;
  }

  public TestTransitData withTransferCost(int transferCostSec) {
    this.transferCostSec = transferCostSec;
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
    TestTransferConstraint constraint
  ) {
    int fromStopPos = fromTrip.pattern().findStopPositionAfter(0, fromStop);
    int toStopPos = toTrip.pattern().findStopPositionAfter(0, toStop);

    for (TestRoute route : routes) {
      route.addTransferConstraint(fromTrip, fromStopPos, toTrip, toStopPos, constraint);
    }
    constrainedTransfers.add(
      new TestConstrainedTransfer(
        constraint,
        fromTrip,
        fromStopPos,
        toTrip,
        toStopPos,
        toTrip.tripSortIndex(),
        toTrip.departure(toStopPos)
      )
    );
    return this;
  }

  public TestTransitData withStopBoardAlightTransferCost(int stop, int boardAlightTransferCost) {
    stopBoardAlightTransferCosts[stop] = boardAlightTransferCost;
    return this;
  }

  public TestConstrainedTransfer findConstrainedTransfer(
    TestTripSchedule fromTrip,
    int fromStop,
    int fromStopPosition,
    TestTripSchedule toTrip,
    int toStop,
    int toStopPosition
  ) {
    for (var tx : constrainedTransfers) {
      if (tx.match(fromTrip, fromStopPosition, toTrip, toStopPosition)) {
        return tx;
      }
    }
    return null;
  }

  public TestTransitData withBoardCost(int boardCostSec) {
    this.boardCostSec = boardCostSec;
    return this;
  }

  public TestTransitData withWaitReluctance(double waitReluctance) {
    this.waitReluctance = waitReluctance;
    return this;
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

  private List<Integer> stopsVisited() {
    final List<Integer> stops = new ArrayList<>();
    for (int i = 0; i < routeIndexesByStopIndex.size(); i++) {
      if (!routeIndexesByStopIndex.get(i).isEmpty()) {
        stops.add(i);
      }
    }
    return stops;
  }

  private static String flipEgress(String egress) {
    int pos = egress.indexOf('~');
    return egress.substring(pos + 1).trim() + " ~ " + egress.substring(0, pos).trim();
  }
}
