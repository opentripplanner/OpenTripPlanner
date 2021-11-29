package org.opentripplanner.transit.raptor._data.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

public class TestConstrainedBoardingSearch
        implements RaptorConstrainedTripScheduleBoardingSearch<TestTripSchedule> {

    private static final TransferConstraint GUARANTEED = TransferConstraint.create().guaranteed().build();


    /** Index of guaranteed transfers by fromStopPos */
    private final TIntObjectMap<List<TestConstrainedTransferBoarding>> transfersByFromStopPos = new TIntObjectHashMap<>();

    private int currentTargetStopPos;

    @Override
    public boolean transferExist(int targetStopPos) {
        this.currentTargetStopPos = targetStopPos;
        return transfersByFromStopPos.containsKey(targetStopPos);
    }

    @Override
    @Nullable
    public RaptorTripScheduleBoardOrAlightEvent<TestTripSchedule> find(
            RaptorTimeTable<TestTripSchedule> timeTable,
            TestTripSchedule sourceTrip,
            int sourceStopIndex,
            int sourceArrivalTime
    ) {
        var list = transfersByFromStopPos.get(currentTargetStopPos);
        for (TestConstrainedTransferBoarding tx : list) {
            var trip = tx.getSourceTrip();
            if(trip == sourceTrip) {
                int stopPos = trip.findDepartureStopPosition(sourceArrivalTime, sourceStopIndex);
                if(tx.getSourceStopPos() == stopPos) {
                    return tx;
                }
            }
        }
        return null;
    }

    /**
     * Return boardings as a result for constrained transfers like a guaranteed transfer.
     */
    public List<TestConstrainedTransferBoarding> constrainedBoardings() {
        return transfersByFromStopPos.valueCollection()
                .stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * The the {@code source/target} is the trips in order of the search direction (forward or
     * reverse). For reverse search it is the opposite from {@code from/to} in the result path.
     */
    void addGuaranteedTransfers(
            TestTripSchedule sourceTrip,
            int sourceStopPos,
            TestTripSchedule targetTrip,
            int targetTripIndex,
            int targetStopPos,
            int targetTime
    ) {
        List<TestConstrainedTransferBoarding> list = transfersByFromStopPos.get(targetStopPos);
        if(list == null) {
            list = new ArrayList<>();
            transfersByFromStopPos.put(targetStopPos, list);
        }
        list.add(new TestConstrainedTransferBoarding(
                GUARANTEED,
                sourceTrip, sourceStopPos,
                targetTrip, targetTripIndex, targetStopPos, targetTime
        ));
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TestConstrainedBoardingSearch.class)
                .addNum("currentTargetStopPos", currentTargetStopPos)
                .addObj("index", transfersByFromStopPos)
                .toString();
    }

}
