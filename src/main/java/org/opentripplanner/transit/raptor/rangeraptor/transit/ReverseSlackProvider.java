package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

/**
 * This class swap the {@code alightSlack} and {@code boardSlack} around to support
 * reverse search, and add the {@code transferSlack} to the {@code alightSlack} to
 * achieve the same effect as in a forward search.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class ReverseSlackProvider<T extends RaptorTripSchedule> implements SlackProvider {

    private final RaptorSlackProvider source;
    private boolean ignoreTransferSlack = true;
    private int reverseAlightSlack;
    private int revereBoardSlack;

    public ReverseSlackProvider(RaptorSlackProvider source, WorkerLifeCycle lifeCycle) {
        this.source = source;
        lifeCycle.onPrepareForNextRound(this::notifyNewRound);
    }

    public void notifyNewRound(int round) {
        ignoreTransferSlack = round < 2;
    }

    @Override
    public void setCurrentPattern(RaptorTripPattern pattern) {
        this.revereBoardSlack = source.alightSlack(pattern);
        this.reverseAlightSlack = source.boardSlack(pattern) + transferSlack();
    }

    @Override
    public final int boardSlack() {
        return revereBoardSlack;
    }

    @Override
    public final int alightSlack() {
        return reverseAlightSlack;
    }

    private int transferSlack() {
        return ignoreTransferSlack ? 0 : source.transferSlack();
    }
}
