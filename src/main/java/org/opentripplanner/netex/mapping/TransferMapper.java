package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.model.transfer.TransferPriority;
import org.opentripplanner.model.transfer.TripTransferPoint;
import org.opentripplanner.netex.issues.InterchangeMaxWaitTimeNotGuaranteed;
import org.opentripplanner.netex.issues.InterchangePointMappingFailed;
import org.opentripplanner.netex.issues.InterchangeWithoutConstraint;
import org.opentripplanner.netex.issues.ObjectNotFound;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.ScheduledStopPointRefStructure;
import org.rutebanken.netex.model.ServiceJourneyInterchange;
import org.rutebanken.netex.model.VehicleJourneyRefStructure;

public class TransferMapper {

    private final FeedScopedIdFactory idFactory;
    private final DataImportIssueStore issueStore;
    private final ArrayListMultimap<String, String> scheduledStopPointsIndex;
    private final EntityById<Trip> trips;

    public TransferMapper(
            FeedScopedIdFactory idFactory,
            DataImportIssueStore issueStore,
            ArrayListMultimap<String, String> scheduledStopPointsIndex,
            EntityById<Trip> trips
    ) {
        this.idFactory = idFactory;
        this.issueStore = issueStore;
        this.scheduledStopPointsIndex = scheduledStopPointsIndex;
        this.trips = trips;
    }

    /**
     * NeTEx ServiceJourneyInterchange example:
     * <pre>
     * ServiceJourneyInterchange {
     *    id: "VYG:ServiceJourneyInterchange:3"
     *    priority: 2
     *    planned: true
     *    guaranteed: true
     *    fromPointRef.ref: "VYG:ScheduledStopPoint:VOS-BUS-341"
     *    toPointRef.ref: "VYG:ScheduledStopPoint:VOS-2"
     *    fromJourneyRef.ref: "VYG:ServiceJourney:BUS-610-322"
     *    toJourneyRef.ref: "VYG:ServiceJourney:610-323"
     * }
     * </pre>
     */
    @Nullable
    public ConstrainedTransfer mapToTransfer(ServiceJourneyInterchange it) {
        var id = it.getId();
        var from = mapPoint("from", id, it.getFromJourneyRef(), it.getFromPointRef());
        var to = mapPoint("to", id, it.getToJourneyRef(), it.getToPointRef());

        if(from==null ||to==null) { return null; }

        var c = mapConstraint(it);
        var tx = new ConstrainedTransfer(idFactory.createId(it.getId()), from, to, c);

        if(tx.noConstraints()) {
            issueStore.add(new InterchangeWithoutConstraint(tx));
            return null;
        }
        return tx;
    }

    @Nullable
    private TripTransferPoint mapPoint(
            String label,
            String interchangeId,
            VehicleJourneyRefStructure sjRef,
            ScheduledStopPointRefStructure pointRef
    ) {
        var sjId = sjRef.getRef();
        var trip = findTrip(label + "Journey", interchangeId, sjId);
        int stopPos = findStopPosition(interchangeId, label + "Point", sjId, pointRef);
        return (trip==null || stopPos<0)
                ? null
                : new TripTransferPoint(trip, stopPos);
    }

    @Nullable
    private Trip findTrip(String fieldName, String rootId, String sjId) {
        var tripId = createId(sjId);
        Trip trip = trips.get(tripId);
        return assertRefExist(fieldName, rootId, sjId, trip) ? trip : null;
    }

    private TransferConstraint mapConstraint(ServiceJourneyInterchange it) {
        var cBuilder = TransferConstraint.create();

        if(it.isStaySeated() != null) { cBuilder.staySeated(it.isStaySeated()); }
        if(it.isGuaranteed() != null) { cBuilder.guaranteed(it.isGuaranteed()); }
        if(it.getPriority() != null) { cBuilder.priority(mapPriority(it.getPriority())); }
        if(it.getMaximumWaitTime() != null) {
            if(it.isGuaranteed()) {
                cBuilder.maxWaitTime((int) it.getMaximumWaitTime().toSeconds());
            }
            else  {
                // Add a warning here and keep the interchange
                issueStore.add(new InterchangeMaxWaitTimeNotGuaranteed(it));
            }
        }
        return cBuilder.build();
    }

    private TransferPriority mapPriority(Number pri) {
        switch (pri.intValue()) {
            case -1:
                return TransferPriority.NOT_ALLOWED;
            case 0:
                return TransferPriority.ALLOWED;
            case 1:
                return TransferPriority.RECOMMENDED;
            case 2:
                return TransferPriority.PREFERRED;
            default:
                throw new IllegalArgumentException("Interchange priority unknown: " + pri);
        }
    }

    private int findStopPosition(
            String interchangeId,
            String label,
            String sjId,
            ScheduledStopPointRefStructure scheduledStopPointRef
    ) {
        String sspId = scheduledStopPointRef.getRef();
        int index = scheduledStopPointsIndex.get(sjId).indexOf(sspId);

        if (index >= 0) { return index; }

        String detailedMsg = scheduledStopPointsIndex.containsKey(sjId)
                ? "Scheduled-stop-point-ref not found"
                : "Service-journey not found";

        issueStore.add(
                new InterchangePointMappingFailed(detailedMsg, interchangeId, label, sjId, sspId)
        );
        return index;
    }

    private FeedScopedId createId(String id) {
        return idFactory.createId(id);
    }

    private <T> boolean assertRefExist(String fieldName, String interchangeId, String id, T instance) {
        if (instance == null) {
            issueStore.add(
                    new ObjectNotFound("Interchange", interchangeId, fieldName, id)
            );
            return false;
        }
        return true;
    }
}
