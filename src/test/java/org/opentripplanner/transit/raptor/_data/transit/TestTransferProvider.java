package org.opentripplanner.transit.raptor._data.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorGuaranteedTransferProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleBoardOrAlightEvent;

public class TestTransferProvider implements RaptorGuaranteedTransferProvider<TestTripSchedule> {

    /** Index of guaranteed transfers by fromStopPos */
    private final TIntObjectMap<List<GuaranteedTransfer>> transfersByFromStopPos = new TIntObjectHashMap<>();

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
        for (GuaranteedTransfer tx : list) {
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

    void addGuaranteedTransfers(
            TestTripSchedule sourceTrip,
            int sourceStopPos,
            TestTripSchedule targetTrip,
            int targetTripIndex,
            int targetStopPos,
            int targetTime
    ) {
        List<GuaranteedTransfer> list = transfersByFromStopPos.get(targetStopPos);
        if(list == null) {
            list = new ArrayList<>();
            transfersByFromStopPos.put(targetStopPos, list);
        }
        list.add(new GuaranteedTransfer(
                sourceTrip, sourceStopPos,
                targetTrip, targetTripIndex, targetStopPos, targetTime
        ));
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TestTransferProvider.class)
                .addNum("currentTargetStopPos", currentTargetStopPos)
                .addObj("index", transfersByFromStopPos)
                .toString();
    }

}
