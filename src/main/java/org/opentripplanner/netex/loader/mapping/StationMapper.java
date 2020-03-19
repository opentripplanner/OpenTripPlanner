package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Coordinate;
import org.opentripplanner.model.Station;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.loader.mapping.PointMapper.mapCoordinate;

class StationMapper {

    private static final Logger LOG = LoggerFactory.getLogger(StationMapper.class);

    private final FeedScopedIdFactory idFactory;

    StationMapper(FeedScopedIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    Station map(StopPlace stopPlace) {
        Station station = new Station();

        if (stopPlace.getName() != null) {
            station.setName(stopPlace.getName().getValue());
        } else {
            station.setName("N/A");
        }

        Coordinate coordinate = mapCoordinate(stopPlace.getCentroid());

        if (coordinate == null) {
            LOG.warn("Station {} does not contain any coordinates.", station.getId());
        }
        else {
            station.setCoordinate(coordinate);
        }
        station.setId(idFactory.createId(stopPlace.getId()));

        return station;
    }
}
