package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import org.opentripplanner.model.NoticeAssignable;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;

import static org.opentripplanner.netex.mapping.CalendarMapper.mapToCalendarDates;

// TODO OTP2 - Add Unit tests

/**
 * This maps a NetexImportDataIndex into an OtpTransitServiceBuilder. This is where the main mapping from NeTEx
 * objects to OTP objects happens. It loops through all the relevant NeTEx objects and calls the appropriate mapper.
 * In some cases the result is returned and added to the transit builder, while in other cases a more complex structure
 * has to be added, so the transit builder is passed into the mapper and objects are added there.
 */

public class NetexMapper {

    private final OtpTransitServiceBuilder transitBuilder;
    private final String agencyId;

    public NetexMapper(OtpTransitServiceBuilder transitBuilder, String agencyId) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;

    }

    public void mapNetexToOtp(NetexImportDataIndex netexIndex) {
        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                transitBuilder,
                transitBuilder.getRoutes(),
                netexIndex.routeById,
                netexIndex.journeyPatternsById,
                netexIndex.quayIdByStopPointRef,
                netexIndex.destinationDisplayById,
                netexIndex.serviceJourneyByPatternId,
                transitBuilder.getStops(),
                transitBuilder.getStopTimesById()
        );
        RouteMapper routeMapper = new RouteMapper(
                transitBuilder,
                netexIndex,
                netexIndex.timeZone.get()
        );
        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                netexIndex.passingTimeByStopPointId,
                netexIndex.noticeById,
                transitBuilder.getRoutes(),
                transitBuilder.getStopTimesById(),
                transitBuilder.getTripsById()
        );

        AgencyMapper agencyMapper = new AgencyMapper(netexIndex.timeZone.get());

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
            StopMapper stopMapper = new StopMapper(netexIndex.quayById);
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

        for (NoticeAssignment noticeAssignment : netexIndex.noticeAssignmentById.localValues()) {
            Multimap<NoticeAssignable, org.opentripplanner.model.Notice> noticesByElementId;

            noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
            transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
        }
    }
}
