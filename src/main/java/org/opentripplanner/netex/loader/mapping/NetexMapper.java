package org.opentripplanner.netex.loader.mapping;

import com.google.common.collect.Multimap;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.NetexImportDataIndexReadOnlyView;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.StopPlace;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// TODO OTP2 - Add Unit tests

/**
 * <p>
 * This is the ROOT mapper to map from the Netex domin model into the OTP internal model. This class delegates to
 * type/argegate specific mappers and take the result from each such mapper and add the result to the
 * {@link OtpTransitServiceBuilder}.
 * </p>
 * <p>
 * The transit builder is updated with the new OTP model entities, holding ALL entities parsed so fare including
 * previous Netex files in the same bundle. This enable the mapping code to make direct references between entities
 * in the OTP domain model.
 * </p>
 */
public class NetexMapper {

    private final OtpTransitServiceBuilder transitBuilder;
    private final String agencyId;
    private final Deduplicator deduplicator;

    /**
     * This is needed to assign a notice to a stop time. It is not part of the target OTPTransitService,
     * so we need to temporally cash this here.
     */
    private final Map<String, StopTime> stopTimesByNetexId = new HashMap<>();


    public NetexMapper(OtpTransitServiceBuilder transitBuilder, String agencyId, Deduplicator deduplicator) {
        this.transitBuilder = transitBuilder;
        this.agencyId = agencyId;
        this.deduplicator = deduplicator;
    }

    /**
     * <p>
     * This method mappes the last Netex file imported using the *local* entities in the
     * hierarchical {@link NetexImportDataIndex}.
     * </p>
     * <p>
     * Note that the order in which the elements are mapped is important. For example, if a file
     * contains Authorities, Line and Notices - they need to be mapped in that order, since
     * Route have a reference on Agency, and Notice may reference on Route.
     * </p>
     *
     * @param netexIndex The parsed Netex entities to be mapped
     */
    public void mapNetexToOtp(NetexImportDataIndexReadOnlyView netexIndex) {

        FeedScopedIdFactory.setFeedId(agencyId);
        // Be careful, the order matter. For example a Route has a reference to Agency; Hence Agency must be mapped
        // before Route - if both entities are defined in the same file.
        mapAgency(netexIndex);
        mapStopPlaceAndQuays(netexIndex);
        mapRoute(netexIndex);
        mapTripPatterns(netexIndex);
        mapCalendarDayTypes(netexIndex);
        mapNoticeAssignments(netexIndex);
    }

    private void mapAgency(NetexImportDataIndexReadOnlyView netexIndex) {
        AgencyMapper agencyMapper = new AgencyMapper(netexIndex.getTimeZone());

        for (Authority authority : netexIndex.getAuthoritiesById().localValues()) {
            Agency agency = agencyMapper.mapAgency(authority);
            transitBuilder.getAgenciesById().add(agency);
        }
    }

    private void mapStopPlaceAndQuays(NetexImportDataIndexReadOnlyView netexIndex) {
        for (String stopPlaceId : netexIndex.getStopPlaceById().localKeys()) {
            Collection<StopPlace> stopPlaceAllVersions = netexIndex.getStopPlaceById().lookup(stopPlaceId);
            StopMapper stopMapper = new StopMapper(netexIndex.getQuayById());
            Collection<Stop> stops = stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
            transitBuilder.getStops().addAll(stops);
        }
    }

    private void mapRoute(NetexImportDataIndexReadOnlyView netexIndex) {
        RouteMapper routeMapper = new RouteMapper(
                transitBuilder.getAgenciesById(),
                netexIndex,
                netexIndex.getTimeZone()
        );
        for (Line line : netexIndex.getLineById().localValues()) {
            Route route = routeMapper.mapRoute(line);
            transitBuilder.getRoutes().add(route);
        }
    }

    private void mapTripPatterns(NetexImportDataIndexReadOnlyView netexIndex) {
        TripPatternMapper tripPatternMapper = new TripPatternMapper(
                transitBuilder.getStops(),
                transitBuilder.getRoutes(),
                netexIndex.getRouteById(),
                netexIndex.getJourneyPatternsById(),
                netexIndex.getQuayIdByStopPointRef(),
                netexIndex.getDestinationDisplayById(),
                netexIndex.getServiceJourneyByPatternId(),
                deduplicator
        );

        for (JourneyPattern journeyPattern : netexIndex.getJourneyPatternsById().localValues()) {
            TripPatternMapper.Result result = tripPatternMapper.mapTripPattern(journeyPattern);

            for (Map.Entry<Trip, List<StopTime>> it : result.tripStopTimes.entrySet()) {
                transitBuilder.getStopTimesSortedByTrip().put(it.getKey(), it.getValue());
                transitBuilder.getTripsById().add(it.getKey());
            }
            for (TripPattern it : result.tripPatterns) {
                transitBuilder.getTripPatterns().put(it.stopPattern, it);
            }
            stopTimesByNetexId.putAll(result.stopTimeByNetexId);
        }
    }

    private void mapCalendarDayTypes(NetexImportDataIndexReadOnlyView netexIndex) {
        CalendarMapper calMapper = new CalendarMapper(
                netexIndex.getDayTypeAssignmentByDayTypeId(),
                netexIndex.getOperatingPeriodById(),
                netexIndex.getDayTypeById()
        );

        for (DayTypeRefsToServiceIdAdapter dayTypeRefs : netexIndex.getDayTypeRefs()) {
            Collection<ServiceCalendarDate> dates = calMapper.mapToCalendarDates(dayTypeRefs);
            transitBuilder.getCalendarDates().addAll(dates);
        }
    }

    private void mapNoticeAssignments(NetexImportDataIndexReadOnlyView netexIndex) {
        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                netexIndex.getPassingTimeByStopPointId(),
                netexIndex.getNoticeById(),
                transitBuilder.getRoutes(),
                stopTimesByNetexId,
                transitBuilder.getTripsById()
        );
        for (NoticeAssignment noticeAssignment : netexIndex.getNoticeAssignmentById().localValues()) {
            Multimap<Serializable, Notice> noticesByElementId;
            noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
            transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
        }
    }
}
