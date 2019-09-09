package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.impl.EntityById;

import java.util.Collection;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/**
 * Links Stop and Stations in both directions after both have been mapped.
 */
public class StationStopLinker {

        public void link(
                Collection<Stop> gtfsStops,
                EntityById<FeedScopedId, Station> otpStations,
                EntityById<FeedScopedId, org.opentripplanner.model.Stop> otpStops
        ) {
                for (Stop stop : gtfsStops) {
                        if (stop.getLocationType() == 0 && stop.getParentStation() != null) {
                               org.opentripplanner.model.Stop otpStop = otpStops.get(mapAgencyAndId(
                                                new AgencyAndId(stop.getId().getAgencyId(),
                                                                stop.getId().getId())));

                                Station otpStation = otpStations.get(mapAgencyAndId(
                                        new AgencyAndId(stop.getId().getAgencyId(),
                                                stop.getParentStation())));

                               otpStop.setParentStation(otpStation);
                               otpStation.addChildStop(otpStop);
                        }
                }
        }
}
