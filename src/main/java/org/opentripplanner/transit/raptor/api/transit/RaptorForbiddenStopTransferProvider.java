package org.opentripplanner.transit.raptor.api.transit;

import org.opentripplanner.model.Stop;


/**
 * This interface enable the transit layer to skip the normal trip search in Raptor. Each {@link
 * RaptorRoute} may provide an instance of this interface to so Raptor can verify it can board stop
 * with the route as the target.
 * <p>
 * When searching forward the <em>target</em> is the "to" end of the transfer, and the
 * <em>source</em> is the "from" transfer point. For a reverse search the <em>target</em> is "from"
 * and the <em>source</em> is the "to" transfer point. Note that only stop to stop transfers are
 * handled.
 */
public interface RaptorForbiddenStopTransferProvider {

    /**
     * Check if the current pattern have any forbidden transfers for the given stop position in
     * pattern. If so, Raptor will not board these stop to stop transfers.
     */
    boolean transferExist(int targetStopPos);

    /**
     * Get the forbidden transfers for the current pattern at the target stop position coming from
     * the source stop.
     */
    boolean isForbiddenTransfer(
            Stop sourceStop
    );
}
