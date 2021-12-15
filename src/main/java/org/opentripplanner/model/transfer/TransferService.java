package org.opentripplanner.model.transfer;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents all transfer information in the graph. Transfers are grouped by
 * stop-to-stop pairs.
 * <p>
 * THIS CLASS IS NOT THREAD_SAFE. This class is loaded with plan data only, and read-only during
 * routing. No real-time update should touch this class; Hence it do not need to be thread-safe.
 */
public class TransferService implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TransferService.class);

    /** Index of guaranteed transfers by the to/destination point. */
    private final Multimap<TripTransferPoint, ConstrainedTransfer> constrainedTransferByToPoint;

    /**
     * Table which contains transfers between two trips/routes
     */
    private final Map<P2<TripTransferPoint>, ConstrainedTransfer> trip2tripTransfers;

    /**
     * Table which contains transfers between a trip/route and a stops
     */
    private final Map<T2<TripTransferPoint, StopLocation>, ConstrainedTransfer> trip2StopTransfers;

    /**
     * Table which contains transfers between a stop and a trip/route
     */
    private final Map<T2<StopLocation, TripTransferPoint>, ConstrainedTransfer> stop2TripTransfers;

    /**
     * Table which contains transfers between two stops
     */
    private final Map<P2<StopLocation>, ConstrainedTransfer> stop2StopTransfers;

    public TransferService() {
        this.constrainedTransferByToPoint = ArrayListMultimap.create();
        this.trip2tripTransfers = new HashMap<>();
        this.trip2StopTransfers = new HashMap<>();
        this.stop2TripTransfers = new HashMap<>();
        this.stop2StopTransfers = new HashMap<>();
    }

    public void addAll(Collection<ConstrainedTransfer> transfers) {
        for (ConstrainedTransfer transfer : transfers) {
            add(transfer);
        }
    }

    public List<ConstrainedTransfer> listAll() {
        var list = new ArrayList<ConstrainedTransfer>();
        list.addAll(trip2tripTransfers.values());
        list.addAll(trip2StopTransfers.values());
        list.addAll(stop2TripTransfers.values());
        list.addAll(stop2StopTransfers.values());
        return list;
    }

    public Collection<ConstrainedTransfer> listConstrainedTransfersTo(Trip toTrip, int toStopIndex) {
        return constrainedTransferByToPoint.get(new TripTransferPoint(toTrip, toStopIndex));
    }

    @Nullable
    public ConstrainedTransfer findTransfer(
            StopLocation fromStop,
            StopLocation toStop,
            Trip fromTrip,
            Trip toTrip,
            int fromStopPosition,
            int toStopPosition
    ) {
        var fromTripKey = new TripTransferPoint(fromTrip, fromStopPosition);
        var toTripKey = new TripTransferPoint(toTrip, toStopPosition);
        ConstrainedTransfer result;

        // Check the highest specificity ranked transfers first (trip-2-trip)
        result = trip2tripTransfers.get(new P2<>(fromTripKey, toTripKey));
        if (result != null) { return result; }

        // Then check the next specificity ranked transfers (trip-2-stop and stop-2-trip)
        result = trip2StopTransfers.get(new T2<>(fromTripKey, toStop));
        if (result != null) { return result; }

        // Then check the next specificity ranked transfers (trip-2-stop and stop-2-trip)
        result = stop2TripTransfers.get(new T2<>(fromStop, toTripKey));
        if (result != null) { return result; }

        // If no specificity ranked transfers found return stop-2-stop transfers (lowest ranking)
        return stop2StopTransfers.get(new P2<>(fromStop, toStop));
    }

    void add(ConstrainedTransfer transfer) {
        TransferPoint from = transfer.getFrom();
        TransferPoint to = transfer.getTo();

        addFacilitatedTransfer(transfer);

        if (from instanceof TripTransferPoint) {
            var fromTrip = (TripTransferPoint) from;
            if (to instanceof TripTransferPoint) {
                var key = new P2<>(fromTrip, (TripTransferPoint) to);
                if (doAddTransferBasedOnSpecificityRanking(transfer, trip2tripTransfers.get(key))) {
                    trip2tripTransfers.put(key, transfer);
                }
            }
            else {
                var key = new T2<>(fromTrip, to.getStop());
                if (doAddTransferBasedOnSpecificityRanking(transfer, trip2StopTransfers.get(key))) {
                    trip2StopTransfers.put(key, transfer);
                }
            }
        }
        else if (to instanceof TripTransferPoint) {
            var key = new T2<>(from.getStop(), (TripTransferPoint) to);
            if (doAddTransferBasedOnSpecificityRanking(transfer, stop2TripTransfers.get(key))) {
                stop2TripTransfers.put(key, transfer);
            }
        }
        else {
            var key = new P2<>(from.getStop(), to.getStop());
            if (doAddTransferBasedOnSpecificityRanking(transfer, stop2StopTransfers.get(key))) {
                stop2StopTransfers.put(key, transfer);
            }
        }
    }

    /**
     * A transfer goes from/to a stop, route* or trip. Route transfers are expanded to all trips
     * using the special {@link RouteTransferPoint} subtype of {@link TripTransferPoint}. This
     * expansion make sure that there can only be one match for each combination of from and to
     * combination (from -> to):
     * <ol>
     *     <li> trip -> trip
     *     <li> trip -> stop
     *     <li> stop -> trip
     *     <li> stop -> stop
     * </ol>
     * For each pair of the above combination we can drop the transfers that have a the lowest
     * specificity-ranking, thus using maps instead of multi-maps.
     */
    private boolean doAddTransferBasedOnSpecificityRanking(
            ConstrainedTransfer newTransfer,
            ConstrainedTransfer existingTransfer
    ) {
        if (existingTransfer == null) { return true; }

        if (existingTransfer.getSpecificityRanking() < newTransfer.getSpecificityRanking()) {
            return true;
        }
        if (existingTransfer.getSpecificityRanking() > newTransfer.getSpecificityRanking()) {
            return false;
        }
        if (existingTransfer.equals(newTransfer)) {
            return false;
        }
        LOG.warn(
                "Two colliding transfers A and B with the same specificity-ranking is imported, B is "
                        + "dropped. A={}, B={}", existingTransfer, newTransfer
        );
        return false;
    }

    private void addFacilitatedTransfer(ConstrainedTransfer transfer) {
        var c = transfer.getTransferConstraint();
        var toPoint = transfer.getTo();

        if(c.isFacilitated()) {
            if(toPoint instanceof TripTransferPoint) {
                constrainedTransferByToPoint.put((TripTransferPoint) toPoint, transfer);
            }
        }
    }
}
