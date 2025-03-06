package org.opentripplanner.netex.mapping;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import jakarta.xml.bind.JAXBElement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.ServiceCalendar;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.index.api.NetexEntityIndexReadOnlyView;
import org.opentripplanner.netex.mapping.calendar.CalendarServiceBuilder;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMapperIndexes;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.Branding;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.rutebanken.netex.model.GroupOfStopPlaces;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.LineRefs_RelStructure;
import org.rutebanken.netex.model.NoticeAssignment;
import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VersionOfObjectRefStructure;

/**
 * <p>
 * This is the ROOT mapper to map from the Netex domin model into the OTP internal model. This class
 * delegates to type/argegate specific mappers and take the result from each such mapper and add the
 * result to the {@link OtpTransitServiceBuilder}.
 * </p>
 * <p>
 * The transit builder is updated with the new OTP model entities, holding ALL entities parsed so
 * fare including previous Netex files in the same bundle. This enable the mapping code to make
 * direct references between entities in the OTP domain model.
 * </p>
 */
public class NetexMapper {

  private static final int LEVEL_SHARED = 0;
  private static final int LEVEL_GROUP = 1;

  private final FeedScopedIdFactory idFactory;
  private final OtpTransitServiceBuilder transitBuilder;
  private final Deduplicator deduplicator;
  private final DataImportIssueStore issueStore;
  private final CalendarServiceBuilder calendarServiceBuilder;
  private final TripCalendarBuilder tripCalendarBuilder;
  private final Set<String> ferryIdsNotAllowedForBicycle;
  private final Set<FeedScopedId> routeToCentroidStopPlaceIds;
  private final double maxStopToShapeSnapDistance;
  private final boolean noTransfersOnIsolatedStops;

  /** Map entries that cross reference entities within a group/operator, for example Interchanges. */
  private GroupNetexMapper groupMapper;

  /** All read netex entities by their id */
  private NetexEntityIndexReadOnlyView currentNetexIndex;

  /**
   * Shared/cached entity index, used by more than one mapper. This index provides alternative
   * indexes to netex entities, as well as global indexes to OTP domain objects needed in the mapping
   * process. Some of these indexes are feed scoped, and some are file group level scoped. As a rule
   * of thumb the indexes for OTP Model entities are global(small memory overhead), while the indexes
   * for the Netex entities follow the main index {@link  #currentNetexIndex}, hence sopped by file
   * group.
   */
  private NetexMapperIndexes currentMapperIndexes = null;

  private int level = LEVEL_SHARED;

  public NetexMapper(
    OtpTransitServiceBuilder transitBuilder,
    String feedId,
    Deduplicator deduplicator,
    DataImportIssueStore issueStore,
    Set<String> ferryIdsNotAllowedForBicycle,
    Collection<FeedScopedId> routeToCentroidStopPlaceIds,
    double maxStopToShapeSnapDistance,
    boolean noTransfersOnIsolatedStops
  ) {
    this.transitBuilder = transitBuilder;
    this.deduplicator = deduplicator;
    this.idFactory = new FeedScopedIdFactory(feedId);
    this.issueStore = issueStore;
    this.ferryIdsNotAllowedForBicycle = ferryIdsNotAllowedForBicycle;
    this.routeToCentroidStopPlaceIds = Set.copyOf(routeToCentroidStopPlaceIds);
    this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    this.calendarServiceBuilder = new CalendarServiceBuilder(idFactory);
    this.tripCalendarBuilder = new TripCalendarBuilder(this.calendarServiceBuilder, issueStore);
  }

  /**
   * Prepare to for mapping of a new sub-level of entities(shared-files, shared-group-files and
   * group-files). This is a life-cycle method used to notify this class that a new dataset is about
   * to be processed. Any existing intermediate state must be saved, so it can be accessed during
   * the next call to {@link #mapNetexToOtp(NetexEntityIndexReadOnlyView)} and after.
   */
  public NetexMapper push() {
    ++level;
    this.tripCalendarBuilder.push();
    setupGroupMapping();
    return this;
  }

  /**
   * It is now safe to discard any intermediate state generated by the last call to {@link
   * #mapNetexToOtp(NetexEntityIndexReadOnlyView)}.
   */
  public NetexMapper pop() {
    performGroupMapping();
    this.tripCalendarBuilder.pop();
    // A new mapper is created for every call to {@link #mapNetexToOtp}
    this.currentMapperIndexes = currentMapperIndexes.getParent();
    --level;
    return this;
  }

  /**
   * Any post-processing step in the mapping is done in this method. The method is called ONCE after
   * all other mapping is complete. Note! Hierarchical data structures are not accessible anymore.
   */
  public void finishUp() {
    // Add Calendar data created during the mapping of dayTypes, dayTypeAssignments,
    // datedServiceJourney and ServiceJourneys
    transitBuilder.getCalendarDates().addAll(calendarServiceBuilder.createServiceCalendar());

    // Add the empty service id, as it can be used for routes expected to be added from realtime
    // updates or DSJs which are replaced, and where we want to keep the original DSJ
    ServiceCalendar emptyCalendar = calendarServiceBuilder.createEmptyCalendar();
    if (
      transitBuilder
        .getTripsById()
        .values()
        .stream()
        .anyMatch(trip -> emptyCalendar.getServiceId().equals(trip.getServiceId()))
    ) {
      transitBuilder.getCalendars().add(emptyCalendar);
    }
  }

  /**
   * <p>
   * This method maps the last Netex file imported using the *local* entities in the hierarchical
   * {@link NetexEntityIndexReadOnlyView}.
   * </p>
   * <p>
   * Note that the order in which the elements are mapped is important. For example, if a file
   * contains Authorities, Line and Notices - they need to be mapped in that order, since Route have
   * a reference on Agency, and Notice may reference on Route.
   * </p>
   *
   * @param netexIndex The parsed Netex entities to be mapped
   */
  public void mapNetexToOtp(NetexEntityIndexReadOnlyView netexIndex) {
    this.currentNetexIndex = netexIndex;
    this.currentMapperIndexes = new NetexMapperIndexes(netexIndex, currentMapperIndexes);

    // Be careful, the order matter. For example a Route has a reference to Agency; Hence Agency must be mapped
    // before Route - if both entities are defined in the same file.

    mapAuthorities();
    mapOperators();
    mapBrandings();

    // The tariffZoneMapper is used to map all currently valid zones and to map the correct
    // referenced zone in StopPlace - which may not be the most currently valid zone.
    // This is a workaround until versioned entities are supported by OTP
    var tariffZoneMapper = mapTariffZones();
    mapStopPlaceAndQuays(tariffZoneMapper);
    mapMultiModalStopPlaces();
    mapGroupsOfStopPlaces();
    mapFlexibleStopPlaces();
    addDatedServiceJourneysToTripCalendar();
    mapDayTypeAssignments();

    // DayType and DSJ is mapped to a service calendar and a serviceId is generated
    Map<String, FeedScopedId> serviceIds = createCalendarForServiceJourney();

    mapRoute();
    mapGroupsOfLines();
    mapTripPatterns(serviceIds);
    mapNoticeAssignments();

    mapScheduledStopPointsToQuays();
    mapVehicleParkings();

    addEntriesToGroupMapperForPostProcessingLater();
  }

  /* PRIVATE METHODS */

  private void setupGroupMapping() {
    if (level != LEVEL_GROUP) {
      return;
    }
    this.groupMapper = new GroupNetexMapper(idFactory, issueStore, transitBuilder);
  }

  /**
   * Group mappings should be done after all individual processed files and most entities are
   * mapped. The group mapping should only be used to map entities(relations) that reference
   * elements in other files within a group(netex namespace);
   */
  private void performGroupMapping() {
    if (level != LEVEL_GROUP) {
      return;
    }
    this.groupMapper.mapGroupEntries();
    // Throw away group data and make it available for garbage collection
    this.groupMapper = null;
  }

  private void mapAuthorities() {
    AuthorityToAgencyMapper agencyMapper = new AuthorityToAgencyMapper(
      idFactory,
      currentNetexIndex.getTimeZone()
    );
    for (Authority authority : currentNetexIndex.getAuthoritiesById().localValues()) {
      Agency agency = agencyMapper.mapAuthorityToAgency(authority);
      transitBuilder.getAgenciesById().add(agency);
    }
  }

  private void mapBrandings() {
    BrandingMapper mapper = new BrandingMapper(idFactory);
    for (Branding branding : currentNetexIndex.getBrandingById().localValues()) {
      transitBuilder.getBrandingsById().add(mapper.mapBranding(branding));
    }
  }

  private void mapGroupsOfLines() {
    GroupOfRoutesMapper mapper = new GroupOfRoutesMapper(idFactory);

    currentNetexIndex
      .getGroupsOfLinesById()
      .localValues()
      .forEach(gol -> {
        GroupOfRoutes model = mapper.mapGroupOfRoutes(gol);

        Optional.ofNullable(gol.getMembers())
          .stream()
          .map(LineRefs_RelStructure::getLineRef)
          .filter(Objects::nonNull)
          .flatMap(Collection::stream)
          .filter(Objects::nonNull)
          .map(JAXBElement::getValue)
          .filter(Objects::nonNull)
          .map(VersionOfObjectRefStructure::getRef)
          .filter(Objects::nonNull)
          .forEach(ref -> {
            FeedScopedId routeId = idFactory.createId(ref);
            // At this point no routes are created yet
            // So we put all group of lines in multimap
            // RouteMapper can then use this map to populate Routes with correct GroupsOfLines
            transitBuilder.getGroupsOfRoutesByRouteId().put(routeId, model);
          });

        // Create this index as well
        // In case relation is set on Line
        transitBuilder.getGroupOfRouteById().add(model);
      });
  }

  private void mapOperators() {
    OperatorToAgencyMapper mapper = new OperatorToAgencyMapper(issueStore, idFactory);
    for (org.rutebanken.netex.model.Operator operator : currentNetexIndex
      .getOperatorsById()
      .localValues()) {
      transitBuilder.getOperatorsById().add(mapper.mapOperator(operator));
    }
  }

  private TariffZoneMapper mapTariffZones() {
    TariffZoneMapper tariffZoneMapper = new TariffZoneMapper(
      getStartOfPeriod(),
      idFactory,
      currentNetexIndex.getTariffZonesById()
    );
    transitBuilder.getFareZonesById().addAll(tariffZoneMapper.listAllCurrentFareZones());
    return tariffZoneMapper;
  }

  private void mapStopPlaceAndQuays(TariffZoneMapper tariffZoneMapper) {
    String timeZone = currentNetexIndex.getTimeZone();
    ZoneId zoneId = timeZone != null ? ZoneId.of(timeZone) : null;

    StopAndStationMapper stopMapper = new StopAndStationMapper(
      idFactory,
      currentNetexIndex.getQuayById(),
      tariffZoneMapper,
      transitBuilder.siteRepository(),
      zoneId,
      issueStore,
      noTransfersOnIsolatedStops,
      routeToCentroidStopPlaceIds
    );
    for (String stopPlaceId : currentNetexIndex.getStopPlaceById().localKeys()) {
      Collection<StopPlace> stopPlaceAllVersions = currentNetexIndex
        .getStopPlaceById()
        .lookup(stopPlaceId);
      stopMapper.mapParentAndChildStops(stopPlaceAllVersions);
    }
    transitBuilder.siteRepository().withRegularStops(stopMapper.resultStops);
    transitBuilder.siteRepository().withStations(stopMapper.resultStations);
    currentMapperIndexes.addStationByMultiModalStationRfs(
      stopMapper.resultStationByMultiModalStationRfs
    );
  }

  private void mapMultiModalStopPlaces() {
    MultiModalStationMapper mapper = new MultiModalStationMapper(issueStore, idFactory);

    for (StopPlace multiModalStopPlace : currentNetexIndex
      .getMultiModalStopPlaceById()
      .localValues()) {
      var stations = currentMapperIndexes
        .getStationsByMultiModalStationRfs()
        .get(multiModalStopPlace.getId());
      var multiModalStation = mapper.map(multiModalStopPlace, stations);
      if (multiModalStation != null) {
        transitBuilder.siteRepository().withMultiModalStation(multiModalStation);
      }
    }
  }

  private void mapGroupsOfStopPlaces() {
    GroupOfStationsMapper groupOfStationsMapper = new GroupOfStationsMapper(
      issueStore,
      idFactory,
      transitBuilder.getMultiModalStationsById(),
      transitBuilder.getStations()
    );
    for (GroupOfStopPlaces groupOfStopPlaces : currentNetexIndex
      .getGroupOfStopPlacesById()
      .localValues()) {
      transitBuilder
        .siteRepository()
        .withGroupOfStation(groupOfStationsMapper.map(groupOfStopPlaces));
    }
  }

  private void mapFlexibleStopPlaces() {
    Collection<FlexibleStopPlace> flexibleStopPlaces = currentNetexIndex
      .getFlexibleStopPlacesById()
      .localValues();

    // Building the indices in FlexStopLocationMapper is expensive, so skip it if not needed
    if (flexibleStopPlaces.size() == 0) {
      return;
    }

    FlexStopsMapper flexStopsMapper = new FlexStopsMapper(
      idFactory,
      transitBuilder.getStops().values(),
      transitBuilder.siteRepository(),
      issueStore
    );

    for (FlexibleStopPlace flexibleStopPlace : flexibleStopPlaces) {
      StopLocation stopLocation = flexStopsMapper.map(flexibleStopPlace);
      if (stopLocation instanceof AreaStop areaStop) {
        transitBuilder.siteRepository().withAreaStop(areaStop);
      } else if (stopLocation instanceof GroupStop groupStop) {
        transitBuilder.siteRepository().withGroupStop(groupStop);
        for (var child : groupStop.getChildLocations()) {
          if (child instanceof AreaStop areaStop) {
            transitBuilder.siteRepository().withAreaStop(areaStop);
          }
        }
      }
    }
  }

  private void addDatedServiceJourneysToTripCalendar() {
    tripCalendarBuilder.addDatedServiceJourneys(
      currentNetexIndex.getOperatingDayById(),
      currentMapperIndexes.getDatedServiceJourneysBySjId()
    );
  }

  private void mapDayTypeAssignments() {
    tripCalendarBuilder.addDayTypeAssignments(
      currentNetexIndex.getDayTypeById(),
      currentNetexIndex.getDayTypeAssignmentByDayTypeId(),
      currentNetexIndex.getOperatingDayById(),
      currentNetexIndex.getOperatingPeriodById()
    );
  }

  private Map<String, FeedScopedId> createCalendarForServiceJourney() {
    return tripCalendarBuilder.createTripCalendar(
      currentNetexIndex.getServiceJourneyById().localValues()
    );
  }

  private void mapRoute() {
    RouteMapper routeMapper = new RouteMapper(
      issueStore,
      idFactory,
      transitBuilder.getAgenciesById(),
      transitBuilder.getOperatorsById(),
      transitBuilder.getBrandingsById(),
      transitBuilder.getGroupsOfRoutesByRouteId(),
      transitBuilder.getGroupOfRouteById(),
      currentNetexIndex,
      currentNetexIndex.getTimeZone(),
      ferryIdsNotAllowedForBicycle
    );
    for (Line line : currentNetexIndex.getLineById().localValues()) {
      Route route = routeMapper.mapRoute(line);
      if (route != null) {
        transitBuilder.getRoutes().add(route);
      }
    }
    for (FlexibleLine line : currentNetexIndex.getFlexibleLineById().localValues()) {
      Route route = routeMapper.mapRoute(line);
      if (route != null) {
        transitBuilder.getRoutes().add(route);
      }
    }
  }

  private void mapTripPatterns(Map<String, FeedScopedId> serviceIds) {
    TripPatternMapper tripPatternMapper = new TripPatternMapper(
      issueStore,
      idFactory,
      transitBuilder.getOperatorsById(),
      transitBuilder.siteRepository().regularStopsById(),
      transitBuilder.siteRepository().areaStopById(),
      transitBuilder.siteRepository().groupStopById(),
      transitBuilder.getRoutes(),
      currentNetexIndex.getRouteById(),
      currentNetexIndex.getJourneyPatternsById(),
      currentNetexIndex.getQuayIdByStopPointRef(),
      currentNetexIndex.getFlexibleStopPlaceByStopPointRef(),
      currentNetexIndex.getDestinationDisplayById(),
      currentNetexIndex.getServiceJourneyById(),
      currentNetexIndex.getServiceLinkById(),
      currentNetexIndex.getFlexibleLineById(),
      currentNetexIndex.getOperatingDayById(),
      currentNetexIndex.getDatedServiceJourneys(),
      currentMapperIndexes.getDatedServiceJourneysBySjId(),
      serviceIds,
      deduplicator,
      maxStopToShapeSnapDistance
    );

    for (JourneyPattern_VersionStructure journeyPattern : currentNetexIndex
      .getJourneyPatternsById()
      .localValues()) {
      tripPatternMapper
        .mapTripPattern(journeyPattern)
        .ifPresent(this::applyTripPatternMapperResult);
    }
  }

  private void applyTripPatternMapperResult(TripPatternMapperResult result) {
    var stopPattern = result.tripPattern().getStopPattern();
    var journeyPatternExists = transitBuilder
      .getTripPatterns()
      .get(stopPattern)
      .stream()
      .anyMatch(tripPattern -> result.tripPattern().getId().equals(tripPattern.getId()));
    if (journeyPatternExists) {
      issueStore.add(
        "DuplicateJourneyPattern",
        "Duplicate of JourneyPattern %s found",
        result.tripPattern().getId().getId()
      );
    }

    for (Map.Entry<Trip, List<StopTime>> it : result.tripStopTimes().entrySet()) {
      transitBuilder.getStopTimesSortedByTrip().put(it.getKey(), it.getValue());
      transitBuilder.getTripsById().add(it.getKey());
    }

    transitBuilder.getTripPatterns().put(stopPattern, result.tripPattern());
    currentMapperIndexes.addStopTimesByNetexId(result.stopTimeByNetexId());
    groupMapper.scheduledStopPointsIndex.putAll(Multimaps.asMap(result.scheduledStopPointsIndex()));
    transitBuilder.getTripOnServiceDates().addAll(result.tripOnServiceDates());
  }

  private void mapNoticeAssignments() {
    NoticeAssignmentMapper noticeAssignmentMapper = new NoticeAssignmentMapper(
      issueStore,
      idFactory,
      currentNetexIndex.getServiceJourneyById().localValues(),
      currentNetexIndex.getNoticeById(),
      transitBuilder.getRoutes(),
      transitBuilder.getTripsById(),
      currentMapperIndexes.getStopTimesByNetexId()
    );
    for (NoticeAssignment noticeAssignment : currentNetexIndex
      .getNoticeAssignmentById()
      .localValues()) {
      Multimap<AbstractTransitEntity, Notice> noticesByElementId;
      noticesByElementId = noticeAssignmentMapper.map(noticeAssignment);
      transitBuilder.getNoticeAssignments().putAll(noticesByElementId);
    }
  }

  private void addEntriesToGroupMapperForPostProcessingLater() {
    if (level != 0) {
      groupMapper.addInterchange(
        currentNetexIndex.getServiceJourneyInterchangeById().localValues()
      );
    }
  }

  private void mapScheduledStopPointsToQuays() {
    currentNetexIndex
      .getQuayIdByStopPointRef()
      .localKeys()
      .forEach(id -> {
        var sspid = idFactory.createId(id);
        var stopId = idFactory.createId(currentNetexIndex.getQuayIdByStopPointRef().lookup(id));
        var stop = Objects.requireNonNull(transitBuilder.getStops().get(stopId));
        transitBuilder.addStopByScheduledStopPoint(sspid, stop);
      });
  }

  private void mapVehicleParkings() {
    var mapper = new VehicleParkingMapper(idFactory, issueStore);
    currentNetexIndex
      .getParkingsById()
      .localKeys()
      .forEach(id -> {
        var parking = mapper.map(currentNetexIndex.getParkingsById().lookup(id));
        if (parking != null) {
          transitBuilder.vehicleParkings().add(parking);
        }
      });
  }

  /**
   * The start of period is used to find the valid entities based on the current time. This should
   * probably be configurable in the future, or even better incorporate the version number into the
   * entity id, so we can operate with more than one version of an entity in OTPs internal model.
   */
  private LocalDateTime getStartOfPeriod() {
    String timeZone = currentNetexIndex.getTimeZone();
    if (timeZone == null) {
      LocalDateTime time = LocalDateTime.now(ZoneId.of("UTC"));
      issueStore.add(
        "NetexImportTimeZone",
        "No timezone set for the current NeTEx input data file. The import " +
        "start-of-period is set to " +
        time +
        " UTC, used to check entity validity " +
        "periods."
      );
      return time;
    }
    return LocalDateTime.now(ZoneId.of(timeZone));
  }
}
