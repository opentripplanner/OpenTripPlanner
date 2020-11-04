package org.opentripplanner.netex.mapping;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

class GroupOfStationsMapper {

    private static final Logger LOG = LoggerFactory.getLogger(GroupOfStationsMapper.class);

    private final FeedScopedIdFactory idFactory;

    private final EntityById<MultiModalStation> multiModalStations;

    private final EntityById<Station> stations;


    GroupOfStationsMapper(
            FeedScopedIdFactory idFactory,
            EntityById<MultiModalStation> multiModalStations,
            EntityById<Station> stations
    ) {
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
            // TODO OTP2 - This should be an data import issue
            LOG.warn(
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
                    LOG.warn("GroupOfStation {} child not found: {}", groupOfStations.getId(), stationId);
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
