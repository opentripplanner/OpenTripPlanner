package org.opentripplanner.routing.algorithm.raptor.transit;

import java.util.Objects;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer.ConstrainedBoardingSearch;
import org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer.TransferForPattern;
import org.opentripplanner.routing.algorithm.raptor.transit.constrainedtransfer.TransferForPatternByStopPos;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TripPatternWithRaptorStopIndexes {

    private final TripPattern pattern;
    private final int[] stopIndexes;

    /**
     * List of transfers TO this pattern for each stop position in pattern used by Raptor during the
     * FORWARD search.
     */
    private final TransferForPatternByStopPos
            constrainedTransfersForwardSearch = new TransferForPatternByStopPos();

    /**
     * List of transfers FROM this pattern for each stop position in pattern used by Raptor during
     * the REVERSE search.
     */
    private final TransferForPatternByStopPos
            constrainedTransfersReverseSearch = new TransferForPatternByStopPos();


    public TripPatternWithRaptorStopIndexes(
            TripPattern pattern,
            int[] stopIndexes
    ) {
        this.pattern = pattern;
        this.stopIndexes = stopIndexes;
    }

    public FeedScopedId getId() {return pattern.getId();}

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
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        TripPatternWithRaptorStopIndexes that = (TripPatternWithRaptorStopIndexes) o;
        return getId() == that.getId();
    }

    @Override
    public String toString() {
        return "TripPattern{" +
                "id=" + getId() +
                ", transitMode=" + pattern.getMode() +
                '}';
    }

    /**
     * This is public to allow the mappers to inject transfers
     */
    public void addTransferConstraintsForwardSearch(
            int targetStopPosition,
            TransferForPattern transferForPattern
    ) {
        constrainedTransfersForwardSearch.add(targetStopPosition, transferForPattern);
    }

    /**
     * This is public to allow the mappers to inject transfers
     */
    public void addTransferConstraintsReverseSearch(
            int targetStopPosition,
            TransferForPattern transferForPattern
    ) {
        constrainedTransfersReverseSearch.add(targetStopPosition, transferForPattern);
    }

    /**
     * This method should be called AFTER all transfers are added, and before the
     * pattern is used in a Raptor search.
     */
    public void sortConstrainedTransfers() {
        constrainedTransfersForwardSearch.sortOnSpecificityRanking();
        constrainedTransfersReverseSearch.sortOnSpecificityRanking();
    }
}
