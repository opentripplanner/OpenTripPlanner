package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

// TODO OTP2 - Add Unit tests
// TODO OTP2 - JavaDoc needed
public class NetexMapper {

    private final OtpTransitServiceBuilder transitBuilder;
    private final String agencyId;

    public NetexMapper(OtpTransitServiceBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;

    }

    public void mapNetexToOtp(NetexImportDataIndex netexIndex) {
        //TODO Consider moving this to constructor
        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                transitBuilder,
                transitBuilder.getRoutes(),
                netexIndex.routeById,
                netexIndex.journeyPatternsById,
                netexIndex.quayIdByStopPointRef,
                netexIndex.destinationDisplayById,
                netexIndex.serviceJourneyByPatternId,
                transitBuilder.getStops()
        );
        RouteMapper routeMapper = new RouteMapper(
                transitBuilder,
                netexIndex.networkByLineId,
                netexIndex.groupOfLinesByLineId,
                netexIndex,
                netexIndex.timeZone.get()
        );
        AgencyMapper agencyMapper = new AgencyMapper(netexIndex.timeZone.get());
        StopMapper stopMapper = new StopMapper(netexIndex.quayById);

        FeedScopedIdFactory.setFeedId(agencyId);

        for (Authority authority : netexIndex.authoritiesById.localValues()) {
            transitBuilder.getAgenciesById().add(
                    agencyMapper.mapAgency(authority)
            );
        }

        for (Line line : netexIndex.lineById.localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }

        for (String stopPlaceId : netexIndex.stopPlaceById.localKeys()) {
            Collection<StopPlace> stopPlaceAllVersions = netexIndex.stopPlaceById.lookup(stopPlaceId);
            Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
            transitBuilder.getStops().addAll(stops);
        }

        for (JourneyPattern journeyPattern : netexIndex.journeyPatternsById.localValues()) {
            tripPatternMapper.mapTripPattern(journeyPattern);
        }

        for (DayTypeRefsToServiceIdAdapter dayTypeRefs : netexIndex.dayTypeRefs) {
            transitBuilder.getCalendarDates().addAll(
                    mapToCalendarDates(dayTypeRefs, netexIndex)
            );
        }
    }
}
