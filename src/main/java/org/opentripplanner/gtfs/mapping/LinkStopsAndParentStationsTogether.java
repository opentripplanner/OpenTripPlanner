package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.ParentStationNotFound;
import org.opentripplanner.model.*;
import org.opentripplanner.model.impl.EntityById;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

import static org.opentripplanner.gtfs.mapping.AgencyAndIdMapper.mapAgencyAndId;

/**
 * Links child stops with parent stations by adding bidirectional object references.
 */
class LinkStopsAndParentStationsTogether {
    private final EntityById<FeedScopedId, Station> otpStations;
    private final EntityById<FeedScopedId, Stop> otpStops;
    private final EntityById<FeedScopedId, Entrance> entrances;
    private final EntityById<FeedScopedId, PathwayNode> nodes;
    private final EntityById<FeedScopedId, BoardingArea> boardingAreas;

    private final DataImportIssueStore issueStore;

    private static Logger LOG = LoggerFactory.getLogger(LinkStopsAndParentStationsTogether.class);

    LinkStopsAndParentStationsTogether(
            EntityById<FeedScopedId, Station> stations,
            EntityById<FeedScopedId, Stop> stops,
            EntityById<FeedScopedId, Entrance> entrances,
            EntityById<FeedScopedId, PathwayNode> nodes,
            EntityById<FeedScopedId, BoardingArea> boardingAreas,
            DataImportIssueStore issueStore
    ) {
        this.otpStations = stations;
        this.otpStops = stops;
        this.entrances = entrances;
        this.nodes = nodes;
        this.boardingAreas = boardingAreas;
        this.issueStore = issueStore;
    }

    void link(Collection<org.onebusaway.gtfs.model.Stop> gtfsStops) {
        for (org.onebusaway.gtfs.model.Stop gtfsStop : gtfsStops) {
            if (gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION
                    && gtfsStop.getLocationType() != org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA
                    && gtfsStop.getParentStation() != null) {

                TransitEntity<FeedScopedId> otpStop = getOtpStop(gtfsStop);
                Station otpStation = getOtpParentStation(gtfsStop);

                if (otpStation == null) {
                    issueStore.add(
                            new ParentStationNotFound(
                                    otpStop,
                                    gtfsStop.getParentStation()
                            )
                    );
                    continue;
                }

                if (otpStop instanceof Stop) {
                    Stop stop = ((Stop) otpStop);
                    stop.setParentStation(otpStation);
                    otpStation.addChildStop(stop);
                    if (stop.getCoordinate() == null) {
                        stop.setCoordinate(new WgsCoordinate(otpStation.getLat(), otpStation.getLon()));
                    }
                } else if (otpStop instanceof PathwayNode) {
                    PathwayNode node = (PathwayNode) otpStop;
                    if (node.getCoordinate() == null) {
                        node.setCoordinate(new WgsCoordinate(otpStation.getLat(), otpStation.getLon()));
                    }
                }
            } else if (gtfsStop.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA
                    && gtfsStop.getParentStation() != null) {
                BoardingArea otpBoardingArea = getOtpBoardingArea(gtfsStop);
                FeedScopedId otpStopId = mapAgencyAndId(gtfsStop.getId());
                Stop otpStop = otpStops.get(otpStopId);

                if (otpStop == null) {
                    issueStore.add(
                        new ParentStationNotFound(
                            otpBoardingArea,
                            gtfsStop.getParentStation()
                        )
                    );
                    continue;
                }

                otpBoardingArea.setParentStop(otpStop);
                otpStop.addBoardingArea(otpBoardingArea);
            }
        }
    }

    private Station getOtpParentStation(org.onebusaway.gtfs.model.Stop stop) {
        FeedScopedId otpParentStationId = new FeedScopedId(stop.getId().getAgencyId(), stop.getParentStation());
        return otpStations.get(otpParentStationId);
    }

    private TransitEntity<FeedScopedId> getOtpStop(org.onebusaway.gtfs.model.Stop stop) {
        FeedScopedId otpStopId = mapAgencyAndId(stop.getId());

        var possibleStop = otpStops.get(otpStopId);
        if (possibleStop != null) return possibleStop;

        var possibleEntrance = entrances.get(otpStopId);
        if (possibleEntrance != null) return possibleEntrance;

        var possiblePathwayNode = nodes.get(otpStopId);
        if (possiblePathwayNode != null) return possiblePathwayNode;

        return null;
    }

    private BoardingArea getOtpBoardingArea(org.onebusaway.gtfs.model.Stop stop) {
        FeedScopedId otpStopId = mapAgencyAndId(stop.getId());
        return boardingAreas.get(otpStopId);
    }
}
