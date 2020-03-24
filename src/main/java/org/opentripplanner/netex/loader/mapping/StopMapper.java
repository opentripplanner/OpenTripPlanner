package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.QuayWithoutCoordinates;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.rutebanken.netex.model.Quay;

import static org.opentripplanner.netex.loader.mapping.PointMapper.mapCoordinate;

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
    Stop mapQuayToStop(Quay quay, Station parentStation) {
        WgsCoordinate coordinate = mapCoordinate(quay.getCentroid());

        if (coordinate == null) {
                issueStore.add(new QuayWithoutCoordinates(quay.getId()));
            return null;
        }

        Stop stop = new Stop();
        stop.setId(idFactory.createId(quay.getId()));
        stop.setName(parentStation.getName());
        stop.setCode(quay.getPublicCode());
        stop.setParentStation(parentStation);
        stop.setCoordinate(coordinate);

        return stop;
    }
}
