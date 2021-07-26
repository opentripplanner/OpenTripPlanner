package org.opentripplanner.routing.algorithm.raptor.transit.request;

import gnu.trove.map.TIntObjectMap;
import java.util.List;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.model.transfer.TransferPoint;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorForbiddenStopTransferProvider;


/**
 * The responsibility of this class is to provide forbidden transfers to the Raptor search for a
 * given pattern. The instance is stateful and not thread-safe. The current stop position is checked
 * for transfers, then the provider is asked to list all transfers between the current pattern and
 * the source trip stop arrival. The source is the "from" point in a transfer for a forward search,
 * and the "to" point in the reverse search. Note that only stop to stop transfers are handled.
 */
public final class PatternForbiddenStopTransferProvider
        implements RaptorForbiddenStopTransferProvider {

    private final DirectionHelper translator;

    /**
     * List of transfers for each stop position in pattern
     */
    private final TIntObjectMap<List<Transfer>> transfers;

    private List<Transfer> currentTransfers;

    public PatternForbiddenStopTransferProvider(
            boolean forwardSearch,
            TIntObjectMap<List<Transfer>> transfers
    ) {
        this.translator = forwardSearch
                ? new ForwardDirectionHelper()
                : new ReverseDirectionHelper();
        this.transfers = transfers;
    }

    @Override
    public final boolean transferExist(int targetStopPos) {
        if (transfers == null) { return false; }

        // Get all forbidden transfers for the target pattern at the target stop position
        this.currentTransfers = transfers.get(targetStopPos);
        return currentTransfers != null;
    }

    @Override
    public final boolean isForbiddenTransfer(
            Stop sourceStop
    ) {
        for (Transfer tx : currentTransfers) {
            var sourcePoint = translator.source(tx);
            if (sourcePoint.getStop() == sourceStop) {
                return true;
            }
        }
        return false;
    }

    private interface DirectionHelper {

        TransferPoint source(Transfer tx);

        TransferPoint target(Transfer tx);
    }

    private static class ForwardDirectionHelper implements DirectionHelper {

        @Override
        public TransferPoint source(Transfer tx) { return tx.getFrom(); }

        @Override
        public TransferPoint target(Transfer tx) { return tx.getTo(); }
    }

    private static class ReverseDirectionHelper implements DirectionHelper {

        @Override
        public TransferPoint source(Transfer tx) { return tx.getTo(); }

        @Override
        public TransferPoint target(Transfer tx) { return tx.getFrom(); }
    }
}
