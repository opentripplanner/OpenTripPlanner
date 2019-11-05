package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.impl.EntityById;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.StopPlaceRefStructure;
import org.rutebanken.netex.model.StopPlaceRefs_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

class GroupOfStationsMapper {

        private static final Logger LOG = LoggerFactory.getLogger(GroupOfStationsMapper.class);

        private final FeedScopedIdFactory idFactory;

        private final EntityById<FeedScopedId, MultiModalStation> multiModalStations;

        private final EntityById<FeedScopedId, Station> stations;


        GroupOfStationsMapper(
                FeedScopedIdFactory idFactory,
                EntityById<FeedScopedId, MultiModalStation> multiModalStations,
                EntityById<FeedScopedId, Station> stations
        ) {
                this.idFactory = idFactory;
                this.multiModalStations = multiModalStations;
                this.stations = stations;
        }

        GroupOfStations map(GroupOfStopPlaces groupOfStopPlaces) {
                GroupOfStations groupOfStations = new GroupOfStations();
                groupOfStations.setId(
                        idFactory.createId(groupOfStopPlaces.getId()));
                groupOfStations.setName(groupOfStopPlaces.getName().getValue());
                boolean locationOk = verifyPointAndProcessCoordinate(
                        groupOfStopPlaces.getCentroid(),
                        // This kind of awkward callback can be avoided if we add a
                        // Coordinate type the the OTP model, and return that instead.
                        coordinate -> {
                                groupOfStations.setLon(coordinate.getLongitude().doubleValue());
                                groupOfStations.setLat(coordinate.getLatitude().doubleValue());
                        }
                );

                if (!locationOk) {
                        LOG.warn(
                                "MultiModal station {} does not contain any coordinates.",
                                groupOfStations.getId()
                        );
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
                                FeedScopedId stationId =
                                        idFactory.createId(stopPlaceRefStructure.getRef());
                                if (stations.containsKey(stationId)) {
                                        groupOfStations.addChildStation(
                                                stations.get(stationId)
                                        );
                                } else if (multiModalStations.containsKey(stationId)) {
                                        groupOfStations.addChildStation(
                                                multiModalStations.get(stationId)
                                        );
                                }
                        }
                }
        }
}
