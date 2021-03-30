package org.opentripplanner.model.transfer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.Collection;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

// TODO OTP2 reimplement all special kinds of transfers

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 */
public class TransferService implements Serializable {

    /**
     * Table which contains transfers between two stops
     */
    protected Multimap<P2<Stop>, Transfer> table = ArrayListMultimap.create();

    private Transfer getTransfer(Stop fromStop, Stop toStop, Trip fromTrip, Trip toTrip) {
        Collection<Transfer> transfers = table.get(new P2<>(fromStop, toStop));
        for (Transfer transfer : transfers) {
            if (transfer.getFromTrip() == fromTrip && transfer.getToTrip() == toTrip) {
                return transfer;
            }
        }
        return null;
    }

    public Collection<Transfer> getTransfers() {
        return table.values();
    }

    public void addAll(Collection<Transfer> transfers) {
        for (Transfer transfer : transfers) {
            add(transfer);
        }
    }

    void add(Transfer transfer) {
        table.put(new P2<>(transfer.getFromStop(), transfer.getToStop()), transfer);
    }
}
