package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
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

    public void mapNetexToOtp(NetexImportDataIndex netexIndex) {
        FeedScopedIdFactory.setFeedId(agencyId);

        for (Authority authority : netexIndex.authoritiesById.localValues()) {
            transitBuilder.getAgenciesById().add(agencyMapper.mapAgency(authority,
                    netexIndex.timeZone.get()));
        }

        for (Line line : netexIndex.lineById.localValues()) {
            Route route = routeMapper.mapRoute(line, transitBuilder, netexIndex,
                    netexIndex.timeZone.get());
            transitBuilder.getRoutes().add(route);
        }

            for (String stopPlaceId : netexIndex.stopPlaceById.localKeys()) {
                Collection<StopPlace> stopPlaceAllVersions = netexIndex.stopPlaceById
                        .lookup(stopPlaceId);
            if (stopPlaceAllVersions != null) {
                Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions,
                        netexIndex);
                for (Stop stop : stops) {
                    transitBuilder.getStops().add(stop);
                }
            }
        }

        for (JourneyPattern journeyPattern : netexIndex.journeyPatternsById.localValues()) {
            tripPatternMapper.mapTripPattern(journeyPattern, transitBuilder, netexIndex);
        }

        for (String serviceId : netexIndex.getCalendarServiceIds()) {
            transitBuilder.getCalendarDates().addAll(
                    mapToCalendarDates(createFeedScopedId(serviceId), netexIndex)
            );
        }
    }
}
