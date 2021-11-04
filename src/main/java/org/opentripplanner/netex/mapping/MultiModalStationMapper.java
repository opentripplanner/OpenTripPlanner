package org.opentripplanner.netex.mapping;

import java.util.Collection;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.StopPlace;

class MultiModalStationMapper {

    private final DataImportIssueStore issueStore;
    private final FeedScopedIdFactory idFactory;

    public MultiModalStationMapper(DataImportIssueStore issueStore, FeedScopedIdFactory idFactory) {
        this.issueStore = issueStore;
        this.idFactory = idFactory;
    }

    MultiModalStation map(StopPlace stopPlace, Collection<Station> childStations) {
        MultiModalStation multiModalStation = new MultiModalStation(
                idFactory.createId(stopPlace.getId()),
                childStations
        );

        if (stopPlace.getName() != null) {
            multiModalStation.setName(stopPlace.getName().getValue());
        }
        else {
            multiModalStation.setName("N/A");
        }

        WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(stopPlace.getCentroid());

        if (coordinate == null) {
            issueStore.add(
                    "MultiModalStationWithoutCoordinates",
                    "MultiModal station {} does not contain any coordinates.",
                    multiModalStation.getId()
            );
        }
        else {
            multiModalStation.setCoordinate(coordinate);
        }
        return multiModalStation;
    }
}
