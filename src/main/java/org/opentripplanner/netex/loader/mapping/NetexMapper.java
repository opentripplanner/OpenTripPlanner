package org.opentripplanner.netex.loader.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.calendar.ServiceCalendarDate;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.NetexImportDataIndex;
import org.opentripplanner.netex.loader.NetexImportDataIndexReadOnlyView;
import org.opentripplanner.netex.support.DayTypeRefsToServiceIdAdapter;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.StopPlace;

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
    private final FeedScopedIdFactory idFactory;
    private final Deduplicator deduplicator;
    private final Multimap<String, Station> stationsByMultiModalStationRfs = ArrayListMultimap.create();


    private final DataImportIssueStore issueStore;

    /**
     * This is needed to assign a notice to a stop time. It is not part of the target OTPTransitService,
     * so we need to temporally cash this here.
     */
    private final Map<String, StopTime> stopTimesByNetexId = new HashMap<>();


    public NetexMapper(
            OtpTransitServiceBuilder transitBuilder,
            String agencyId,
            Deduplicator deduplicator,
            DataImportIssueStore issueStore
    ) {
        this.transitBuilder = transitBuilder;
        this.deduplicator = deduplicator;
        this.idFactory = new FeedScopedIdFactory(agencyId);
        this.issueStore = issueStore;
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
        // Be careful, the order matter. For example a Route has a reference to Agency; Hence Agency must be mapped
        // before Route - if both entities are defined in the same file.
        mapAuthorities(netexIndex);
        mapOperators(netexIndex);
        mapShapePoints(netexIndex);
        mapStopPlaceAndQuays(netexIndex);
        mapMultiModalStopPlaces(netexIndex);
        mapGroupsOfStopPlaces(netexIndex);
        mapRoute(netexIndex);
        mapTripPatterns(netexIndex);
        mapCalendarDayTypes(netexIndex);
        mapNoticeAssignments(netexIndex);
    }

    private void mapOperators(NetexImportDataIndexReadOnlyView netexIndex) {
        OperatorToAgencyMapper mapper = new OperatorToAgencyMapper(idFactory);
        for (org.rutebanken.netex.model.Operator operator : netexIndex.getOperatorsById().localValues()) {
            transitBuilder.getOperatorsById().add(mapper.mapOperator(operator));
        }
    }

    private void mapAuthorities(NetexImportDataIndexReadOnlyView netexIndex) {
        AuthorityToAgencyMapper agencyMapper = new AuthorityToAgencyMapper(idFactory, netexIndex.getTimeZone());
        for (Authority authority : netexIndex.getAuthoritiesById().localValues()) {
            Agency agency = agencyMapper.mapAuthorityToAgency(authority);
            transitBuilder.getAgenciesById().add(agency);
        }
    }

    private void mapShapePoints(NetexImportDataIndexReadOnlyView netexIndex) {
        ServiceLinkMapper serviceLinkMapper = new ServiceLinkMapper(idFactory, issueStore);
        for (JourneyPattern journeyPattern : netexIndex.getJourneyPatternsById().localValues()) {

            Collection<ShapePoint> shapePoints = serviceLinkMapper.getShapePointsByJourneyPattern(
                journeyPattern,
                netexIndex.getServiceLinkById(),
                netexIndex.getQuayIdByStopPointRef(),
                netexIndex.getQuayById());

            for (ShapePoint shapePoint : shapePoints) {
                transitBuilder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
            }
        }
    }

    private void mapStopPlaceAndQuays(NetexImportDataIndexReadOnlyView netexIndex) {
        for (String stopPlaceId : netexIndex.getStopPlaceById().localKeys()) {
            Collection<StopPlace> stopPlaceAllVersions = netexIndex.getStopPlaceById().lookup(stopPlaceId);
            StopAndStationMapper stopMapper = new StopAndStationMapper(idFactory, netexIndex.getQuayById(),
                issueStore
            );
            stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
            transitBuilder.getStops().addAll(stopMapper.resultStops);
            transitBuilder.getStations().addAll(stopMapper.resultStations);
            stationsByMultiModalStationRfs.putAll(stopMapper.resultStationByMultiModalStationRfs);
        }
    }

    private void mapMultiModalStopPlaces(NetexImportDataIndexReadOnlyView netexIndex) {
        MultiModalStationMapper mapper = new MultiModalStationMapper(idFactory);
        for (StopPlace multiModalStopPlace : netexIndex.getMultiModalStopPlaceById().localValues()) {
            transitBuilder.getMultiModalStationsById().add(
                mapper.map(
                    multiModalStopPlace,
                    stationsByMultiModalStationRfs.get(multiModalStopPlace.getId())
                )
            );
        }
    }

    private void mapGroupsOfStopPlaces(NetexImportDataIndexReadOnlyView netexIndex) {
        GroupOfStationsMapper groupOfStationsMapper = new GroupOfStationsMapper(
                idFactory,
                transitBuilder.getMultiModalStationsById(),
                transitBuilder.getStations()
        );
        for (GroupOfStopPlaces groupOfStopPlaces : netexIndex.getGroupOfStopPlacesById().localValues()) {
            transitBuilder.getGroupsOfStationsById().add(groupOfStationsMapper.map(groupOfStopPlaces));
        }
    }

    private void mapRoute(NetexImportDataIndexReadOnlyView netexIndex) {
        RouteMapper routeMapper = new RouteMapper(
                idFactory,
                transitBuilder.getAgenciesById(),
                transitBuilder.getOperatorsById(),
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
                idFactory,
                transitBuilder.getStops(),
                transitBuilder.getRoutes(),
                transitBuilder.getShapePoints().keySet(),
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
                idFactory,
                netexIndex.getDayTypeAssignmentByDayTypeId(),
                netexIndex.getOperatingPeriodById(),
                netexIndex.getDayTypeById(), issueStore
        );

        for (DayTypeRefsToServiceIdAdapter dayTypeRefs : netexIndex.getDayTypeRefs()) {
            Collection<ServiceCalendarDate> dates = calMapper.mapToCalendarDates(dayTypeRefs);
            transitBuilder.getCalendarDates().addAll(dates);
        }
    }

    private void mapNoticeAssignments(NetexImportDataIndexReadOnlyView netexIndex) {
        NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
                idFactory,
                netexIndex.getPassingTimeByStopPointId(),
                netexIndex.getNoticeById(),
                transitBuilder.getRoutes(),
                transitBuilder.getTripsById(),
                stopTimesByNetexId
        );
        for (NoticeAssignment noticeAssignment : netexIndex.getNoticeAssignmentById().localValues()) {
            Multimap<TransitEntity<?>, Notice> noticesByElementId;
            noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
            transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
        }
    }
}
