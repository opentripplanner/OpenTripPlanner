package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.WgsCoordinate;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

class MultiModalStationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(MultiModalStationMapper.class);

    private final FeedScopedIdFactory idFactory;

    public MultiModalStationMapper(FeedScopedIdFactory idFactory) {
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
            LOG.warn(
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
