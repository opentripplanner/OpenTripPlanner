package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Station;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

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
        boolean locationOk = verifyPointAndProcessCoordinate(
                stopPlace.getCentroid(),
                // This kind of awkward callback can be avoided if we add a
                // Coordinate type the the OTP model, and return that instead.
                coordinate -> {
                    station.setLon(coordinate.getLongitude().doubleValue());
                    station.setLat(coordinate.getLatitude().doubleValue());
                }
        );

        if (!locationOk) {
            LOG.warn("Station {} does not contain any coordinates.", station.getId());
        }

        station.setId(idFactory.createId(stopPlace.getId()));

        return station;
    }
}
