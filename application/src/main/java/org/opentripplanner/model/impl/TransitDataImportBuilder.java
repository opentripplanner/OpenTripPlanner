package org.opentripplanner.model.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.core.model.time.TimePenalty;
import org.opentripplanner.ext.flex.FlexTripsMapper;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.ValidateAndInterpolateStopTimesForEachTrip;
import org.opentripplanner.gtfs.mapping.StaySeatedNotAllowed;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.Frequency;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.TransitDataImport;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.service.vehicleparking.model.VehicleParking;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.DeferredConstrainedTransferPoint;
import org.opentripplanner.transfer.constrained.model.PendingConstrainedTransfer;
import org.opentripplanner.transfer.constrained.model.TransferPoint;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.calendar.TripCalendars;
import org.opentripplanner.transit.model.calendar.TripCalendarsBuilder;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.ImmutableEntityById;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.FareZone;
import org.opentripplanner.transit.model.site.MultiModalStation;
import org.opentripplanner.transit.model.site.Pathway;
import org.opentripplanner.transit.model.site.PathwayNode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.transit.service.SiteRepositoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for building a {@link TransitDataImport}. The instance returned by the
 * {@link #build()} method is read only, and this class provide a mutable collections to construct a
 * {@link TransitDataImport} instance.
 */
public class TransitDataImportBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(TransitDataImportBuilder.class);

  /**
   * Whether/how {@link #build()} should validate the accumulated stop times before generating
   * trip patterns. {@code SKIP} preserves the pre-{@code build()} behavior relied on by tests that
   * construct a builder directly and never route it through {@link
   * org.opentripplanner.gtfs.graphbuilder.GtfsModule} or {@link
   * org.opentripplanner.netex.NetexModule}.
   */
  public enum StopTimeValidation {
    SKIP,
    INTERPOLATE,
    VALIDATE,
  }

  private final EntityById<Agency> agenciesById = new DefaultEntityById<>();

  private final TripCalendarsBuilder tripCalendars;

  private final List<FeedInfo> feedInfos = new ArrayList<>();

  private final List<Frequency> frequencies = new ArrayList<>();

  private final SiteRepositoryBuilder siteRepositoryBuilder;

  private final Multimap<AbstractTransitEntity, Notice> noticeAssignments =
    ArrayListMultimap.create();

  private final EntityById<Operator> operatorsById = new DefaultEntityById<>();

  private final List<Pathway> pathways = new ArrayList<>();

  private final EntityById<Route> routesById = new DefaultEntityById<>();

  private final Map<FeedScopedId, Iterable<ShapePoint>> shapePoints = new HashMap<>();

  private final EntityById<PathwayNode> pathwayNodesById = new DefaultEntityById<>();

  private final EntityById<BoardingArea> boardingAreasById = new DefaultEntityById<>();

  private final TripStopTimes stopTimesByTrip = new TripStopTimes();

  private final Map<Trip, TimePenalty> flexTimePenalties = new HashMap<>();

  private final EntityById<FareZone> fareZonesById = new DefaultEntityById<>();

  private final List<ConstrainedTransfer> transfers = new ArrayList<>();

  private final List<PendingConstrainedTransfer> pendingTransfers = new ArrayList<>();

  private final List<StaySeatedNotAllowed> staySeatedNotAllowed = new ArrayList<>();

  private final EntityById<Trip> tripsById = new DefaultEntityById<>();

  private final Multimap<StopPattern, TripPattern> tripPatterns = ArrayListMultimap.create();

  private final EntityById<FlexTrip<?, ?>> flexTripsById = new DefaultEntityById<>();

  private final EntityById<Branding> brandingsById = new DefaultEntityById<>();

  private final Multimap<FeedScopedId, GroupOfRoutes> groupsOfRoutesByRouteId =
    ArrayListMultimap.create();

  private final EntityById<TripOnServiceDate> tripOnServiceDates = new DefaultEntityById<>();

  private final EntityById<GroupOfRoutes> groupOfRouteById = new DefaultEntityById<>();

  private final List<VehicleParking> vehicleParkings = new ArrayList<>();

  private final Map<FeedScopedId, RegularStop> stopsByScheduledStopPoints = new HashMap<>();

  private final DataImportIssueStore issueStore;

  private final StopTimeValidation stopTimeValidation;

  private final TripPatternGenerator patternGenerator;

  private boolean hasFrequencyBasedTrips = false;

  private boolean hasScheduledTrips = false;

  /**
   * Create a builder with no transit period limit - see
   * {@link #TransitDataImportBuilder(SiteRepository, DataImportIssueStore, LocalDateRange)}.
   */
  public TransitDataImportBuilder(SiteRepository siteRepository, DataImportIssueStore issueStore) {
    this(siteRepository, issueStore, LocalDateRange.ofUnbounded());
  }

  /**
   * @param transitPeriodLimit dates outside this period are dropped as calendar data is added
   * (see build-config's {@code transitServiceStart}/{@code transitServiceEnd}).
   */
  public TransitDataImportBuilder(
    SiteRepository siteRepository,
    DataImportIssueStore issueStore,
    LocalDateRange transitPeriodLimit
  ) {
    this(
      siteRepository,
      issueStore,
      transitPeriodLimit,
      StopTimeValidation.SKIP,
      TripPatternGenerator.NONE
    );
  }

  /**
   * @param transitPeriodLimit dates outside this period are dropped as calendar data is added
   * (see build-config's {@code transitServiceStart}/{@code transitServiceEnd}).
   * @param stopTimeValidation whether/how {@link #build()} should validate stop times before
   * generating trip patterns - see {@link StopTimeValidation}.
   * @param patternGenerator generates {@code TripPattern}s as the last step of {@link #build()} -
   * see {@link TripPatternGenerator}.
   */
  public TransitDataImportBuilder(
    SiteRepository siteRepository,
    DataImportIssueStore issueStore,
    LocalDateRange transitPeriodLimit,
    StopTimeValidation stopTimeValidation,
    TripPatternGenerator patternGenerator
  ) {
    this.siteRepositoryBuilder = siteRepository.withContext();
    this.issueStore = issueStore;
    this.tripCalendars = TripCalendars.of(transitPeriodLimit);
    this.stopTimeValidation = stopTimeValidation;
    this.patternGenerator = patternGenerator;
    if (!transitPeriodLimit.isUnbounded()) {
      LOG.info("Limiting transit service to time period: {}", transitPeriodLimit);
    }
  }

  /* Accessors */

  public EntityById<Agency> getAgenciesById() {
    return agenciesById;
  }

  /**
   * The builder used to accumulate scheduled calendar data for this feed - one
   * {@link TripCalendarsBuilder#addWeeklyCalendar} call per GTFS {@code calendar.txt} row, plus
   * one {@link TripCalendarsBuilder#addServiceDate}/{@link TripCalendarsBuilder#removeServiceDate}
   * call per {@code calendar_dates.txt} row (or NeTEx equivalent).
   */
  public TripCalendarsBuilder tripCalendars() {
    return tripCalendars;
  }

  public List<FeedInfo> getFeedInfos() {
    return feedInfos;
  }

  public List<Frequency> getFrequencies() {
    return frequencies;
  }

  public SiteRepositoryBuilder siteRepository() {
    return siteRepositoryBuilder;
  }

  public ImmutableEntityById<MultiModalStation> getMultiModalStationsById() {
    return siteRepositoryBuilder.multiModalStationById();
  }

  /**
   * get multimap of Notices by the TransitEntity id (Multiple types; hence the Serializable).
   * Entities that might have Notices are Routes, Trips, Stops and StopTimes.
   */
  public Multimap<AbstractTransitEntity, Notice> getNoticeAssignments() {
    return noticeAssignments;
  }

  public EntityById<Operator> getOperatorsById() {
    return operatorsById;
  }

  public List<Pathway> getPathways() {
    return pathways;
  }

  public EntityById<Route> getRoutes() {
    return routesById;
  }

  public Map<FeedScopedId, Iterable<ShapePoint>> getShapePoints() {
    return shapePoints;
  }

  public ImmutableEntityById<Station> getStations() {
    return siteRepositoryBuilder.stationById();
  }

  public ImmutableEntityById<RegularStop> getStops() {
    return siteRepositoryBuilder.regularStopsById();
  }

  public EntityById<PathwayNode> getPathwayNodes() {
    return pathwayNodesById;
  }

  public EntityById<BoardingArea> getBoardingAreas() {
    return boardingAreasById;
  }

  public ImmutableEntityById<AreaStop> getAreaStops() {
    return siteRepositoryBuilder.areaStopById();
  }

  public TripStopTimes getStopTimesSortedByTrip() {
    return stopTimesByTrip;
  }

  public Map<Trip, TimePenalty> getFlexTimePenalty() {
    return flexTimePenalties;
  }

  public EntityById<FareZone> getFareZonesById() {
    return fareZonesById;
  }

  public List<ConstrainedTransfer> getTransfers() {
    return transfers;
  }

  /**
   * Transfers whose endpoints still need a stop position resolved against a trip's final,
   * post-filtering stop-time sequence - see {@link DeferredConstrainedTransferPoint}. Resolved into
   * {@link #getTransfers()} by {@link #build()}.
   */
  public List<PendingConstrainedTransfer> getPendingTransfers() {
    return pendingTransfers;
  }

  public List<StaySeatedNotAllowed> getStaySeatedNotAllowed() {
    return staySeatedNotAllowed;
  }

  public EntityById<Trip> getTripsById() {
    return tripsById;
  }

  public Multimap<StopPattern, TripPattern> getTripPatterns() {
    return tripPatterns;
  }

  public EntityById<FlexTrip<?, ?>> getFlexTripsById() {
    return flexTripsById;
  }

  public EntityById<Branding> getBrandingsById() {
    return brandingsById;
  }

  public Multimap<FeedScopedId, GroupOfRoutes> getGroupsOfRoutesByRouteId() {
    return groupsOfRoutesByRouteId;
  }

  public EntityById<GroupOfRoutes> getGroupOfRouteById() {
    return groupOfRouteById;
  }

  public EntityById<TripOnServiceDate> getTripOnServiceDates() {
    return tripOnServiceDates;
  }

  /**
   * The list of parking lots contained in the transit data (so far only NeTEx). Note that parking
   * lots can also be sourced from OSM data as well as realtime updaters.
   */
  public List<VehicleParking> vehicleParkings() {
    return vehicleParkings;
  }

  /**
   * @see org.opentripplanner.transit.service.TransitRepository#findStopByScheduledStopPoint(FeedScopedId)
   */
  public Map<FeedScopedId, RegularStop> stopsByScheduledStopPoints() {
    return stopsByScheduledStopPoints;
  }

  /**
   * Set by the {@link TripPatternGenerator} passed in at construction time. Exposed on the built
   * {@link TransitDataImport} - see {@link TransitDataImport#hasFrequencyBasedTrips()}/
   * {@link TransitDataImport#hasScheduledTrips()} - rather than read back off this builder.
   */
  public void markServiceType(boolean hasFrequencyBasedTrips, boolean hasScheduledTrips) {
    this.hasFrequencyBasedTrips = this.hasFrequencyBasedTrips || hasFrequencyBasedTrips;
    this.hasScheduledTrips = this.hasScheduledTrips || hasScheduledTrips;
  }

  boolean hasFrequencyBasedTrips() {
    return hasFrequencyBasedTrips;
  }

  boolean hasScheduledTrips() {
    return hasScheduledTrips;
  }

  /**
   * Build the {@link TransitDataImport}. Since calendar data is clipped to this builder's transit
   * period limit as it is added (see the constructor), any trip/stopTime/pattern/transfer/
   * tripOnServiceDate/flexTrip left referencing a service id with no remaining dates is removed
   * first - before flex trips are created, stop times are validated/interpolated, pending
   * transfers are resolved, and trip patterns are generated, in that order. A second,
   * safety-net prune then catches anything those later steps created that references a since
   * pruned trip.
   */
  public TransitDataImport build() {
    removeEntitiesWithInvalidReferences();
    if (OTPFeature.FlexRouting.isOn()) {
      flexTripsById.addAll(FlexTripsMapper.createFlexTrips(this, issueStore));
    }
    if (stopTimeValidation != StopTimeValidation.SKIP) {
      new ValidateAndInterpolateStopTimesForEachTrip(
        stopTimesByTrip,
        stopTimeValidation == StopTimeValidation.INTERPOLATE,
        issueStore
      ).run();
    }
    resolvePendingTransfers();
    patternGenerator.generate(this, issueStore);
    removeEntitiesWithInvalidReferences();
    return new DefaultTransitDataImport(this);
  }

  /**
   * Add a mapping from a scheduled stop point to the regular stop.
   */
  public void addStopByScheduledStopPoint(FeedScopedId sspid, RegularStop stop) {
    stopsByScheduledStopPoints.put(sspid, stop);
  }

  /**
   * Find all serviceIds registered on {@link #tripCalendars()}.
   */
  Set<FeedScopedId> findAllServiceIds() {
    return tripCalendars.listServiceIds();
  }

  private static void logRemove(String type, int orgSize, int newSize, String reason) {
    if (orgSize == newSize) {
      return;
    }
    LOG.info("{} of {} {}(s) removed. Reason: {}", orgSize - newSize, orgSize, type, reason);
  }

  /**
   * Check all relations and remove entities which reference none existing entries. This may happen
   * as a result of inconsistent data or by deliberate removal of elements in the builder.
   */
  void removeEntitiesWithInvalidReferences() {
    removeTripsWithNoneExistingServiceIds();
    removeStopTimesForNoneExistingTrips();
    fixOrRemovePatternsWhichReferenceNoneExistingTrips();
    removeTransfersForNoneExistingTrips();
    removeTripOnServiceDateForNonExistingTrip();
    removeFlexTripsForNoneExistingTrips();
  }

  /** Remove all trips which reference none existing service ids */
  private void removeTripsWithNoneExistingServiceIds() {
    Set<FeedScopedId> serviceIds = findAllServiceIds();
    int orgSize = tripsById.size();
    tripsById.removeIf(
      t -> !serviceIds.contains(t.getServiceId()),
      t ->
        issueStore.add(
          "RemovedMissingServiceIdTrip",
          "Removed trip %s as service id %s does not exist",
          t.getId(),
          t.getServiceId()
        )
    );
    logRemove("Trip", orgSize, tripsById.size(), "Trip service id does not exist.");
  }

  /** Remove all stopTimes which reference none existing trips */
  private void removeStopTimesForNoneExistingTrips() {
    int orgSize = stopTimesByTrip.size();
    stopTimesByTrip.removeIf(t -> !tripsById.containsKey(t.getId()));
    logRemove("StopTime", orgSize, stopTimesByTrip.size(), "StopTime trip does not exist.");
  }

  /** Remove none existing trips from patterns and then remove empty patterns */
  private void fixOrRemovePatternsWhichReferenceNoneExistingTrips() {
    int orgSize = tripPatterns.size();
    List<Map.Entry<StopPattern, TripPattern>> removePatterns = new ArrayList<>();
    List<TripPattern> updatedPatterns = new ArrayList<>();

    for (Map.Entry<StopPattern, TripPattern> e : tripPatterns.entries()) {
      TripPattern ptn = e.getValue();
      Set<TripTimes<?>> tripTimesToBeRemoved = ptn
        .getScheduledTimetable()
        .getTripTimes()
        .stream()
        .filter(tripTimes -> !tripsById.containsKey(tripTimes.getTrip().getId()))
        .collect(Collectors.toUnmodifiableSet());
      if (!tripTimesToBeRemoved.isEmpty()) {
        removePatterns.add(e);
        Timetable updatedTimetable = ptn
          .getScheduledTimetable()
          .copyOf()
          .removeAllTripTimes(tripTimesToBeRemoved)
          .build();
        TripPattern updatedPattern = ptn.copy().withScheduledTimeTable(updatedTimetable).build();
        if (!updatedTimetable.getTripTimes().isEmpty()) {
          updatedPatterns.add(updatedPattern);
        } else {
          issueStore.add(
            "RemovedEmptyTripPattern",
            "Removed trip pattern %s as it contains no trips",
            updatedPattern.getId()
          );
        }
      }
    }
    for (Map.Entry<StopPattern, TripPattern> it : removePatterns) {
      tripPatterns.remove(it.getKey(), it.getValue());
    }
    for (TripPattern tripPattern : updatedPatterns) {
      tripPatterns.put(tripPattern.getStopPattern(), tripPattern);
    }
    logRemove("TripPattern", orgSize, tripPatterns.size(), "No trips for pattern exist.");
  }

  /** Remove all transfers which reference none existing trips */
  private void removeTransfersForNoneExistingTrips() {
    int orgSize = transfers.size();
    transfers.removeIf(this::transferTripReferencesDoNotExist);
    logRemove("Trip", orgSize, transfers.size(), "Transfer to/from trip does not exist.");
  }

  /**
   * Remove TripOnServiceDates if there are no trips using them
   */
  private void removeTripOnServiceDateForNonExistingTrip() {
    int orgSize = tripOnServiceDates.size();
    for (TripOnServiceDate tripOnServiceDate : tripOnServiceDates.values()) {
      if (!tripsById.containsKey(tripOnServiceDate.getTrip().getId())) {
        logRemove(
          "TripOnServiceDate",
          orgSize,
          tripOnServiceDates.size(),
          "Trip for TripOnServiceDate does not exist."
        );
      }
    }
  }

  /** Remove all flex trips which reference none existing trips */
  private void removeFlexTripsForNoneExistingTrips() {
    int orgSize = flexTripsById.size();
    flexTripsById.removeIf(
      t -> !tripsById.containsKey(t.getTrip().getId()),
      t ->
        issueStore.add(
          "RemovedMissingServiceIdFlexTrip",
          "Removed flex trip %s as trip %s does not exist",
          t.getId(),
          t.getTrip().getId()
        )
    );
    logRemove("FlexTrip", orgSize, flexTripsById.size(), "Trip for FlexTrip does not exist.");
  }

  /**
   * Resolve every {@link #pendingTransfers} entry's stop position(s) against the now-final
   * {@link #stopTimesByTrip} - see {@link DeferredConstrainedTransferPoint}. Must run after stop times have
   * been pruned/validated/interpolated, and before {@link DefaultTransitDataImport} takes its
   * snapshot of {@link #transfers}.
   */
  private void resolvePendingTransfers() {
    for (PendingConstrainedTransfer pending : pendingTransfers) {
      TransferPoint from = resolveTransferPoint(pending.from());
      TransferPoint to = resolveTransferPoint(pending.to());
      if (from == null || to == null) {
        issueStore.add(
          "InvalidTransfer",
          "Dropped transfer %s: could not resolve a stop position for one of its trip endpoints",
          pending.id()
        );
        continue;
      }
      transfers.add(new ConstrainedTransfer(pending.id(), from, to, pending.constraint()));
    }
    pendingTransfers.clear();
  }

  @Nullable
  private TransferPoint resolveTransferPoint(DeferredConstrainedTransferPoint point) {
    return switch (point) {
      case DeferredConstrainedTransferPoint.Resolved r -> r.point();
      case DeferredConstrainedTransferPoint.PendingTripPosition p -> {
        int pos = p.boardTrip()
          ? boardStopPosition(p.trip(), p.stop(), p.station())
          : alightStopPosition(p.trip(), p.stop(), p.station());
        yield pos < 0 ? null : new TripTransferPoint(p.trip(), pos);
      }
    };
  }

  private int boardStopPosition(Trip trip, @Nullable RegularStop stop, @Nullable Station station) {
    List<StopTime> stopTimes = stopTimesByTrip.get(trip);
    Predicate<StopLocation> stopMatches =
      station != null
        ? s -> s instanceof RegularStop regStop && regStop.getParentStation() == station
        : s -> s == stop;

    for (int i = 0; i < stopTimes.size() - 1; i++) {
      StopTime stopTime = stopTimes.get(i);
      if (stopTime.getPickupType().isNotRoutable()) {
        continue;
      }
      if (stopMatches.test(stopTime.getStop())) {
        return i;
      }
    }
    return -1;
  }

  private int alightStopPosition(Trip trip, @Nullable RegularStop stop, @Nullable Station station) {
    List<StopTime> stopTimes = stopTimesByTrip.get(trip);
    Predicate<StopLocation> stopMatches =
      station != null
        ? s -> s instanceof RegularStop regStop && regStop.getParentStation() == station
        : s -> s == stop;

    for (int i = stopTimes.size() - 1; i > 0; i--) {
      StopTime stopTime = stopTimes.get(i);
      if (stopTime.getDropOffType().isNotRoutable()) {
        continue;
      }
      if (stopMatches.test(stopTime.getStop())) {
        return i;
      }
    }
    return -1;
  }

  /** Return {@code true} if the from/to trip reference is none null, but do not exist. */
  private boolean transferTripReferencesDoNotExist(ConstrainedTransfer t) {
    return (
      transferPointTripReferenceDoesNotExist(t.getFrom()) ||
      transferPointTripReferenceDoesNotExist(t.getTo())
    );
  }

  /**
   * Return {@code true} if the point is a trip-transfer-point and the trip reference is missing.
   */
  private boolean transferPointTripReferenceDoesNotExist(TransferPoint point) {
    if (!point.isTripTransferPoint()) {
      return false;
    }
    var trip = point.asTripTransferPoint().getTrip();
    return !tripsById.containsKey(trip.getId());
  }
}
