package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable bussiness logic should be moved into the Builder; hence
 * reusable for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {
    private final AgencyMapper agencyMapper = new AgencyMapper();

    private final StationMapper stationMapper = new StationMapper();

    private final StopMapper stopMapper = new StopMapper();

    private final EntranceMapper entranceMapper = new EntranceMapper();

    private final PathwayNodeMapper pathwayNodeMapper = new PathwayNodeMapper();

    private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();

    private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();

    private final FeedInfoMapper feedInfoMapper = new FeedInfoMapper();

    private final ShapePointMapper shapePointMapper = new ShapePointMapper();

    private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

    private final PathwayMapper pathwayMapper = new PathwayMapper(
        stopMapper,
        entranceMapper,
        pathwayNodeMapper
    );

    private final RouteMapper routeMapper = new RouteMapper(agencyMapper);

    private final TripMapper tripMapper = new TripMapper(routeMapper);

    private final StopTimeMapper stopTimeMapper = new StopTimeMapper(stopMapper, tripMapper);

    private final FrequencyMapper frequencyMapper = new FrequencyMapper(tripMapper);

    private final TransferMapper transferMapper = new TransferMapper(
            routeMapper, stationMapper, stopMapper, tripMapper
    );

    private final FareRuleMapper fareRuleMapper = new FareRuleMapper(
            routeMapper, fareAttributeMapper
    );

    private final DataImportIssueStore issueStore;

    GTFSToOtpTransitServiceMapper(DataImportIssueStore issueStore) {
        this.issueStore = issueStore;
    }

    /**
     * Map from GTFS data to the internal OTP model
     */
    public static OtpTransitServiceBuilder mapGtfsDaoToInternalTransitServiceBuilder(
            GtfsRelationalDao data,
            DataImportIssueStore issueStore
    ) {
        return new GTFSToOtpTransitServiceMapper(issueStore).map(data);
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
        for (Stop it : data.getAllStops()) {
            if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
                builder.getStops().add(stopMapper.map(it));
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION) {
                builder.getStations().add(stationMapper.map(it));
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
                builder.getEntrances().add(entranceMapper.map(it));
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
                builder.getPathwayNodes().add(pathwayNodeMapper.map(it));
            }
        }
        new LinkStopsAndParentStationsTogether(
                builder.getStations(),
                builder.getStops(),
                builder.getEntrances(),
                builder.getPathwayNodes(),
                issueStore
        )
            .link(data.getAllStops());
    }
}
