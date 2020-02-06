package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.QuayWithoutCoordinates;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransferPriority;
import org.rutebanken.netex.model.InterchangeWeightingEnumeration;
import org.rutebanken.netex.model.Quay;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

class StopMapper {

    private final DataImportIssueStore issueStore;

    private final FeedScopedIdFactory idFactory;

    StopMapper(FeedScopedIdFactory idFactory, DataImportIssueStore issueStore) {
        this.idFactory = idFactory;
        this.issueStore = issueStore;
    }

    /**
     * Map Netex Quay to OTP Stop
     */
    Stop mapQuayToStop(
        Quay quay,
        Station parentStation,
        InterchangeWeightingEnumeration interchangeWeight
    ) {
        Stop stop = new Stop();
        boolean locationOk = verifyPointAndProcessCoordinate(
            quay.getCentroid(),
            // TODO OTP2 - This kind of awkward callback can be avoided if we add a
            //           - Coordinate type the the OTP model, and return that instead.
            coordinate -> {
                stop.setLon(coordinate.getLongitude().doubleValue());
                stop.setLat(coordinate.getLatitude().doubleValue());
            }
        );
        if (!locationOk) {
            issueStore.add(new QuayWithoutCoordinates(quay.getId()));
            return null;
        }
        stop.setId(idFactory.createId(quay.getId()));
        stop.setName(parentStation.getName());
        stop.setCode(quay.getPublicCode());
        stop.setParentStation(parentStation);
        stop.setTransferPriority(mapTransferPriority(interchangeWeight));

        return stop;
    }

    private TransferPriority mapTransferPriority(
        InterchangeWeightingEnumeration interchangeWeight
    ) {
        if (interchangeWeight == null) return TransferPriority.TRANSFER_ALLOWED;
        switch (interchangeWeight) {
            case PREFERRED_INTERCHANGE:
                return TransferPriority.PREFERRED_TRANSFER;
            case RECOMMENDED_INTERCHANGE:
                return TransferPriority.RECOMMENDED_TRANSFER;
            case NO_INTERCHANGE:
                return TransferPriority.NO_TRANSFER;
            case INTERCHANGE_ALLOWED:
            default:
                return TransferPriority.TRANSFER_ALLOWED;
        }
    }
}