package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.opentripplanner.transit.raptor.api.transit.CostCalculator.ZERO_COST;

import javax.annotation.Nullable;
import org.opentripplanner.model.base.ValueObjectToStringBuilder;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.routing.algorithm.transferoptimization.api.OptimizedPath;
import org.opentripplanner.routing.algorithm.transferoptimization.api.TransferOptimized;
import org.opentripplanner.transit.raptor.api.path.PathBuilder;
import org.opentripplanner.transit.raptor.api.path.PathBuilderLeg;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;
import org.opentripplanner.transit.raptor.api.transit.CostCalculator;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorStopNameResolver;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.BoardAndAlightTime;

/**
 * This class is used to decorate a {@link TransitPathLeg} with information about transfers
 * constraints, and also caches transfer-priority-cost and optimized-wait-time-transfer-cost.
 * <p>
 * The class is only used inside the {@code transferoptimization} package to store temporary
 * path "tails", while building new paths with new transfer points.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class OptimizedPathTail<T extends RaptorTripSchedule>
        extends PathBuilder<T> implements TransferOptimized
{

    @Nullable
    private final TransferWaitTimeCalculator waitTimeCostCalculator;
    private int transferPriorityCost = TransferConstraint.ZERO_COST;
    private int waitTimeOptimizedCost = TransferWaitTimeCalculator.ZERO_COST;

    public OptimizedPathTail(
            RaptorSlackProvider slackProvider,
            CostCalculator costCalculator,
            TransferWaitTimeCalculator waitTimeCostCalculator,
            RaptorStopNameResolver stopNameResolver
    ) {
        super(null, slackProvider, costCalculator, stopNameResolver);
        this.waitTimeCostCalculator = waitTimeCostCalculator;
    }

    private OptimizedPathTail(OptimizedPathTail<T> other) {
        super(other);
        this.waitTimeCostCalculator = other.waitTimeCostCalculator;
        this.waitTimeOptimizedCost = other.waitTimeOptimizedCost;
        this.transferPriorityCost = other.transferPriorityCost;
    }

    @Override
    protected void add(PathBuilderLeg<T> newLeg) {
        addHead(newLeg);
        // Keep from- and to- times up to date by time-shifting access, transfer and egress legs.
        newLeg.timeShiftThisAndNextLeg(slackProvider);
        addTransferPriorityCost(newLeg);
        addOptimizedWaitTimeCost(newLeg);
    }

    @Override
    protected void updateAggregatedFields() {
        /* Empty, aggregated fields are updated while adding new legs */
    }

    /**
     * Create a deep-copy of this builder.
     */
    public OptimizedPathTail<T> mutate() {
        return new OptimizedPathTail<>(this);
    }

    /** Start by adding the last transit leg with the egress leg attached. */
    public OptimizedPathTail<T> addTransitTail(TransitPathLeg<T> leg) {
        egress(leg.nextLeg().asEgressLeg().egress());
        var times = new BoardAndAlightTime(leg.trip(), leg.getFromStopPosition(), leg.getToStopPosition());
        transit(leg.trip(), times);
        return this;
    }

    /**
     * Insert a new transit leg at the head and return the new object. The new tail is returned
     * with the given transit + transfer leg, earliest-departure-time and the current leg as a
     * new tail.
     */
    public OptimizedPathTail<T> addTransitAndTransferLeg(
            TransitPathLeg<T> originalLeg,
            TripToTripTransfer<T> tx
    ) {
        head().changeBoardingPosition(tx.to().stopPosition());

        if(!tx.sameStop()) {
            transfer(tx.getPathTransfer(), tx.to().stop());
        }

        // The transfer may happen before the original boarding point. If so, the boarding must be
        // changed so that the leg is valid (not traveling in reverse/back in time). Also, setting
        // the boarding position to the first stop in the pattern makes sure that all paths start at the
        // same place; hence the generalized-cost can be compared.
        // The board position will be changed when a new head is inserted.
        int boardStopPos = 0;

        // Using the earliest-departure-time as input here is in some cases wrong, but the leg
        // created here is temporary and will be mutated when connected with the leg in front of it.
        addTransitLeg(
                originalLeg.trip(),
                boardStopPos,
                tx.from().stopPosition(),
                tx.constrainedTransfer()
        );
        return this;
    }

    private void addTransitLeg(
            T trip,
            int boardStopPos,
            int alightStopPos,
            @Nullable ConstrainedTransfer txConstraintsAfter
    ) {
        var times = new BoardAndAlightTime(trip, boardStopPos, alightStopPos);
        var c = txConstraintsAfter == null ? null : txConstraintsAfter.getTransferConstraint();
        transit(trip, times, txConstraintsAfter);
    }

    @Override
    public OptimizedPath<T> build(int iterationDepartureTime) {
        return new OptimizedPath<>(
                createPathLegs(costCalculator, slackProvider),
                iterationDepartureTime,
                generalizedCost(),
                transferPriorityCost,
                waitTimeOptimizedCost,
                breakTieCost()
        );
    }

    /**
     * Return the generalized cost for the current set of paths.
     */
    public int generalizedCost() {
        if(skipCostCalc()) { return ZERO_COST; }
        return legsAsStream()
                .mapToInt(it -> it.generalizedCost(costCalculator, slackProvider))
                .sum();
    }

    /**
     * The latest possible time to board. We use the first transit leg arrival time
     * as the limit, you need to board before you alight.
     */
    public int latestPossibleBoardingTime() {
        return head().toTime();
    }

    @Override
    public int waitTimeOptimizedCost() {
        return waitTimeOptimizedCost;
    }

    @Override
    public int transferPriorityCost() {
        return transferPriorityCost;
    }

    @Override
    public int breakTieCost() {
        // We add the arrival times together to mimic doing the transfers as early as possible
        // when more than one transfer point exists between two trips.
        // We calculate this on the fly, because it is not likely to be done very often and
        // the calculation is light-weight.
        return legsAsStream()
                .filter(PathBuilderLeg::isTransit)
                .mapToInt(PathBuilderLeg::toTime)
                .sum();
    }

    @Override
    public String toString() {
        return ValueObjectToStringBuilder.of()
                .addObj(super.toString())
                .addText(" [")
                .addCost(generalizedCost())
                .addCost(transferPriorityCost, "pri")
                .addCost(waitTimeOptimizedCost, "wtc")
                .addText("]")
                .toString();
    }


    /*private methods */

    private void addTransferPriorityCost(PathBuilderLeg<T> pathLeg) {
        boolean transferExist = pathLeg.isTransit() && pathLeg.nextTransitLeg() != null;
        this.transferPriorityCost += OptimizedPath.priorityCost(
                transferExist, pathLeg::constrainedTransferAfterLeg
        );
    }

    /**
     * Add cost of wait-time, if the given path leg is a transit leg and it is followed by
     * another transit leg (with a optional transfer leg in between).
     * <p>
     *   Guaranteed and stay-seated transfers have zero wait-time cost.
     * <p>
     * We could argue that we should include cost for wait-time after FLEX access,
     * and wait-time before FLEX egress. But since it can be time-shifted, it become almost
     * impossible to do a proper cost calculation for it. For example, if the FLEX ride is
     * pre-booked, then it might wait for the passenger.
     */
    private void addOptimizedWaitTimeCost(PathBuilderLeg<?> pathLeg) {
        if(waitTimeCostCalculator == null) { return; }

        int waitTime = pathLeg.waitTimeBeforeNextTransitIncludingSlack();
        if(waitTime < 0) { return; }

        var tx = pathLeg.constrainedTransferAfterLeg();

        if(tx != null) {
            var c = (TransferConstraint)tx.getTransferConstraint();
            // If the transfer is stay-seated or guaranteed, then no wait-time cost is added
            if (c != null && c.isFacilitated()) {
                if(c.isStaySeated()) {
                    this.waitTimeOptimizedCost +=
                            waitTimeCostCalculator.calculateStaySeatedTransferCost();
                }
                else if(c.isGuaranteed()) {
                    this.waitTimeOptimizedCost +=
                            waitTimeCostCalculator.calculateGuaranteedTransferCost();
                }
                return;
            }
        }

        int cost = waitTimeCostCalculator.calculateOptimizedWaitCost(waitTime);
        this.waitTimeOptimizedCost += cost;
    }
}
