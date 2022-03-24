package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.rangeraptor.SlackProvider;
import org.opentripplanner.transit.raptor.rangeraptor.WorkerLifeCycle;

/**
 * This class is an adapter for the internal {@link SlackProvider} which wrap the
 * api {@link RaptorSlackProvider}. The Adapter is needed to swap board/alight
 * in the reverse search. It also incorporates the transfer slack into the bordSlack,
 * so the algorithm have one thing less to account for.
 * <p>
 * Uses the adapter design pattern.
 * <p>
 * Use the factory methods to create new instances for forward and reverse search.
 */
public final class SlackProviderAdapter {

    private SlackProviderAdapter() { /* empty */ }

    public static SlackProvider forwardSlackProvider(
        RaptorSlackProvider source,
        WorkerLifeCycle lifeCycle
    ) {
        var slackProvider = new ForwardSlackProvider(source);
        lifeCycle.onPrepareForNextRound(slackProvider::notifyNewRound);
        return slackProvider;
    }

    public static SlackProvider reverseSlackProvider(
        RaptorSlackProvider source,
        WorkerLifeCycle lifeCycle
    ) {
        var slackProvider = new ReverseSlackProvider(source);
        lifeCycle.onPrepareForNextRound(slackProvider::notifyNewRound);
        return slackProvider;
    }

    private static final class ForwardSlackProvider implements SlackProvider {
        private final RaptorSlackProvider source;
        private int transferSlack;

        private ForwardSlackProvider(RaptorSlackProvider source) {
            this.source = source;
            this.transferSlack = 0;
        }

        public void notifyNewRound(int round) {
            transferSlack = round < 2 ? 0 : source.transferSlack();
        }

        @Override
        public int boardSlack(RaptorTripPattern pattern) {
            return source.boardSlack(pattern) + transferSlack;
        }

        @Override
        public int alightSlack(RaptorTripPattern pattern) {
            return source.alightSlack(pattern);
        }

        @Override
        public int accessEgressWithRidesTransferSlack() {
            return source.transferSlack();
        }
    }

    private static final class ReverseSlackProvider implements SlackProvider {
        private final RaptorSlackProvider source;
        private int transferSlack;

        private ReverseSlackProvider(RaptorSlackProvider source) {
            this.source = source;
            this.transferSlack = 0;
        }

        public void notifyNewRound(int round) {
            transferSlack = round < 2 ? 0 : source.transferSlack();
        }

        @Override
        public int boardSlack(RaptorTripPattern pattern) {
            return source.alightSlack(pattern) + transferSlack;
        }

        @Override
        public int alightSlack(RaptorTripPattern pattern) {
            return source.boardSlack(pattern);
        }

        @Override
        public int accessEgressWithRidesTransferSlack() {
            return source.transferSlack();
        }
    }
}
