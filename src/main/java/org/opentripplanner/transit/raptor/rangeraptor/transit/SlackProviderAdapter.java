package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

/**
 * This class is an adapter for the internal {@link SlackProvider} witch wrap the
 * api {@link RaptorSlackProvider}. The Adapter is needed to swap board/alight
 * in the reverse search. It also incorporate the transfer slack into the bordSlack,
 * so the algorithm have one thing less to account for.
 * <p>
 * Uses the adapter design pattern.
 * <p>
 * Use the factory methods to create new instances for forward and reverse search.
 */
public final class SlackProviderAdapter implements SlackProvider {

    private int transferSlack;
    private int boardSlack;
    private int alightSlack;

    private final ToIntFunction<RaptorTripPattern> sourceBoardSlack;
    private final ToIntFunction<RaptorTripPattern> sourceAlightSlack;
    private final IntSupplier sourceTransferSlack;

    private SlackProviderAdapter(
        ToIntFunction<RaptorTripPattern> sourceBoardSlack,
        ToIntFunction<RaptorTripPattern> sourceAlightSlack,
        IntSupplier sourceTransferSlack,
        WorkerLifeCycle lifeCycle
    ) {
        this.sourceBoardSlack = sourceBoardSlack;
        this.sourceAlightSlack = sourceAlightSlack;
        this.sourceTransferSlack = sourceTransferSlack;
        lifeCycle.onPrepareForNextRound(this::notifyNewRound);
    }

    public static SlackProvider forwardSlackProvider(
        RaptorSlackProvider source,
        WorkerLifeCycle lifeCycle
    ) {
        return new SlackProviderAdapter(
            source::boardSlack,
            source::alightSlack,
            source::transferSlack,
            lifeCycle
        );
    }

    public static SlackProvider reverseSlackProvider(
        RaptorSlackProvider source,
        WorkerLifeCycle lifeCycle
    ) {
        return new SlackProviderAdapter(
            source::alightSlack,
            source::boardSlack,
            source::transferSlack,
            lifeCycle
        );
    }

    public void notifyNewRound(int round) {
        transferSlack = round < 2 ? 0 : sourceTransferSlack.getAsInt();
    }

    @Override
    public void setCurrentPattern(RaptorTripPattern pattern) {
        this.boardSlack = sourceBoardSlack.applyAsInt(pattern) + transferSlack;
        this.alightSlack = sourceAlightSlack.applyAsInt(pattern);
    }

    @Override
    public final int boardSlack() {
        return boardSlack;
    }

    @Override
    public final int alightSlack() {
        return alightSlack;
    }

    @Override
    public int accessEgressWithRidesTransferSlack() {
        return sourceTransferSlack.getAsInt();
    }
}
