package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;

import java.util.stream.Collectors;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable bussiness logic should be moved into the Builder; hence
 * reusable for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {
    private final AgencyMapper agencyMapper = new AgencyMapper();

    private final StationMapper stationMapper = new StationMapper();

    private final StationStopLinker stationStopLinker = new StationStopLinker();

    private final StopMapper stopMapper = new StopMapper();

    private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();

    private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();

    private final FeedInfoMapper feedInfoMapper = new FeedInfoMapper();

    private final ShapePointMapper shapePointMapper = new ShapePointMapper();

    private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

    private final PathwayMapper pathwayMapper = new PathwayMapper(stopMapper);

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


    /**
     * Map from GTFS data to the internal OTP model
     */
    public static OtpTransitServiceBuilder mapGtfsDaoToInternalTransitServiceBuilder(
            GtfsRelationalDao data
    ) {
        return new GTFSToOtpTransitServiceMapper().map(data);
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
        builder.getPathways().addAll(pathwayMapper.map(data.getAllPathways()));
        builder.getRoutes().addAll(routeMapper.map(data.getAllRoutes()));
        builder.getShapePoints().addAll(shapePointMapper.map(data.getAllShapePoints()));
        builder.getStops().addAll(stopMapper.map(
                data.getAllStops().stream().filter(s -> s.getLocationType() == 0)
                        .collect(Collectors.toList())));
        builder.getStations().addAll(stationMapper.map(
                data.getAllStops().stream().filter(s -> s.getLocationType() == 1)
                        .collect(Collectors.toList())));
        stationStopLinker.link(data.getAllStops(), builder.getStations(), builder.getStops());
        builder.getStopTimesSortedByTrip().addAll(stopTimeMapper.map(data.getAllStopTimes()));
        builder.getTransfers().addAll(transferMapper.map(data.getAllTransfers()));
        builder.getTripsById().addAll(tripMapper.map(data.getAllTrips()));

        return builder;
    }
}
