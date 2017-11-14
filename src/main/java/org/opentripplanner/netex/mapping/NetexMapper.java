package org.opentripplanner.netex.mapping;

import org.opentripplanner.graph_builder.model.NetexDao;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

public class NetexMapper {

    final OtpTransitServiceBuilder transitBuilder;

    AgencyMapper agencyMapper = new AgencyMapper();

    RouteMapper routeMapper = new RouteMapper();

    StopMapper stopMapper = new StopMapper();

    TripPatternMapper tripPatternMapper = new TripPatternMapper();


    public NetexMapper(OtpTransitServiceBuilder transitBuilder) {
        this.transitBuilder = transitBuilder;
    }

    public OtpTransitServiceBuilder mapNetexToOtp(NetexDao netexDao) {
        for (Operator operator : netexDao.getOperators().values()) {
            if (operator != null) {
                transitBuilder.getAgencies().add(agencyMapper.mapAgency(operator, "Europe/Oslo"));
            }
        }

        for (Line line : netexDao.getLineById().values()) {
            if (line != null) {
                Route route = routeMapper.mapRoute(line, transitBuilder);
                transitBuilder.getRoutes().add(route);
            }
        }

        for (String stopPlaceId : netexDao.getStopsById().keySet()) {
            Collection<StopPlace> stopPlaceAllVersions = netexDao.getStopsById().get(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions, netexDao.getPare.);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexDao.getJourneyPatternsById().values()) {
            if (journeyPattern != null) {
                tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexDao);
            } else {
                int i = 0;
            }
        }

        for (String serviceId : netexDao.getServiceIds().values()) {
            transitBuilder.getCalendarDates().addAll(mapToCalendarDates(FeedScopedIdFactory.createAgencyAndId(serviceId), netexDao));
        }

        return transitBuilder;
    }
}
