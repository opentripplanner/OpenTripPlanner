package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Transfer;
import org.opentripplanner.model.Trip;
import org.opentripplanner.common.model.P2;

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 */
public class TransferTable implements Serializable {

    /**
     * Table which contains transfers between two stops
     */
    protected Map<P2<Stop>, Transfer> table = new HashMap<>();

    private Transfer getTransfer(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip) {
        Transfer transfer = table.get(new P2<>(fromStop, toStop));
        if (transfer.getFromTrip() == fromTrip && transfer.getToTrip() == toTrip) {
            return transfer;
        } else {
            return null;
        }
    }

    public void addTransfer(Transfer transfer) {
        table.put(new P2<>(transfer.getFromStop(), transfer.getToStop()), transfer);
    }
}
