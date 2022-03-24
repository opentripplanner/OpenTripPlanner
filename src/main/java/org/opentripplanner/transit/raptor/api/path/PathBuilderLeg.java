package org.opentripplanner.transit.raptor.api.path;

import static org.opentripplanner.transit.raptor.api.transit.CostCalculator.ZERO_COST;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.BoardAndAlightTime;
import org.opentripplanner.transit.raptor.util.PathStringBuilder;

/**
 * This is the leg implementation for the {@link PathBuilder}. It Private inner class which help cashing and calculating values before constructing a path.
 */
public class PathBuilderLeg<T extends RaptorTripSchedule> {
    private static final int NOT_SET = -999_999_999;

    /** Immutable data for the current leg. */
    private MyLeg leg;

    private int fromTime = NOT_SET;
    private int toTime = NOT_SET;

    private PathBuilderLeg<T> prev = null;
    private PathBuilderLeg<T> next = null;

    /**
     * Copy-constructor - do a deep copy with the exception of immutable types.
     * Always start with the desired head. The constructor will recursively copy the entire
     * path (all legs until {@code next} do not exist).
     */
    private PathBuilderLeg(PathBuilderLeg<T> other) {
        // Immutable fields
        this.fromTime = other.fromTime;
        this.toTime = other.toTime;
        this.leg = other.leg;

        // Mutable fields
        if(other.next != null) {
            this.next = new PathBuilderLeg<>(other.next);
            this.next.prev = this;
        }
    }

    private PathBuilderLeg(MyLeg leg) {
        this.leg = leg;
        if(leg.isTransit()) {
            @SuppressWarnings("unchecked") var transit = (MyTransitLeg<T>)leg;
            this.fromTime = transit.fromTime();
            this.toTime = transit.toTime();
        }
    }


    /* factory methods */

    static <T extends RaptorTripSchedule> PathBuilderLeg<T> accessLeg(RaptorTransfer access) {
        return new PathBuilderLeg<>(new MyAccessLeg(access));
    }

    /**
     * Create a transfer leg. The given {@code toStop} is the stop you arrive on when
     * traveling in the direction from origin to destination. In a forward search the
     * given {@code transfer.stop()} and the {@code toStop} is the same, but in a reverse
     * search, the {@code transfer.stop()} is the fromStop; hence the {@code toStop} must be
     * passed in.
     */
    static <T extends RaptorTripSchedule> PathBuilderLeg<T> transferLeg(
            RaptorTransfer transfer, int toStop
    ) {
        return new PathBuilderLeg<>(new MyTransferLeg(transfer, toStop));
    }

    static <T extends RaptorTripSchedule> PathBuilderLeg<T> transitLeg(
            T trip, BoardAndAlightTime boardAndAlightTime
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

    static <T extends RaptorTripSchedule> PathBuilderLeg<T> egress(RaptorTransfer egress) {
      return new PathBuilderLeg<>(new MyEgressLeg(egress));
    }

    /* accessors */

    public int fromTime() { return fromTime; }

    public int fromStop() { return prev.toStop(); }

    public int fromStopPos() {
        return asTransitLeg().fromStopPos();
    }

    public int toTime() { return toTime; }

    public int toStop() { return leg.toStop(); }

    public int toStopPos() {
        return asTransitLeg().toStopPos();
    }

    private void setTime(int fromTime, int toTime) {
        this.fromTime = fromTime;
        this.toTime = toTime;
    }

    public int durationInSec() { return toTime - fromTime; }

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

    public boolean isAccess() { return leg.isAccess(); }

    private boolean isAccessWithoutRides() { return isAccess() && hasNoRides(); }

    /** This leg is a transit leg or access/transfer/egress leg with rides (FLEX) */
    public boolean hasRides() {
        return isTransit() || ((MyStreetLeg)leg).streetPath.hasRides();
    }

    /** This leg is an access/transfer/egress leg without any rides. */
    public boolean hasNoRides() {
        return !hasRides();
    }

    public boolean isTransit() { return leg.isTransit(); }

    public boolean isTransfer() { return leg.isTransfer(); }

    public boolean isEgress() { return leg.isEgress(); }

    private MyAccessLeg asAccessLeg() { return (MyAccessLeg) leg; }

    private MyTransferLeg asTransferLeg() { return (MyTransferLeg) leg; }

    private MyTransitLeg<T> asTransitLeg() {
        //noinspection unchecked
        return (MyTransitLeg<T>) leg;
    }

    private MyEgressLeg asEgressLeg() { return (MyEgressLeg) leg; }

    public T trip() { return asTransitLeg().trip; }

    PathBuilderLeg<T> prev() { return prev; }

    void setPrev(PathBuilderLeg<T> prev) { this.prev = prev; }

    public PathBuilderLeg<T> next() { return next; }

    void setNext(PathBuilderLeg<T> next) {
        this.next = next;
    }

    @Nullable
    PathBuilderLeg<T> prevTransitLeg() {
        var it = prev();
        if (it == null) { return null; }
        if (it.isTransfer()) { it = it.prev(); }
        if (it == null) { return null; }
        // Check transfer, it can be a FLEX access
        return it.isTransit() ? it : null;
    }

    @Nullable
    public PathBuilderLeg<T> nextTransitLeg() {  return next(PathBuilderLeg::isTransit); }

    @Nullable
    PathBuilderLeg<T> next(Predicate<PathBuilderLeg<T>> test) {
        var it = next();
        while (it != null && !test.test(it)) {
            it = it.next();
        }
        return it;
    }

    @Override
    public String toString() {
        return leg.toString();
    }

    public void toString(PathStringBuilder builder) {
        leg.addToString(builder);
    }


    /* Build helper methods, package local */

    PathBuilderLeg<T> mutate() {
        return new PathBuilderLeg<>(this);
    }

    /**
     * Access, transfers and egress legs must be time-shifted towards the appropriate
     * transit leg. The access is moved to arrive just in time to board the next transit, and
     * transfers and egress are moved in time to start when the previous transit arrive, maximising
     * the wait time after the transfer. Slacks are respected.
     * <p>
     * This method operate on the current leg for access legs and the NEXT leg for transfer and
     * egress. So, if the NEXT leg is a transit or egress leg it is time-shifted. This make it safe
     * to call this method on any leg - just make sure the legs are linked first.
     */
    public void timeShiftThisAndNextLeg(RaptorSlackProvider slackProvider) {
        if(isAccess()) { timeShiftAccessTime(slackProvider); }
        if(next != null) {
            if (next.isTransfer()) { next.timeShiftTransferTime(slackProvider); }
            else if (next.isEgress()) { next.timeShiftEgressTime(slackProvider); }
        }
    }

    /**
     * Calculate the cost of this leg including wait time [if previous is set].
     * <p>
     * This method is safe to use event as long as the next leg is set.
     */
    public int generalizedCost(CostCalculator costCalculator, RaptorSlackProvider slackProvider) {
        if(costCalculator == null) { return ZERO_COST; }
        if(isAccess()) { return asAccessLeg().streetPath.generalizedCost(); }
        if(isTransfer()) { return asTransferLeg().streetPath.generalizedCost(); }
        if(isTransit()) { return transitCost(costCalculator, slackProvider); }
        if(isEgress()) { return egressCost(costCalculator, slackProvider); }
        throw new IllegalStateException("Unknown leg type: " + this);
    }

    public void changeBoardingPosition(int stopPosition) {
        if(stopPosition == fromStopPos()) { return; }

        var old = asTransitLeg();
        var boardAndAlightTime = new BoardAndAlightTime(old.trip, stopPosition, old.boardAndAlightTime.alightStopPos());

        this.leg = new MyTransitLeg<>(old.trip, boardAndAlightTime, old.constrainedTransferAfterLeg);
        this.fromTime = boardAndAlightTime.boardTime();
    }


    /* Factory methods, create new path */

    AccessPathLeg<T> createAccessPathLeg(
            CostCalculator costCalculator, RaptorSlackProvider slackProvider
    ) {
        PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
        var accessPath = asAccessLeg().streetPath;
        int cost = cost(costCalculator, accessPath);
        return new AccessPathLeg<>(accessPath, fromTime, toTime, cost, nextLeg);
    }

    private PathLeg<T> createPathLeg(CostCalculator costCalculator, RaptorSlackProvider slackProvider) {
        if(isAccess()) { return createAccessPathLeg(costCalculator, slackProvider); }
        if(isTransit()) { return createTransitPathLeg(costCalculator, slackProvider); }
        if(isTransfer()) { return createTransferPathLeg(costCalculator, slackProvider); }
        if(isEgress()) { return createEgressPathLeg(costCalculator, slackProvider); }
        throw new IllegalStateException("Unknown leg type: " + this);
    }

    private TransferPathLeg<T> createTransferPathLeg(
            CostCalculator costCalculator, RaptorSlackProvider slackProvider
    ) {
        PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
        var streetPath = asTransferLeg().streetPath;
        int cost = cost(costCalculator, streetPath);
        return new TransferPathLeg<>(fromStop(), fromTime, toTime, cost, streetPath, nextLeg);
    }

    private TransitPathLeg<T> createTransitPathLeg(
            CostCalculator costCalculator,
            RaptorSlackProvider slackProvider
    ) {
        PathLeg<T> nextLeg = next.createPathLeg(costCalculator, slackProvider);
        var leg = asTransitLeg();
        int cost = transitCost(costCalculator, slackProvider);
        return new TransitPathLeg<>(
                leg.trip, leg.boardAndAlightTime, leg.constrainedTransferAfterLeg, cost, nextLeg
        );
    }

    private EgressPathLeg<T> createEgressPathLeg(
            CostCalculator costCalculator,
            RaptorSlackProvider slackProvider
    ) {
        int cost = egressCost(costCalculator, slackProvider);
        return new EgressPathLeg<>(asEgressLeg().streetPath, fromTime, toTime, cost);
    }


    /* private methods */

    /**
     * Find the stop arrival time for this leg, this include alight-slack for transit legs.
     */
    private int stopArrivalTime(RaptorSlackProvider slackProvider) {
        if(!isTransit()) { return toTime; }
        return toTime + slackProvider.alightSlack(asTransitLeg().trip.pattern());
    }

    /**
     * The wait-time between this TRANSIT leg and the next transit leg including any slack.
     * If there is a transfer leg between the two transit legs, the transfer time is excluded from
     * the wait time.
     * <p>
     * {@code -1} is returned:
     * <ul>
     *     <li>if this leg is not a transit leg</li>
     *     <li>no transit leg exist after this leg</li>
     * <ul>
     */
    public int waitTimeBeforeNextTransitIncludingSlack() {
        if(next.hasRides()) { return waitTimeBeforeNextLegIncludingSlack(); }

        // Add wait-time before and after transfer(the next leg)
        return waitTimeBeforeNextLegIncludingSlack() + next.waitTimeBeforeNextLegIncludingSlack();
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
        int slack = slackProvider.boardSlack(leg.trip.pattern()) +
                (withTransferSlack ? slackProvider.transferSlack() : 0);

        return leg.fromTime() - slack;
    }

    /**
     * We need to calculate the access-arrival-time. There are 3 cases:
     * <ol>
     *     <li>Normal case: Walk ~ boardSlack ~ transit (access can be time-shifted)</li>
     *     <li>Flex and transit: Flex ~ (transferSlack + boardSlack) ~ transit</li>
     *     <li>Flex, walk and transit: Flex ~ Walk ~ (transferSlack + boardSlack) ~ transit</li>
     * </ol>
     * Flex access may or may not be time-shifted.
     */
    private void timeShiftAccessTime(RaptorSlackProvider slackProvider) {

        var accessPath = asAccessLeg().streetPath;
        var nextTransitLeg = nextTransitLeg();

        @SuppressWarnings("ConstantConditions")
        int newToTime = nextTransitLeg.transitStopArrivalTimeBefore(slackProvider, hasRides());

        if(next.isTransfer()) {
            newToTime -= next.asTransferLeg().streetPath.durationInSeconds();
        }
        newToTime = accessPath.latestArrivalTime(newToTime);

        setTime(newToTime - accessPath.durationInSeconds(), newToTime);
    }

    private void timeShiftTransferTime(RaptorSlackProvider slackProvider) {
        int newFromTime;
        if(prev.isTransit()) {
            newFromTime = prev.toTime() + slackProvider.alightSlack(prev.asTransitLeg().trip.pattern());
        }
        else if(prev.isAccess()) {
            newFromTime = prev.toTime();
        }
        else {
            throw new IllegalStateException("Unexpected leg type before TransferLeg: " + this);
        }
        setTime(newFromTime, newFromTime + asTransferLeg().streetPath.durationInSeconds());
    }

    private void timeShiftEgressTime(RaptorSlackProvider slackProvider) {
        var egressPath = asEgressLeg().streetPath;
        int newFromTime = prev.stopArrivalTime(slackProvider);

        if (egressPath.hasRides()) {
            newFromTime += slackProvider.transferSlack();
        }
        newFromTime = egressPath.earliestDepartureTime(newFromTime);

        setTime(newFromTime, newFromTime + egressPath.durationInSeconds());
    }

    private int transitCost(CostCalculator costCalculator, RaptorSlackProvider slackProvider) {
        if(costCalculator == null) { return ZERO_COST; }

        var leg = asTransitLeg();

        // Need to check prev, since this method should return a value for partially constructed
        // paths (a tail of a path only).
        int prevStopArrivalTime;

        // If path is not fully constructed yet, then prev leg might be null.
        if(prev == null) {
            prevStopArrivalTime = fromTime;
        }
        else {
            prevStopArrivalTime = prev.stopArrivalTime(slackProvider);
        }

        var prevTransit = prevTransitLeg();
        var txBeforeLeg = prevTransit == null
                ? null
                : prevTransit.constrainedTransferAfterLeg();
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
                slackProvider.alightSlack(leg.trip.pattern()),
                durationInSec(),
                leg.trip.transitReluctanceFactorIndex(),
                toStop()
        );
    }

    private int egressCost(CostCalculator costCalculator, RaptorSlackProvider slackProvider) {
        if(costCalculator == null) { return ZERO_COST; }

        var egressPath = asEgressLeg().streetPath;

        final int egressCost = costCalculator.costEgress(egressPath);

        if(prev == null) { return egressCost; }

        final int waitCost = costCalculator.waitCost(waitTimeAfterPrevStopArrival(slackProvider));

        return waitCost + egressCost;
    }

    private static int cost(CostCalculator costCalculator, RaptorTransfer streetPath) {
        return costCalculator != null ? streetPath.generalizedCost() : ZERO_COST;
    }

    /* PRIVATE INTERFACES */


    /** This is the common interface for all immutable leg structures. */
    private interface MyLeg {
        default boolean isAccess() { return false; }
        default boolean isTransit() { return false; }
        default boolean isTransfer() { return false; }
        default boolean isEgress() { return false; }
        int toStop();
        PathStringBuilder addToString(PathStringBuilder builder);
    }

    /* PRIVATE CLASSES */


    /** Abstract access/transfer/egress leg */
    private static abstract class MyStreetLeg implements MyLeg {
        final RaptorTransfer streetPath;
        MyStreetLeg(RaptorTransfer streetPath) { this.streetPath = streetPath; }
        final @Override public String toString() {
            return addToString(new PathStringBuilder(null)).toString();
        }
    }

    private static class MyAccessLeg extends MyStreetLeg {
        private MyAccessLeg(RaptorTransfer streetPath) {
            super(streetPath);
        }
        @Override public boolean isAccess() { return true; }
        @Override public int toStop() { return streetPath.stop(); }
        @Override public PathStringBuilder addToString(PathStringBuilder builder) {
            return builder.accessEgress(streetPath).sep().stop(toStop()).sep();
        }
    }

    private static class MyTransferLeg extends MyStreetLeg {
        final int toStop;
        MyTransferLeg(RaptorTransfer streetPath, int toStop) {
            super(streetPath);
            this.toStop = toStop;
        }
        @Override public boolean isTransfer() { return true; }
        @Override public int toStop() { return toStop; }
        @Override public PathStringBuilder addToString(PathStringBuilder builder) {
            return builder.walk(streetPath.durationInSeconds()).sep().stop(toStop()).sep();
        }
    }

    private static class MyTransitLeg<T extends RaptorTripSchedule> implements MyLeg {
        final T trip;
        final BoardAndAlightTime boardAndAlightTime;

        /**
         * Transfer constraints are attached to the transit leg BEFORE the transit, this
         * simplifies the code a bit. We do not always have transfers (same stop) between
         * to transits, so it is can not be attached to the transfer. Also, attaching it to the
         * transit leg BEFORE make the transit leg after more reusable when mutating the builder.
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
        @Override public boolean isTransit() { return true; }
        public int fromStop() { return trip.pattern().stopIndex(boardAndAlightTime.boardStopPos()); }
        public int fromStopPos() { return boardAndAlightTime.boardStopPos(); }
        public int fromTime() { return boardAndAlightTime.boardTime(); }
        @Override public int toStop() { return trip.pattern().stopIndex(boardAndAlightTime.alightStopPos()); }
        public int toStopPos() { return boardAndAlightTime.alightStopPos(); }
        public int toTime() { return boardAndAlightTime.alightTime(); }
        final @Override public String toString() {
            return addToString(new PathStringBuilder(null)).toString();
        }
        @Override public PathStringBuilder addToString(PathStringBuilder builder) {
            return builder.transit(trip.pattern().debugInfo(), fromTime(), toTime()).sep()
                    .stop(toStop()).sep();
        }
    }

    private static class MyEgressLeg extends MyStreetLeg {
        MyEgressLeg(RaptorTransfer streetPath) {
            super(streetPath);
        }
        @Override public boolean isEgress() { return true; }
        @Override public int toStop() {
            throw new IllegalStateException("Egress leg have no toStop");
        }
        @Override public PathStringBuilder addToString(PathStringBuilder builder) {
            return builder.accessEgress(streetPath);
        }
    }
}