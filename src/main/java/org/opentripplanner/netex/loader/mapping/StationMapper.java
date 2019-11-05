package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Station;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

class StationMapper {

        private static final Logger LOG = LoggerFactory.getLogger(StationMapper.class);

        static Station mapToStation(StopPlace stop) {
                Station station = new Station();

                if (stop.getName() != null) {
                        station.setName(stop.getName().getValue());
                } else {
                        station.setName("N/A");
                }
                boolean locationOk = verifyPointAndProcessCoordinate(
                        stop.getCentroid(),
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

                station.setId(FeedScopedIdFactory.createFeedScopedId(stop.getId()));

                return station;
        }
}
