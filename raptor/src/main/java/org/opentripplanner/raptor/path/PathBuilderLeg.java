package org.opentripplanner.raptor.path;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorConstrainedTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.AccessPathLeg;
import org.opentripplanner.raptor.api.path.EgressPathLeg;
import org.opentripplanner.raptor.api.path.PathLeg;
import org.opentripplanner.raptor.api.path.PathStringBuilder;
import org.opentripplanner.raptor.api.path.TransferPathLeg;
import org.opentripplanner.raptor.api.path.TransitPathLeg;
import org.opentripplanner.raptor.rangeraptor.transit.AccessWithPenalty;
import org.opentripplanner.raptor.rangeraptor.transit.EgressWithPenalty;
import org.opentripplanner.raptor.rangeraptor.transit.ForwardTransitCalculator;
import org.opentripplanner.raptor.rangeraptor.transit.TransitCalculator;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This is the leg implementation for the {@link PathBuilder}. It helps to cache and calculate
 * values before constructing a path.
 */
public class PathBuilderLeg<T extends RaptorTripSchedule> {

  private static final int NOT_SET = -999_999_999;

  /**
   * The path is not reversed, so we can use the forward transit calculator to calculate the
   * egress board-time (egress with rides).
   */
  private static final TransitCalculator<?> TRANSIT_CALCULATOR = new ForwardTransitCalculator<>();

  /** Immutable data for the current leg. */
  private MyLeg leg;

  private int fromTime = NOT_SET;
  private int toTime = NOT_SET;
  private int c2 = RaptorConstants.NOT_SET;

  private PathBuilderLeg<T> prev = null;
  private PathBuilderLeg<T> next = null;

  /**
   * Copy-constructor - do a deep copy with the exception of immutable types. Always start with the
   * desired head. The constructor will recursively copy the entire path (all legs until {@code
   * next} do not exist).
   */
  private PathBuilderLeg(PathBuilderLeg<T> other) {
    // Immutable fields
    this.fromTime = other.fromTime;
    this.toTime = other.toTime;
    this.leg = other.leg;
    this.c2 = other.c2;

    // Mutable fields
    if (other.next != null) {
      this.next = new PathBuilderLeg<>(other.next);
      this.next.prev = this;
    }
  }

  private PathBuilderLeg(MyLeg leg) {
    this.leg = leg;
    if (leg.isTransit()) {
      @SuppressWarnings("unchecked")
      var transit = (MyTransitLeg<T>) leg;
      this.fromTime = transit.fromTime();
      this.toTime = transit.toTime();
    }
  }

  /* accessors */

  public int fromTime() {
    return fromTime;
  }

  public int fromStop() {
    return prev.toStop();
  }

  public int fromStopPos() {
    return asTransitLeg().fromStopPos();
  }

  public int toTime() {
    return toTime;
  }

  public int toStop() {
    return leg.toStop();
  }

  public int toStopPos() {
    return asTransitLeg().toStopPos();
  }

  public int durationInSec() {
    return toTime - fromTime;
  }

  public int c2() {
    return c2;
  }

  public void c2(int c2) {
    this.c2 = c2;
  }

  public boolean isC2Set() {
    return c2 != RaptorConstants.NOT_SET;
  }

  @Nullable
  public RaptorConstrainedTransfer constrainedTransferAfterLeg() {
    return isTransit() ? asTransitLeg().constrainedTransferAfterLeg : null;
  }

  public void setConstrainedTransferAfterLeg(
    @Nullable RaptorConstrainedTransfer constrainedTransferAfterLeg
  ) {
    var old = asTransitLeg();
    this.leg = new MyTransitLeg<>(old.trip, old.boardAndAlightTime, constrainedTransferAfterLeg);
  }

  public boolean isAccess() {
    return leg.isAccess();
  }

  /** This leg is a transit leg or access/transfer/egress leg with rides (FLEX) */
  public boolean hasRides() {
    return leg.hasRides();
  }

  /** This leg is an access/transfer/egress leg without any rides. */
  public boolean hasNoRides() {
    return !hasRides();
  }

  public boolean isTransit() {
    return leg.isTransit();
  }

  public boolean isTransfer() {
    return leg.isTransfer();
  }

  public boolean isEgress() {
    return leg.isEgress();
  }

  public T trip() {
    return asTransitLeg().trip;
  }

  public PathBuilderLeg<T> prev() {
    return prev;
  }

  public PathBuilderLeg<T> next() {
    return next;
  }

  @Nullable
  public PathBuilderLeg<T> nextTransitLeg() {
    return next(PathBuilderLeg::isTransit);
  }

  @Override
  public String toString() {
    return leg.toString();
  }

  public void toString(PathStringBuilder builder) {
    leg.addToString(builder);
  }

  /**
   * Access, transfers and egress legs must be time-shifted towards the appropriate transit leg. The
   * access is moved to arrive just in time to board the next transit, and transfers and egress are
   * moved in time to start when the previous transit arrive, maximising the wait time after the
   * transfer. Slacks are respected.
   * <p>
   * This method operate on the current leg for access legs and the NEXT leg for transfer and
   * egress. So, if the NEXT leg is a transit or egress leg it is time-shifted. This make it safe to
   * call this method on any leg - just make sure the legs are linked first.
   */
  public void timeShiftThisAndNextLeg(
    RaptorSlackProvider slackProvider,
    int iterationDepartureTime
  ) {
    if (isAccess()) {
      timeShiftAccessTime(slackProvider, iterationDepartureTime);
    }
    if (next != null) {
      if (next.isTransfer()) {
        next.setTransferTimeBasedOnPreviousLeg(slackProvider);
        if (next.next().isEgress()) {
          next.timeShiftThisAndNextLeg(slackProvider, iterationDepartureTime);
        }
      } else if (next.isEgress()) {
        next.timeShiftEgressTime(slackProvider);
      }
    }
  }

  /**
   * Calculate the cost of this leg including wait time [if previous is set].
   * <p>
   * This method is safe to use event as long as the next leg is set.
   */
  public int c1(RaptorCostCalculator<T> costCalculator, RaptorSlackProvider slackProvider) {
    if (costCalculator == null) {
      return RaptorCostCalculator.ZERO_COST;
    }
    if (isAccess()) {
      return asAccessLeg().streetPath.c1();
    }
    if (isTransfer()) {
      return asTransferLeg().transfer.c1();
    }
    if (isTransit()) {
      return transitCost(costCalculator, slackProvider);
    }
    if (isEgress()) {
      return egressCost(costCalculator, slackProvider);
    }
    throw new IllegalStateException("Unknown leg type: " + this);
  }

  public void changeBoardingPosition(int stopPosition) {
    if (stopPosition == fromStopPos()) {
      return;
    }

    var old = asTransitLeg();
    var boardAndAlightTime = new BoardAndAlightTime(
      old.trip,
      stopPosition,
      old.boardAndAlightTime.alightStopPos()
    );

    this.leg = new MyTransitLeg<>(old.trip, boardAndAlightTime, old.constrainedTransferAfterLeg);
    this.fromTime = boardAndAlightTime.boardTime();
  }

  /**
   * The wait-time between this TRANSIT leg and the next transit leg including any slack. If there
   * is a transfer leg between the two transit legs, the transfer time is excluded from the wait
   * time.
   * <p>
   * {@code -1} is returned:
   * <ul>
   *     <li>if this leg is not a transit leg</li>
   *     <li>no transit leg exist after this leg</li>
   * <ul>
   */
  public int waitTimeBeforeNextTransitIncludingSlack() {
    if (next.hasRides()) {
      return waitTimeBeforeNextLegIncludingSlack();
    }

    // Add wait-time before and after transfer(the next leg)
    return waitTimeBeforeNextLegIncludingSlack() + next.waitTimeBeforeNextLegIncludingSlack();
  }

  static <T extends RaptorTripSchedule> PathBuilderLeg<T> accessLeg(RaptorAccessEgress access) {
    return new PathBuilderLeg<>(new MyAccessLeg(access));
  }

  /**
   * Create a transfer leg. The given {@code toStop} is the stop you arrive on when traveling in the
   * direction from origin to destination. In a forward search the given {@code transfer.stop()} and
   * the {@code toStop} is the same, but in a reverse search, the {@code transfer.stop()} is the
   * fromStop; hence the {@code toStop} must be passed in.
   */
  static <T extends RaptorTripSchedule> PathBuilderLeg<T> transferLeg(
    RaptorTransfer transfer,
    int toStop
  ) {
    return new PathBuilderLeg<>(new MyTransferLeg(transfer, toStop));
  }

  static <T extends RaptorTripSchedule> PathBuilderLeg<T> transitLeg(
    T trip,
    BoardAndAlightTime boardAndAlightTime
  ) {
    return transitLeg(trip, boardAndAlightTime, null);
  }

  static <T extends RaptorTripSchedule> PathBuilderLeg<T> transitLeg(
    T trip,
    BoardAndAlightTime boardAndAlightTime,
    @Nullable RaptorConstrainedTransfer txConstrainedAfterLeg
  ) {
    return new PathBuilderLeg<>(
      new MyTransitLeg<>(trip, boardAndAlightTime, txConstrainedAfterLeg)
    );
  }

  static <T extends RaptorTripSchedule> PathBuilderLeg<T> egress(RaptorAccessEgress egress) {
    return new PathBuilderLeg<>(new MyEgressLeg(egress));
  }

  void setPrev(PathBuilderLeg<T> prev) {
    this.prev = prev;
  }

  void setNext(PathBuilderLeg<T> next) {
    this.next = next;
  }

  @Nullable
  PathBuilderLeg<T> prevTransitLeg() {
    var it = prev();
    if (it == null) {
      return null;
    }
    if (it.isTransfer()) {
      it = it.prev();
    }
    if (it == null) {
      return null;
    }
    // Check transfer, it can be a FLEX access
    return it.isTransit() ? it : null;
  }

  @Nullable
  PathBuilderLeg<T> next(Predicate<PathBuilderLeg<T>> test) {
    var it = next();
    while (it != null && !test.test(it)) {
      it = it.next();
    }
    return it;
  }

  PathBuilderLeg<T> mutate() {
    return new PathBuilderLeg<>(this);
  }

  AccessPathLeg<T> createAccessPathLeg(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
    var accessPath = asAccessLeg().streetPath;
    int cost = cost(costCalculator, accessPath);
    return new AccessPathLeg<>(accessPath, fromTime, toTime, cost, nextLeg);
  }

  /* Build helper methods, package local */

  private static int cost(RaptorCostCalculator<?> costCalculator, RaptorAccessEgress streetPath) {
    return costCalculator != null ? streetPath.c1() : RaptorCostCalculator.ZERO_COST;
  }

  private static int cost(RaptorCostCalculator<?> costCalculator, RaptorTransfer transfer) {
    return costCalculator != null ? transfer.c1() : RaptorCostCalculator.ZERO_COST;
  }

  private void setTime(int fromTime, int toTime) {
    this.fromTime = fromTime;
    this.toTime = toTime;
  }

  private boolean isAccessWithoutRides() {
    return isAccess() && hasNoRides();
  }

  private MyAccessLeg asAccessLeg() {
    return (MyAccessLeg) leg;
  }

  /* Factory methods, create new path */

  private MyTransferLeg asTransferLeg() {
    return (MyTransferLeg) leg;
  }

  private MyTransitLeg<T> asTransitLeg() {
    //noinspection unchecked
    return (MyTransitLeg<T>) leg;
  }

  private MyEgressLeg asEgressLeg() {
    return (MyEgressLeg) leg;
  }

  private PathLeg<T> createPathLeg(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    if (isAccess()) {
      return createAccessPathLeg(costCalculator, slackProvider);
    }
    if (isTransit()) {
      return createTransitPathLeg(costCalculator, slackProvider);
    }
    if (isTransfer()) {
      return createTransferPathLeg(costCalculator, slackProvider);
    }
    if (isEgress()) {
      return createEgressPathLeg(costCalculator, slackProvider);
    }
    throw new IllegalStateException("Unknown leg type: " + this);
  }

  private TransferPathLeg<T> createTransferPathLeg(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
    var transfer = asTransferLeg().transfer;
    int cost = cost(costCalculator, transfer);
    return new TransferPathLeg<>(fromStop(), fromTime, toTime, cost, transfer, nextLeg);
  }

  /* private methods */

  private TransitPathLeg<T> createTransitPathLeg(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
    var leg = asTransitLeg();
    int cost = transitCost(costCalculator, slackProvider);
    return new TransitPathLeg<>(
      leg.trip,
      leg.boardAndAlightTime.boardTime(),
      leg.boardAndAlightTime.alightTime(),
      leg.boardAndAlightTime.boardStopPos(),
      leg.boardAndAlightTime.alightStopPos(),
      leg.constrainedTransferAfterLeg,
      cost,
      nextLeg
    );
  }

  private EgressPathLeg<T> createEgressPathLeg(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    int cost = egressCost(costCalculator, slackProvider);
    var egressPath = asEgressLeg().streetPath;
    return new EgressPathLeg<>(egressPath, fromTime, toTime, cost);
  }

  /**
   * Find the stop arrival time for this leg, this include alight-slack for transit legs.
   */
  private int stopArrivalTime(RaptorSlackProvider slackProvider) {
    if (!isTransit()) {
      return toTime;
    }
    return toTime + slackProvider.alightSlack(asTransitLeg().trip.pattern().slackIndex());
  }

  private int waitTimeAfterPrevStopArrival(RaptorSlackProvider slackProvider) {
    return fromTime - prev.stopArrivalTime(slackProvider);
  }

  private int waitTimeBeforeNextLegIncludingSlack() {
    return next.fromTime - toTime;
  }

  /**
   * Return the stop-arrival-time for the leg in front of this transit leg.
   */
  private int transitStopArrivalTimeBefore(
    RaptorSlackProvider slackProvider,
    boolean withTransferSlack
  ) {
    var leg = asTransitLeg();
    int slack =
      slackProvider.boardSlack(leg.trip.pattern().slackIndex()) +
      (withTransferSlack ? slackProvider.transferSlack() : 0);

    return leg.fromTime() - slack;
  }

  /**
   * We need to calculate the access-arrival-time. There are 3 cases:
   * <ol>
   *     <li>Normal case: Walk ~ boardSlack ~ transit (access can be time-shifted)</li>
   *     <li>Flex and transit: Flex ~ (transferSlack + boardSlack) ~ transit</li>
   *     <li>Flex, walk and transit: Flex ~ Walk ~ (transferSlack + boardSlack) ~ transit</li>
   *     <li>Flex, walk and Flex: Flex ~ Walk ~ Flex (will be timeshifted in relation to the iteration departure time)</li>
   * </ol>
   * Flex access may or may not be time-shifted.
   */
  private void timeShiftAccessTime(RaptorSlackProvider slackProvider, int iterationDepartureTime) {
    var accessPath = asAccessLeg().streetPath;
    var nextTransitLeg = nextTransitLeg();

    // if there is no next transit leg then move the access as early as possible relative to
    // the iteration-departure-time
    if (nextTransitLeg == null) {
      int fromTime = accessPath.earliestDepartureTime(iterationDepartureTime);
      assertTimeExist(
        fromTime,
        accessPath,
        "Access can not be time-shifted after iteration-departure-time."
      );
      setTime(fromTime, fromTime + accessPath.durationInSeconds());
    }
    // For transit, we time-shift the access as late as possible to fit with the transit.
    else {
      int toTime = nextTransitLeg.transitStopArrivalTimeBefore(slackProvider, hasRides());
      if (next.isTransfer()) {
        toTime -= next.asTransferLeg().transfer.durationInSeconds();
      }
      toTime = accessPath.latestArrivalTime(toTime);
      assertTimeExist(
        toTime,
        accessPath,
        "Access can not be time-shifted before first transit leg."
      );
      setTime(toTime - accessPath.durationInSeconds(), toTime);
    }
  }

  private void setTransferTimeBasedOnPreviousLeg(RaptorSlackProvider slackProvider) {
    int newFromTime;
    if (prev.isTransit()) {
      newFromTime =
        prev.toTime() + slackProvider.alightSlack(prev.asTransitLeg().trip.pattern().slackIndex());
    } else if (prev.isAccess()) {
      newFromTime = prev.toTime();
    } else {
      throw new IllegalStateException("Unexpected leg type before TransferLeg: " + this);
    }
    setTime(newFromTime, newFromTime + asTransferLeg().transfer.durationInSeconds());
  }

  private void timeShiftEgressTime(RaptorSlackProvider slackProvider) {
    var egressPath = asEgressLeg().streetPath;
    int stopArrivalTime = prev.stopArrivalTime(slackProvider);

    int egressDepartureTime = TRANSIT_CALCULATOR.calculateEgressDepartureTime(
      stopArrivalTime,
      egressPath,
      slackProvider.transferSlack()
    );

    assertTimeExist(
      egressDepartureTime,
      egressPath,
      "Transit does not arrive in time to board flex access."
    );

    setTime(egressDepartureTime, egressDepartureTime + egressPath.durationInSeconds());
  }

  private int transitCost(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    if (costCalculator == null) {
      return RaptorCostCalculator.ZERO_COST;
    }

    var leg = asTransitLeg();

    // Need to check prev, since this method should return a value for partially constructed
    // paths (a tail of a path only).
    int prevStopArrivalTime;

    // If path is not fully constructed yet, then prev leg might be null.
    if (prev == null) {
      prevStopArrivalTime = fromTime;
    } else {
      prevStopArrivalTime = prev.stopArrivalTime(slackProvider);
    }

    var prevTransit = prevTransitLeg();
    var txBeforeLeg = prevTransit == null ? null : prevTransit.constrainedTransferAfterLeg();
    var transferConstraint = txBeforeLeg == null
      ? RaptorTransferConstraint.REGULAR_TRANSFER
      : txBeforeLeg.getTransferConstraint();
    boolean firstBoarding = prev != null && prev.isAccessWithoutRides();

    int boardCost = costCalculator.boardingCost(
      firstBoarding,
      prevStopArrivalTime,
      leg.fromStop(),
      fromTime,
      trip(),
      transferConstraint
    );

    return costCalculator.transitArrivalCost(
      boardCost,
      slackProvider.alightSlack(leg.trip.pattern().slackIndex()),
      durationInSec(),
      leg.trip,
      toStop()
    );
  }

  private int egressCost(
    RaptorCostCalculator<T> costCalculator,
    RaptorSlackProvider slackProvider
  ) {
    if (costCalculator == null) {
      return RaptorCostCalculator.ZERO_COST;
    }

    var egressPath = asEgressLeg().streetPath;

    final int egressCost = costCalculator.costEgress(egressPath);

    if (prev == null) {
      return egressCost;
    }

    final int waitCost = costCalculator.waitCost(waitTimeAfterPrevStopArrival(slackProvider));

    return waitCost + egressCost;
  }

  private static IllegalStateException egressDepartureNotAvailable(
    int arrivalTime,
    RaptorAccessEgress egressPath
  ) {
    return new IllegalStateException(
      "Unable to reconstruct path. Transit does not arrive in time to board flex access." +
      " Arrived: " +
      TimeUtils.timeToStrCompact(arrivalTime) +
      " Egress: " +
      egressPath
    );
  }

  private static int assertTimeExist(int time, RaptorAccessEgress path, String details) {
    if (time == RaptorConstants.TIME_NOT_SET) {
      throw new IllegalStateException(
        String.format(
          "Unable to reconstruct path. %s. Arrival-time stop: %s, path: %s",
          details,
          TimeUtils.timeToStrCompact(time),
          path
        )
      );
    }
    return time;
  }

  /* PRIVATE INTERFACES */

  /** This is the common interface for all immutable leg structures. */
  private interface MyLeg {
    default boolean isAccess() {
      return false;
    }

    default boolean isTransit() {
      return false;
    }

    default boolean isTransfer() {
      return false;
    }

    default boolean isEgress() {
      return false;
    }

    boolean hasRides();

    int toStop();

    PathStringBuilder addToString(PathStringBuilder builder);
  }

  /* PRIVATE CLASSES */

  /** Abstract access/transfer/egress leg */

  private abstract static class AbstractMyLeg implements MyLeg {

    @Override
    public final String toString() {
      return addToString(new PathStringBuilder(null)).toString();
    }
  }

  private abstract static class MyStreetLeg extends AbstractMyLeg {

    final RaptorAccessEgress streetPath;

    MyStreetLeg(RaptorAccessEgress streetPath) {
      this.streetPath = removeTimePenaltyDecorator(streetPath);
    }

    @Override
    public boolean hasRides() {
      return streetPath.hasRides();
    }

    private static RaptorAccessEgress removeTimePenaltyDecorator(RaptorAccessEgress path) {
      if (path instanceof AccessWithPenalty awp) {
        return awp.removeDecorator();
      }
      if (path instanceof EgressWithPenalty awp) {
        return awp.removeDecorator();
      }
      return path;
    }
  }

  private static class MyAccessLeg extends MyStreetLeg {

    private MyAccessLeg(RaptorAccessEgress streetPath) {
      super(streetPath);
    }

    @Override
    public boolean isAccess() {
      return true;
    }

    @Override
    public int toStop() {
      return streetPath.stop();
    }

    @Override
    public PathStringBuilder addToString(PathStringBuilder builder) {
      return builder.accessEgress(streetPath).stop(toStop());
    }
  }

  private static class MyEgressLeg extends MyStreetLeg {

    MyEgressLeg(RaptorAccessEgress streetPath) {
      super(streetPath);
    }

    @Override
    public boolean isEgress() {
      return true;
    }

    @Override
    public int toStop() {
      throw new IllegalStateException("Egress leg have no toStop");
    }

    @Override
    public PathStringBuilder addToString(PathStringBuilder builder) {
      return builder.accessEgress(streetPath);
    }
  }

  private static class MyTransferLeg extends AbstractMyLeg {

    final int toStop;
    final RaptorTransfer transfer;

    MyTransferLeg(RaptorTransfer transfer, int toStop) {
      this.transfer = transfer;
      this.toStop = toStop;
    }

    @Override
    public boolean isTransfer() {
      return true;
    }

    @Override
    public boolean hasRides() {
      return false;
    }

    @Override
    public int toStop() {
      return toStop;
    }

    @Override
    public PathStringBuilder addToString(PathStringBuilder builder) {
      return builder.walk(transfer.durationInSeconds()).stop(toStop());
    }
  }

  private static class MyTransitLeg<T extends RaptorTripSchedule> extends AbstractMyLeg {

    final T trip;
    final BoardAndAlightTime boardAndAlightTime;

    /**
     * Transfer constraints are attached to the transit leg BEFORE the transit, this simplifies the
     * code a bit. We do not always have transfers (same stop) between to transits, so it is can not
     * be attached to the transfer. Also, attaching it to the transit leg BEFORE make the transit
     * leg after more reusable when mutating the builder.
     */
    @Nullable
    private final RaptorConstrainedTransfer constrainedTransferAfterLeg;

    private MyTransitLeg(
      T trip,
      BoardAndAlightTime boardAndAlightTime,
      RaptorConstrainedTransfer constrainedTransferAfterLeg
    ) {
      this.trip = trip;
      this.boardAndAlightTime = boardAndAlightTime;
      this.constrainedTransferAfterLeg = constrainedTransferAfterLeg;
    }

    @Override
    public boolean isTransit() {
      return true;
    }

    @Override
    public boolean hasRides() {
      return true;
    }

    @Override
    public int toStop() {
      return trip.pattern().stopIndex(boardAndAlightTime.alightStopPos());
    }

    @Override
    public PathStringBuilder addToString(PathStringBuilder builder) {
      return builder.transit(trip.pattern().debugInfo(), fromTime(), toTime()).stop(toStop());
    }

    public int fromStop() {
      return trip.pattern().stopIndex(boardAndAlightTime.boardStopPos());
    }

    public int fromStopPos() {
      return boardAndAlightTime.boardStopPos();
    }

    public int fromTime() {
      return boardAndAlightTime.boardTime();
    }

    public int toStopPos() {
      return boardAndAlightTime.alightStopPos();
    }

    public int toTime() {
      return boardAndAlightTime.alightTime();
    }
  }
}
