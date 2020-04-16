package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable bussiness logic should be moved into the Builder; hence
 * reusable for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {
    private final AgencyMapper agencyMapper;

    private final StationMapper stationMapper = new StationMapper();

    private final StopMapper stopMapper = new StopMapper();

    private final EntranceMapper entranceMapper = new EntranceMapper();

    private final PathwayNodeMapper pathwayNodeMapper = new PathwayNodeMapper();

    private final BoardingAreaMapper boardingAreaMapper = new BoardingAreaMapper();

    private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();

    private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();

    private final FeedInfoMapper feedInfoMapper = new FeedInfoMapper();

    private final ShapePointMapper shapePointMapper = new ShapePointMapper();

    private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

    private final PathwayMapper pathwayMapper = new PathwayMapper(
        stopMapper,
        entranceMapper,
        pathwayNodeMapper,
        boardingAreaMapper
    );

    private final RouteMapper routeMapper;

    private final TripMapper tripMapper;

    private final StopTimeMapper stopTimeMapper;

    private final FrequencyMapper frequencyMapper;

    private final TransferMapper transferMapper;

    private final FareRuleMapper fareRuleMapper;

    private final DataImportIssueStore issueStore;

    GTFSToOtpTransitServiceMapper(DataImportIssueStore issueStore, String feedId) {
        this.issueStore = issueStore;
        agencyMapper = new AgencyMapper(feedId);
        routeMapper = new RouteMapper(agencyMapper);
        tripMapper = new TripMapper(routeMapper);
        stopTimeMapper = new StopTimeMapper(stopMapper, tripMapper);
        frequencyMapper = new FrequencyMapper(tripMapper);
        transferMapper = new TransferMapper(
            routeMapper, stationMapper, stopMapper, tripMapper
        );
        fareRuleMapper = new FareRuleMapper(
            routeMapper, fareAttributeMapper
        );
    }

    /**
     * Map from GTFS data to the internal OTP model
     */
    public static OtpTransitServiceBuilder mapGtfsDaoToInternalTransitServiceBuilder(
        GtfsRelationalDao data, String feedId, DataImportIssueStore issueStore
    ) {
        return new GTFSToOtpTransitServiceMapper(issueStore, feedId).map(data);
    }

    private OtpTransitServiceBuilder map(GtfsRelationalDao data) {
        OtpTransitServiceBuilder builder = new OtpTransitServiceBuilder();

        builder.getAgenciesById().addAll(agencyMapper.map(data.getAllAgencies()));
        builder.getCalendarDates().addAll(serviceCalendarDateMapper.map(data.getAllCalendarDates()));
        builder.getCalendars().addAll(serviceCalendarMapper.map(data.getAllCalendars()));
        builder.getFareAttributes().addAll(fareAttributeMapper.map(data.getAllFareAttributes()));
        builder.getFareRules().addAll(fareRuleMapper.map(data.getAllFareRules()));
        builder.getFeedInfos().addAll(feedInfoMapper.map(data.getAllFeedInfos()));
        builder.getFrequencies().addAll(frequencyMapper.map(data.getAllFrequencies()));
        builder.getRoutes().addAll(routeMapper.map(data.getAllRoutes()));
        for (ShapePoint shapePoint : shapePointMapper.map(data.getAllShapePoints())) {
            builder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
        }

        mapGtfsStopsToOtpTypes(data, builder);

        builder.getPathways().addAll(pathwayMapper.map(data.getAllPathways()));
        builder.getStopTimesSortedByTrip().addAll(stopTimeMapper.map(data.getAllStopTimes()));
        builder.getTransfers().addAll(transferMapper.map(data.getAllTransfers()));
        builder.getTripsById().addAll(tripMapper.map(data.getAllTrips()));

        return builder;
    }

    private void mapGtfsStopsToOtpTypes(GtfsRelationalDao data, OtpTransitServiceBuilder builder) {
        StopToParentStationLinker stopToParentStationLinker = new StopToParentStationLinker(issueStore);
        for (org.onebusaway.gtfs.model.Stop it : data.getAllStops()) {
            if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
                Stop stop = stopMapper.map(it);
                builder.getStops().add(stop);
                stopToParentStationLinker.addStationElement(stop, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION) {
                Station station = stationMapper.map(it);
                builder.getStations().add(station);
                stopToParentStationLinker.addStation(station);
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
                Entrance entrance = entranceMapper.map(it);
                builder.getEntrances().add(entrance);
                stopToParentStationLinker.addStationElement(entrance, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
                PathwayNode pathwayNode = pathwayNodeMapper.map(it);
                builder.getPathwayNodes().add(pathwayNode);
                stopToParentStationLinker.addStationElement(pathwayNode, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA) {
                BoardingArea boardingArea = boardingAreaMapper.map(it);
                builder.getBoardingAreas().add(boardingArea);
                stopToParentStationLinker.addBoardingArea(boardingArea, it.getParentStation());
            }
        }

        stopToParentStationLinker.link();
    }
}
