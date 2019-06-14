package org.opentripplanner.netex.mapping;

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
    private final TripPatternMapper tripPatternMapper;
    private final RouteMapper routeMapper;
    private final AgencyMapper agencyMapper;
    private final StopMapper stopMapper;

    public NetexMapper(OtpTransitServiceBuilder transitBuilder, NetexImportDataIndex netexIndex, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
        tripPatternMapper = new TripPatternMapper(
                transitBuilder,
                transitBuilder.getRoutes(),
                netexIndex.routeById,
                netexIndex.journeyPatternsById,
                netexIndex.quayIdByStopPointRef,
                netexIndex.destinationDisplayById,
                netexIndex.serviceJourneyByPatternId,
                transitBuilder.getStops()
        );
        routeMapper = new RouteMapper(
                transitBuilder,
                netexIndex.networkByLineId,
                netexIndex.groupOfLinesByLineId,
                netexIndex,
                netexIndex.timeZone.get()
        );
        agencyMapper = new AgencyMapper(netexIndex.timeZone.get());
        stopMapper = new StopMapper(netexIndex.quayById);
    }

    public void mapNetexToOtp(NetexImportDataIndex netexIndex) {
        FeedScopedIdFactory.setFeedId(agencyId);

        for (Authority authority : netexIndex.authoritiesById.localValues()) {
            transitBuilder.getAgenciesById().add(
                    agencyMapper.mapAgency(authority)
            );
        }

        for (Line line : netexIndex.lineById.localValues()) {
            transitBuilder.getRoutes().add(
                    routeMapper.mapRoute(line)
            );
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
