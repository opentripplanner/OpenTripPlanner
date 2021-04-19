package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternWithRaptorStopIndexes;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.GuaranteedTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

/**
 * A collection of all the TripSchedules active on a range of consecutive days. The outer list of tripSchedulesByDay
 * refers to days in order.
 */
public class TripPatternForDates
        implements
                RaptorRoute<TripSchedule>,
                RaptorTimeTable<TripSchedule>,
                RaptorTripPattern<TripSchedule>
{

    private final TripPatternWithRaptorStopIndexes tripPattern;

    private final TripPatternForDate[] tripPatternForDates;

    private final int[] offsets;

    private final int numberOfTripSchedules;

    private TIntObjectMap<List<GuaranteedTransfer<TripSchedule>>> transfersFrom = null;

    private TIntObjectMap<List<GuaranteedTransfer<TripSchedule>>> transfersTo = null;


    TripPatternForDates(
            TripPatternWithRaptorStopIndexes tripPattern,
            List<TripPatternForDate> tripPatternForDates,
            List<Integer> offsets
    ) {
        this.tripPattern = tripPattern;
        this.tripPatternForDates = tripPatternForDates.toArray(new TripPatternForDate[]{});
        this.offsets = offsets.stream().mapToInt(i -> i).toArray();
        this.numberOfTripSchedules = Arrays.stream(this.tripPatternForDates).mapToInt(TripPatternForDate::numberOfTripSchedules).sum();
    }

    public TripPatternWithRaptorStopIndexes getTripPattern() {
        return tripPattern;
    }

    // Implementing RaptorRoute
    @Override
    public RaptorTimeTable<TripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern<TripSchedule> pattern() {
        return this;
    }

    // Implementing RaptorTripPattern

    @Override public int stopIndex(int stopPositionInPattern) {
        return tripPattern.stopIndex(stopPositionInPattern);
    }

    @Override
    public boolean boardingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canBoard(stopPositionInPattern);
    }

    @Override
    public boolean alightingPossibleAt(int stopPositionInPattern) {
        return tripPattern.getPattern().canAlight(stopPositionInPattern);
    }

    @Override public int numberOfStopsInPattern() {
        return tripPattern.getStopIndexes().length;
    }

    @Override
    public String debugInfo() {
        return tripPattern.getTransitMode().name() + " " + tripPattern.getPattern().route.getShortName();
    }

    @Override
    @Nullable
    public Collection<GuaranteedTransfer<TripSchedule>> listGuaranteedTransfersFromPattern(
            int stopPos
    ) {
        return transfersFrom == null ? null : transfersFrom.get(stopPos);
    }

    @Override
    @Nullable
    public Collection<GuaranteedTransfer<TripSchedule>> listGuaranteedTransfersToPattern(
            int stopPos
    ) {
        return transfersTo == null ? null : transfersTo.get(stopPos);
    }

    // Implementing RaptorTimeTable

    @Override public TripSchedule getTripSchedule(int index) {
        for (int i = 0; i < tripPatternForDates.length; i++) {
            TripPatternForDate tripPatternForDate = tripPatternForDates[i];

            if (index < tripPatternForDate.numberOfTripSchedules()) {
                return new TripScheduleWithOffset(this, tripPatternForDate.getLocalDate(),
                        tripPatternForDate.getTripTimes(index), offsets[i]);
            }
            index -= tripPatternForDate.numberOfTripSchedules();
        }
        throw new IndexOutOfBoundsException("Index out of bound: " + index);
    }

    @Override public int numberOfTripSchedules() {
        return numberOfTripSchedules;
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(TripPatternForDates.class)
                .addObj("pattern", debugInfo())
                .addServiceTimeSchedule("offsets", offsets)
                .addNum("nTrips", numberOfTripSchedules)
                .toString();
    }

    void addTransferFrom(GuaranteedTransfer<TripSchedule> tx) {
        if(transfersFrom == null) { transfersFrom = new TIntObjectHashMap<>(); }
        addTransfer(tx, tx.getFromStopPos(), transfersFrom);
    }

    void addTransfersTo(GuaranteedTransfer<TripSchedule> tx) {
        if(transfersTo == null) { transfersTo = new TIntObjectHashMap<>(); }
        addTransfer(tx, tx.getToStopPos(), transfersTo);
    }

    private static void addTransfer(
            GuaranteedTransfer<TripSchedule> tx,
            int stopPos,
            TIntObjectMap<List<GuaranteedTransfer<TripSchedule>>> index
    ) {
        var transfersAtStop = index.get(stopPos);
        if(transfersAtStop == null) {
            transfersAtStop = new ArrayList<>();
            index.put(stopPos, transfersAtStop);
        }
        transfersAtStop.add(tx);
    }
}
