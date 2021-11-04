package org.opentripplanner.netex.mapping;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;

class GroupOfStationsMapper {

    private final DataImportIssueStore issueStore;

    private final FeedScopedIdFactory idFactory;

    private final EntityById<MultiModalStation> multiModalStations;

    private final EntityById<Station> stations;


    GroupOfStationsMapper(
            DataImportIssueStore issueStore,
            FeedScopedIdFactory idFactory,
            EntityById<MultiModalStation> multiModalStations,
            EntityById<Station> stations
    ) {
        this.issueStore = issueStore;
        this.idFactory = idFactory;
        this.multiModalStations = multiModalStations;
        this.stations = stations;
    }

    GroupOfStations map(GroupOfStopPlaces groupOfStopPlaces) {
        GroupOfStations groupOfStations = new GroupOfStations(
            idFactory.createId(groupOfStopPlaces.getId())
        );
        groupOfStations.setName(groupOfStopPlaces.getName().getValue());
        WgsCoordinate coordinate = WgsCoordinateMapper.mapToDomain(groupOfStopPlaces.getCentroid());

        if (coordinate == null) {
            issueStore.add(
                    "GroupOfStationWithoutCoordinates",
                    "MultiModal station {} does not contain any coordinates.",
                    groupOfStations.getId()
            );
        }
        else {
            groupOfStations.setCoordinate(coordinate);
        }

        connectChildStation(groupOfStopPlaces, groupOfStations);

        return groupOfStations;
    }

    private void connectChildStation(
            GroupOfStopPlaces groupOfStopPlaces,
            GroupOfStations groupOfStations
    ) {
        StopPlaceRefs_RelStructure members = groupOfStopPlaces.getMembers();
        if (members != null) {
            List<StopPlaceRefStructure> memberList = members.getStopPlaceRef();
            for (StopPlaceRefStructure stopPlaceRefStructure : memberList) {
                FeedScopedId stationId = idFactory.createId(stopPlaceRefStructure.getRef());
                StopCollection station = lookupStation(stationId);
                if (station != null) {
                    groupOfStations.addChildStation(station);
                } else {
                    issueStore.add(
                            "GroupOfStationWithoutStations",
                            "GroupOfStation %s child not found: %s",
                            groupOfStations.getId(),
                            stationId
                    );
                }
            }
        }
    }

    @Nullable
    private StopCollection lookupStation(FeedScopedId stationId) {
        if (stations.containsKey(stationId)) {
            return stations.get(stationId);
        } else if (multiModalStations.containsKey(stationId)) {
            return multiModalStations.get(stationId);
        }
        return null;
    }
}
