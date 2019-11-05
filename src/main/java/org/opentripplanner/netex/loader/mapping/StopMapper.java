package org.opentripplanner.netex.loader.mapping;

import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.rutebanken.netex.model.Quay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opentripplanner.netex.loader.mapping.PointMapper.verifyPointAndProcessCoordinate;

class StopMapper {
    private static final Logger LOG = LoggerFactory.getLogger(StopMapper.class);

    private final FeedScopedIdFactory idFactory;

    StopMapper(FeedScopedIdFactory idFactory) {
        this.idFactory = idFactory;
    }

    /**
     * Map Netex Quay to OTP Stop
     */
    Stop mapQuayToStop(Quay quay, Station parentStation) {
        Stop stop = new Stop();
        boolean locationOk = verifyPointAndProcessCoordinate(
                quay.getCentroid(),
                // TODO OTP2 - This kind of awkward callback can be avoided if we add a
                //           - Coordinate type the the OTP model, and return that instead.
                coordinate -> {
                    stop.setLon(coordinate.getLongitude().doubleValue());
                    stop.setLat(coordinate.getLatitude().doubleValue());
                }
        );
        if (!locationOk) {
            LOG.warn("Quay {} does not contain any coordinates. Quay is ignored.", quay.getId());
            return null;
        }
        stop.setId(idFactory.createId(quay.getId()));
        stop.setName(parentStation.getName());
        stop.setCode(quay.getPublicCode());
        stop.setParentStation(parentStation);
        return stop;
    }
}
