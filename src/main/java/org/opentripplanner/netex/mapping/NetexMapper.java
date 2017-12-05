package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexDao;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;
import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

public class NetexMapper {

    private final AgencyMapper agencyMapper = new AgencyMapper();
    private final RouteMapper routeMapper = new RouteMapper();
    private final StopMapper stopMapper = new StopMapper();
    private final TripPatternMapper tripPatternMapper = new TripPatternMapper();

    private final OtpTransitServiceBuilder transitBuilder;
    private final String agencyId;


    public NetexMapper(OtpTransitServiceBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
    }

    public void mapNetexToOtp(NetexDao netexDao) {
        FeedScopedIdFactory.setFeedId(agencyId);

        for (Authority authority : netexDao.getAuthorities()) {
            transitBuilder.getAgencies().add(agencyMapper.mapAgency(authority, netexDao.getTimeZone()));
        }

        for (Line line : netexDao.getLines()) {
            Route route = routeMapper.mapRoute(line, transitBuilder, netexDao, netexDao.getTimeZone());
            transitBuilder.getRoutes().add(route);
        }

        for (String stopPlaceId : netexDao.getStopPlaceIds()) {
            Collection<StopPlace> stopPlaceAllVersions = netexDao.lookupStopPlacesById(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions, netexDao);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexDao.getJourneyPatterns()) {
            tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexDao);
        }

        for (String serviceId : netexDao.getCalendarServiceIds()) {
            transitBuilder.getCalendarDates().addAll(
                    mapToCalendarDates(createFeedScopedId(serviceId), netexDao)
            );
        }
    }
}
