package org.opentripplanner.raptor.rangeraptor.destination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.rangeraptor.destination.DestinationArrivalParetoSetComparators.paretoComparator;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.NONE;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost.USE_C1;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_ARRIVAL_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_DEPARTURE_TIME;
import static org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime.USE_TIMETABLE;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.api.TestRaptorPath;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.debug.DebugLogger;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.request.DebugRequest;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.rangeraptor.debug.DebugHandlerFactory;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetCost;
import org.opentripplanner.raptor.rangeraptor.internalapi.ParetoSetTime;
import org.opentripplanner.raptor.rangeraptor.internalapi.SlackProvider;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleEventPublisher;
import org.opentripplanner.raptor.rangeraptor.lifecycle.LifeCycleSubscriptions;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.stop.StopArrivalFactoryC1;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRideC1;
import org.opentripplanner.raptor.rangeraptor.transit.EgressWithPenalty;
import org.opentripplanner.raptor.rangeraptor.transit.ForwardRaptorTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.RaptorTransitCalculator;
import org.opentripplanner.raptor.spi.RaptorConstants;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.spi.SearchDirection;

class DestinationArrivalPathsTest {

  private static final int ANY = 9_999;
  private static final int STOP = 101;
  private static final int ITERATION_DEPARTURE_TIME = 8 * 60 * 60;
  private static final int ALIGHT_TIME = ITERATION_DEPARTURE_TIME + 600;
  private static final int EGRESS_DURATION = 60;
  private static final int TIME_PENALTY = 300;
  private static final int NO_TIME_LIMIT = RaptorConstants.TIME_NOT_SET;

  private static final StopArrivalFactoryC1<RaptorTripSchedule> FACTORY =
    new StopArrivalFactoryC1<>();

  private record TestSlack(int boardSlack) implements RaptorSlackProvider {
    @Override
    public int transferSlack() {
      return 0;
    }

    @Override
    public int alightSlack(int slackIndex) {
      return 0;
    }

    @Override
    public int boardSlack(int slackIndex) {
      return boardSlack;
    }
  }

  private static final SlackProvider NO_SLACK = new SlackProvider() {
    @Override
    public int boardSlack(int slackIndex) {
      return 0;
    }

    @Override
    public int alightSlack(int slackIndex) {
      return 0;
    }

    @Override
    public int transferSlack() {
      return 0;
    }
  };

  private final LifeCycleSubscriptions subscriptions = new LifeCycleSubscriptions();
  private final List<DestinationArrival<RaptorTripSchedule>> mappedArrivals = new ArrayList<>();
  private LifeCycleEventPublisher lifeCycle;

  @BeforeEach
  void setUp() {
    mappedArrivals.clear();
  }

  @Test
  void stdRaptorBuildsPathsEagerlyAndOnlyForAcceptedArrivals() {
    var subject = createStd(USE_ARRIVAL_TIME, NO_TIME_LIMIT);

    subject.add(stopArrival(ALIGHT_TIME), egress());
    assertEquals(1, mappedArrivals.size());

    // Dominated by the first arrival: rejected, no path is built
    subject.add(stopArrival(ALIGHT_TIME + 100), egress());
    assertEquals(1, mappedArrivals.size());

    // Dominates the first arrival: accepted, and the first path is dropped from the result
    subject.add(stopArrival(ALIGHT_TIME - 100), egress());
    assertEquals(2, mappedArrivals.size());

    var paths = subject.listPaths();
    assertEquals(1, paths.size());
    assertEquals(2, mappedArrivals.size(), "listPaths() must not build paths for StdRaptor");
    assertEquals(
      ALIGHT_TIME - 100 + EGRESS_DURATION,
      paths.iterator().next().endTimeInclusivePenalty()
    );
  }

  @Test
  void mcRaptorBuildsPathsLazilyForTheArrivalsInTheResult() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);

    subject.add(stopArrival(ALIGHT_TIME), egress());
    subject.add(stopArrival(ALIGHT_TIME + 100), egress());
    subject.add(stopArrival(ALIGHT_TIME - 100), egress());
    assertTrue(mappedArrivals.isEmpty(), "No paths should be built during the search");

    var paths = subject.listPaths();
    assertEquals(1, paths.size());
    assertEquals(1, mappedArrivals.size());
    assertEquals(
      ALIGHT_TIME - 100 + EGRESS_DURATION,
      paths.iterator().next().endTimeInclusivePenalty()
    );
  }

  @Test
  void arrivalsExceedingTheTimeLimitAreRejected() {
    var subject = createStd(USE_ARRIVAL_TIME, ALIGHT_TIME + EGRESS_DURATION);

    subject.add(stopArrival(ALIGHT_TIME + 1), egress());
    assertTrue(subject.isEmpty());
    assertTrue(mappedArrivals.isEmpty());

    subject.add(stopArrival(ALIGHT_TIME), egress());
    assertFalse(subject.isEmpty());
  }

  @Test
  void reachedCurrentRoundIsSetOnAcceptedArrivalAndClearedInTheNextRound() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);
    assertFalse(subject.isReachedCurrentRound());

    subject.add(stopArrival(ALIGHT_TIME), egress());
    assertTrue(subject.isReachedCurrentRound());

    lifeCycle.prepareForNextRound(1);
    assertFalse(subject.isReachedCurrentRound());

    // A rejected arrival does not mark the round as reached
    subject.add(stopArrival(ALIGHT_TIME + 100), egress());
    assertFalse(subject.isReachedCurrentRound());
  }

  @Test
  void qualifyOnEmptySetIsTrueAndDoesNotAddToTheSet() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);

    assertTrue(subject.qualify(ANY, 0, ANY, ANY));
    assertTrue(subject.isEmpty());
  }

  @Test
  void consecutiveQualifyCallsDoNotInterfere() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);
    int arrivalTime = ALIGHT_TIME + EGRESS_DURATION;
    subject.add(stopArrival(ALIGHT_TIME), egress());

    assertFalse(subject.qualify(arrivalTime + 1, 0, 0, 0));
    assertTrue(subject.qualify(arrivalTime - 1, 0, 0, 0));
    assertFalse(subject.qualify(arrivalTime + 1, 0, 0, 0));
    // More transfers and later: dominated
    assertFalse(subject.qualify(arrivalTime, 1, 0, 0));
    // Fewer transfers is not possible here (0 already), same values: not better
    assertFalse(subject.qualify(arrivalTime, 0, 0, 0));

    // The set is not modified by qualify
    assertEquals(1, subject.listPaths().size());
  }

  @Test
  void qualifyUsesTheCurrentIterationDepartureTime() {
    var subject = createMc(USE_TIMETABLE, NO_TIME_LIMIT);
    int arrivalTime = ALIGHT_TIME + EGRESS_DURATION;

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME), egress());

    // Same iteration and same values: not better
    assertFalse(subject.qualify(arrivalTime, 0, 0, 0));

    // An earlier iteration is worse when the criteria are equal, a later is better
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);
    assertFalse(subject.qualify(arrivalTime, 0, 0, 0));
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME + 60);
    assertTrue(subject.qualify(arrivalTime, 0, 0, 0));
  }

  @Test
  void pathsAreBuiltFromTheArrivalAndNotFromTheCurrentIteration() {
    var subject = createMc(USE_TIMETABLE, NO_TIME_LIMIT);

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME), egress());
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);

    assertEquals(1, subject.listPaths().size());
    assertEquals(
      ITERATION_DEPARTURE_TIME,
      mappedArrivals.getFirst().iterationDepartureTime(),
      "The iteration departure time must be taken from the arrival, not the worker state"
    );
  }

  /**
   * Raptor decorates egress paths with the time-penalty, so the penalty is part of the egress
   * duration, and thereby part of the destination arrival time. It must be counted only once.
   */
  @Test
  void forwardEgressTimePenaltyIsCountedOnce() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);

    subject.add(stopArrival(ALIGHT_TIME), penalisedEgress(EGRESS_DURATION, TIME_PENALTY));
    subject.listPaths();

    assertEquals(
      ALIGHT_TIME + EGRESS_DURATION + TIME_PENALTY,
      mappedArrivals.getFirst().arrivalTime()
    );
  }

  @Test
  void forwardPenalisedEgressWinsWhenFasterThanPlainEgressIncludingThePenalty() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);
    int arrivalWithPenalty = EGRESS_DURATION + TIME_PENALTY;

    // The plain egress is slower than the penalised egress including the penalty
    subject.add(stopArrival(ALIGHT_TIME), TestAccessEgress.walk(STOP, arrivalWithPenalty + 40));
    subject.add(stopArrival(ALIGHT_TIME), penalisedEgress(EGRESS_DURATION, TIME_PENALTY));
    var paths = subject.listPaths();

    assertEquals(1, paths.size());
    assertEquals(ALIGHT_TIME + arrivalWithPenalty, mappedArrivals.getFirst().arrivalTime());
    assertTrue(mappedArrivals.getFirst().egressPath().egress().hasTimePenalty());
  }

  @Test
  void reversePenalisedEgressLosesWhenSlowerThanPlainEgressIncludingThePenalty() {
    var subject = createMc(USE_DEPARTURE_TIME, RaptorTransitCalculator.testDummyCalculator(false));
    int durationWithPenalty = EGRESS_DURATION + TIME_PENALTY;

    // In a reverse search the egress is the forward access. A shorter total duration is better.
    subject.add(stopArrival(ALIGHT_TIME), penalisedEgress(EGRESS_DURATION, TIME_PENALTY));
    subject.add(stopArrival(ALIGHT_TIME), TestAccessEgress.walk(STOP, durationWithPenalty - 20));
    var paths = subject.listPaths();

    assertEquals(1, paths.size());
    assertEquals(ALIGHT_TIME - durationWithPenalty + 20, mappedArrivals.getFirst().arrivalTime());
    assertFalse(mappedArrivals.getFirst().egressPath().egress().hasTimePenalty());
  }

  /**
   * Two journeys with the same arrival time and duration, found in different iterations. The
   * departure time of the journey is the time-shifted departure, not the iteration departure time.
   */
  @Test
  void departureTimeIsTimeShiftedWhenTheCalculatorIsSet() {
    int boardSlack = 60;
    int accessDuration = 72;
    int boardTime = ITERATION_DEPARTURE_TIME + 900;
    var trip = TestTripSchedule.schedule()
      .pattern("L1", STOP, STOP + 1)
      .times(boardTime, boardTime + 600)
      .build();
    var slack = new TestSlack(boardSlack);
    var subject = createMc(
      USE_ARRIVAL_TIME,
      USE_C1,
      calculator(NO_TIME_LIMIT),
      new TimeShiftedDepartureTime<>(slack)
    );
    var access = FACTORY.createAccessStopArrival(
      ITERATION_DEPARTURE_TIME,
      TestAccessEgress.walk(STOP, accessDuration)
    );
    var transit = FACTORY.createTransitStopArrival(
      new PatternRideC1<>(access, 0, boardTime, ANY, ANY, ANY, trip),
      STOP,
      trip.arrival(1),
      access.c1() + 1000
    );

    subject.add(transit, egress());
    subject.listPaths();

    var arrival = mappedArrivals.getFirst();
    assertEquals(boardTime - boardSlack - accessDuration, arrival.departureTime());
    assertEquals(ITERATION_DEPARTURE_TIME, arrival.iterationDepartureTime());
  }

  @Test
  void forwardDepartureTimeModePrefersTheLaterIterationEvenIfItArrivesLater() {
    var subject = createMc(USE_DEPARTURE_TIME, NO_TIME_LIMIT);

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME + 600), egress());
    // The earlier iteration arrives earlier, but departs earlier: not better in this mode
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);
    subject.add(stopArrival(ALIGHT_TIME), egress());

    var paths = subject.listPaths();
    assertEquals(1, paths.size());
    assertEquals(ITERATION_DEPARTURE_TIME, mappedArrivals.getFirst().iterationDepartureTime());
  }

  @Test
  void forwardArrivalTimeModeWithCostDropsALaterJourneyWithTheSameDuration() {
    // Without a time-shifted departure calculator the iteration departure time is used as
    // departure time. The second journey departs and arrives one minute later: same duration
    var subject = createMc(USE_ARRIVAL_TIME, USE_C1, calculator(NO_TIME_LIMIT));

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME + 60), egress());
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);
    subject.add(stopArrival(ALIGHT_TIME), egress());

    // The earlier iteration arrives earlier, but the duration is the same
    assertEquals(1, subject.listPaths().size());
    assertEquals(ALIGHT_TIME + EGRESS_DURATION, mappedArrivals.getFirst().arrivalTime());
  }

  @Test
  void timetableKeepsALaterJourneyWithTheSameDuration() {
    var subject = createMc(USE_TIMETABLE, USE_C1, calculator(NO_TIME_LIMIT));

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME + 60), egress());
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);
    subject.add(stopArrival(ALIGHT_TIME), egress());

    assertEquals(2, subject.listPaths().size());
  }

  @Test
  void forwardArrivalTimeModeWithoutCostIgnoresTheDepartureTime() {
    var subject = createMc(USE_ARRIVAL_TIME, NO_TIME_LIMIT);

    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    subject.add(stopArrival(ALIGHT_TIME + 600), egress());
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME - 60);
    subject.add(stopArrival(ALIGHT_TIME), egress());

    var paths = subject.listPaths();
    assertEquals(1, paths.size());
    assertEquals(ITERATION_DEPARTURE_TIME - 60, mappedArrivals.getFirst().iterationDepartureTime());
  }

  /* private helpers */

  private static RaptorAccessEgress penalisedEgress(int duration, int timePenalty) {
    return new EgressWithPenalty(
      TestAccessEgress.walk(STOP, duration).withTimePenalty(timePenalty)
    );
  }

  private DestinationArrivalPaths<RaptorTripSchedule> createStd(
    ParetoSetTime time,
    int latestArrivalTime
  ) {
    return init(
      DestinationArrivalPaths.forStdRaptor(
        paretoComparator(time, SearchDirection.FORWARD, NONE, RelaxFunction.NORMAL, null),
        calculator(latestArrivalTime),
        null,
        null,
        NO_SLACK,
        this::mapToPath,
        debugHandlerFactory(),
        RaptorTestConstants::stopIndexToName,
        subscriptions
      )
    );
  }

  private DestinationArrivalPaths<RaptorTripSchedule> createMc(
    ParetoSetTime time,
    int latestArrivalTime
  ) {
    return createMc(time, calculator(latestArrivalTime));
  }

  private DestinationArrivalPaths<RaptorTripSchedule> createMc(
    ParetoSetTime time,
    RaptorTransitCalculator<RaptorTripSchedule> calculator
  ) {
    return createMc(time, NONE, calculator);
  }

  private DestinationArrivalPaths<RaptorTripSchedule> createMc(
    ParetoSetTime time,
    ParetoSetCost cost,
    RaptorTransitCalculator<RaptorTripSchedule> calculator
  ) {
    return createMc(time, cost, calculator, null);
  }

  private DestinationArrivalPaths<RaptorTripSchedule> createMc(
    ParetoSetTime time,
    ParetoSetCost cost,
    RaptorTransitCalculator<RaptorTripSchedule> calculator,
    TimeShiftedDepartureTime<RaptorTripSchedule> departureTimeCalculator
  ) {
    return init(
      DestinationArrivalPaths.forMcRaptor(
        paretoComparator(
          time,
          calculator.searchForward() ? SearchDirection.FORWARD : SearchDirection.REVERSE,
          cost,
          RelaxFunction.NORMAL,
          null
        ),
        calculator,
        null,
        departureTimeCalculator,
        NO_SLACK,
        this::mapToPath,
        debugHandlerFactory(),
        RaptorTestConstants::stopIndexToName,
        subscriptions
      )
    );
  }

  /** The life-cycle publisher must be created after all subscribers are registered. */
  private DestinationArrivalPaths<RaptorTripSchedule> init(
    DestinationArrivalPaths<RaptorTripSchedule> subject
  ) {
    lifeCycle = new LifeCycleEventPublisher(subscriptions);
    lifeCycle.setupIteration(ITERATION_DEPARTURE_TIME);
    return subject;
  }

  private DebugHandlerFactory<RaptorTripSchedule> debugHandlerFactory() {
    var noDebug = new DebugRequest(List.of(), List.of(), 0, null, null, null, DebugLogger.noop());
    return new DebugHandlerFactory<>(noDebug, subscriptions);
  }

  private static ForwardRaptorTransitCalculator<RaptorTripSchedule> calculator(
    int latestArrivalTime
  ) {
    return new ForwardRaptorTransitCalculator<>(
      ITERATION_DEPARTURE_TIME,
      3600,
      latestArrivalTime,
      60
    );
  }

  private RaptorPath<RaptorTripSchedule> mapToPath(DestinationArrival<RaptorTripSchedule> a) {
    mappedArrivals.add(a);
    return new TestRaptorPath(
      a.iterationDepartureTime(),
      a.iterationDepartureTime(),
      a.arrivalTime(),
      a.numberOfTransfers(),
      a.c1(),
      a.c2()
    );
  }

  private static TestAccessEgress egress() {
    return TestAccessEgress.walk(STOP, EGRESS_DURATION);
  }

  /** Create a transit stop arrival (round 1) - no transfers, when combined with an egress. */
  private static ArrivalView<RaptorTripSchedule> stopArrival(int alightTime) {
    var access = FACTORY.createAccessStopArrival(
      ITERATION_DEPARTURE_TIME,
      TestAccessEgress.walk(STOP, 72)
    );
    return FACTORY.createTransitStopArrival(
      new PatternRideC1<>(access, ANY, ANY, ANY, ANY, ANY, null),
      STOP,
      alightTime,
      access.c1() + 1000
    );
  }
}
