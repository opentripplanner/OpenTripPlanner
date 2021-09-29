package org.opentripplanner.routing.algorithm.raptor.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.algorithm.raptor.transit.request.ConstrainedBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TripPatternWithRaptorStopIndexes {
    private final TripPattern pattern;

    private final int[] stopIndexes;

    /**
     * List of transfers TO this pattern for each stop position in pattern used by Raptor during
     * the FORWARD search.
     */
    private final TIntObjectMap<List<ConstrainedTransfer>> constrainedTransfersForwardSearch =
            new TIntObjectHashMap<>();

    /**
     * List of transfers FROM this pattern for each stop position in pattern used by Raptor during
     * the REVERSE search.
     */
    private final TIntObjectMap<List<ConstrainedTransfer>> constrainedTransfersReverseSearch =
            new TIntObjectHashMap<>();


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

    public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> constrainedTransferForwardSearch() {
        return new ConstrainedBoardingSearch(true, constrainedTransfersForwardSearch);
    }

    public RaptorConstrainedTripScheduleBoardingSearch<TripSchedule> constrainedTransferReverseSearch() {
        return new ConstrainedBoardingSearch(false, constrainedTransfersReverseSearch);
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
    public void addTransferConstraintsForwardSearch(ConstrainedTransfer tx) {
        // In the Raptor search the transfer is looked up using the target
        // trip, the trip boarded after the transfer is done for a forward search.
        add(constrainedTransfersForwardSearch, tx, tx.getTo().getStopPosition());
    }

    /** These are public to allow the mappers to inject transfers */
    public void addTransferConstraintsReverseSearch(ConstrainedTransfer tx) {
        // In the Raptor search the transfer is looked up using the target
        // trip. Thus, the transfer "from trip" should be used in a reverse search.
        add(constrainedTransfersReverseSearch, tx, tx.getFrom().getStopPosition());
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
