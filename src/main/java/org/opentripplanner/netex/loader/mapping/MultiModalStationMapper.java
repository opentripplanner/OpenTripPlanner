package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.MultiModalStation;
import org.rutebanken.netex.model.StopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

class MultiModalStationMapper {
        private static final Logger LOG = LoggerFactory.getLogger(MultiModalStationMapper.class);

        static MultiModalStation map(StopPlace stopPlace) {
                MultiModalStation multiModalStation = new MultiModalStation();

                if (stopPlace.getName() != null) {
                        multiModalStation.setName(stopPlace.getName().getValue());
                } else {
                        multiModalStation.setName("N/A");
                }
                boolean locationOk = verifyPointAndProcessCoordinate(
                        stopPlace.getCentroid(),
                        // This kind of awkward callback can be avoided if we add a
                        // Coordinate type the the OTP model, and return that instead.
                        coordinate -> {
                                multiModalStation.setLon(coordinate.getLongitude().doubleValue());
                                multiModalStation.setLat(coordinate.getLatitude().doubleValue());
                        }
                );

                if (!locationOk) {
                        LOG.warn(
                                "MultiModal station {} does not contain any coordinates.",
                                multiModalStation.getId()
                        );
                }

                multiModalStation.setId(FeedScopedIdFactory.createFeedScopedId(stopPlace.getId()));

                return multiModalStation;
        }
}
