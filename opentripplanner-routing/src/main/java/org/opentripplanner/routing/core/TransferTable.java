package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.opentripplanner.common.model.P2;

public class TransferTable implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int UNKNOWN_TRANSFER = -999;

    public static final int PREFERRED_TRANSFER = -2;
    
    public static final int FORBIDDEN_TRANSFER = -1;

    public static final int TIMED_TRANSFER = 0; /*
                                                 * In a timed transfer, the departing vehicle will
                                                 * wait for passengers from the arriving vehicle, so
                                                 * the minimum transfer time is effectively zero
                                                 */


    protected HashMap<P2<Vertex>, Integer> table = new HashMap<P2<Vertex>, Integer>();
    protected boolean preferredTransfers = false;
    
    public void setPreferredTransfers(boolean preferredTransfers) {
        this.preferredTransfers = preferredTransfers;
    }
    
    public boolean hasPreferredTransfers() {
        return preferredTransfers;
    }    
    /** Get the transfer time, in seconds, between the stops */
    public int getTransferTime(Vertex previousStop, Vertex vertex) {
        Integer result = table.get(new P2<Vertex>(previousStop, vertex));
        if (result == null) {
            return UNKNOWN_TRANSFER;
        }
        return result;
    }
    
    public void setTransferTime(Vertex fromStop, Vertex toStop, int transferTime) {
        table.put(new P2<Vertex>(fromStop, toStop), transferTime);
        if (transferTime == PREFERRED_TRANSFER) {
            setPreferredTransfers(true);
        }
    }
    
    public class Transfer {
        public Vertex from, to;
        public int seconds;
        public Transfer(Vertex from, Vertex to, int seconds) {
            this.from = from;
            this.to = to;
            this.seconds = seconds;
        }        
    }
    
    public Iterable<Transfer> getAllTransfers() {
        ArrayList<Transfer> transfers = new ArrayList<Transfer>(table.size());
        for (Entry<P2<Vertex>, Integer> entry : table.entrySet()) {
            P2<Vertex> p2 = entry.getKey();
            transfers.add(new Transfer(p2.getFirst(), p2.getSecond(), entry.getValue()));
        }
        return transfers;
    }
}
