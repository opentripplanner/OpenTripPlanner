package org.opentripplanner.routing.algorithm.raptor.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.request.PatternGuaranteedTransferProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorGuaranteedTransferProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TripPatternWithRaptorStopIndexes {
    private final TripPattern pattern;

    private final int[] stopIndexes;

    /**
     * List of transfers FROM this pattern for each stop position in pattern
     */
    private final TIntObjectMap<List<Transfer>> guaranteedTransfersFrom = new TIntObjectHashMap<>();

    /**
     * List of transfers TTO this pattern for each stop position in pattern
     */
    private final TIntObjectMap<List<Transfer>> guaranteedTransfersTo = new TIntObjectHashMap<>();


    public TripPatternWithRaptorStopIndexes(
        int[] stopIndexes,
        TripPattern pattern
    ) {
        this.stopIndexes = stopIndexes;
        this.pattern = pattern;
    }

    public FeedScopedId getId() { return pattern.getId(); }

    public TransitMode getTransitMode() {
        return pattern.getMode();
    }

    public int[] getStopIndexes() {
        return stopIndexes;
    }

    public final TripPattern getPattern() {
        return this.pattern;
    }

    /**
     * See {@link RaptorTripPattern#stopIndex(int)}
     */
    public int stopIndex(int stopPositionInPattern) {
        return stopIndexes[stopPositionInPattern];
    }

    public RaptorGuaranteedTransferProvider<TripSchedule> getGuaranteedTransfersTo() {
        return new PatternGuaranteedTransferProvider(true, guaranteedTransfersTo);
    }

    public RaptorGuaranteedTransferProvider<TripSchedule> getGuaranteedTransfersFrom() {
        return new PatternGuaranteedTransferProvider(false, guaranteedTransfersFrom);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        TripPatternWithRaptorStopIndexes that = (TripPatternWithRaptorStopIndexes) o;
        return getId() == that.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "TripPattern{" +
                "id=" + getId() +
                ", transitMode=" + pattern.getMode() +
                '}';
    }

    /** These are public to allow the mappers to inject transfers */
    public void addGuaranteedTransferFrom(Transfer tx) {
        add(guaranteedTransfersFrom, tx, tx.getFrom().getStopPosition());
    }

    /** These are public to allow the mappers to inject transfers */
    public void addGuaranteedTransfersTo(Transfer tx) {
        add(guaranteedTransfersTo, tx, tx.getTo().getStopPosition());
    }

    private static <T> void add(TIntObjectMap<List<T>> index, T e, int pos) {
        var list = index.get(pos);
        if(list == null) {
            list = new ArrayList<>();
            index.put(pos, list);
        }
        list.add(e);
    }
}
