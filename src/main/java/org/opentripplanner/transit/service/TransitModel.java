package org.opentripplanner.transit.service;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.hash.TIntHashSet;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.lang.ObjectUtils;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NoFutureDates;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.transfer.DefaultTransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.util.ConcurrentPublished;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Transit entities.
 */
public class TransitModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TransitModel.class);

  private final Collection<Agency> agencies = new ArrayList<>();
  private final Collection<Operator> operators = new ArrayList<>();
  private final Collection<String> feedIds = new HashSet<>();
  private final Map<String, FeedInfo> feedInfoForId = new HashMap<>();

  private final Multimap<AbstractTransitEntity, Notice> noticesByElement = HashMultimap.create();
  private final DefaultTransferService transferService = new DefaultTransferService();

  private final HashSet<TransitMode> transitModes = new HashSet<>();

  private final Map<FeedScopedId, Integer> serviceCodes = new HashMap<>();

  private final Multimap<StopLocation, PathTransfer> transfersByStop = HashMultimap.create();

  private StopModel stopModel;
  private ZonedDateTime transitServiceStarts = LocalDate.MAX.atStartOfDay(ZoneId.systemDefault());
  private ZonedDateTime transitServiceEnds = LocalDate.MIN.atStartOfDay(ZoneId.systemDefault());

  private final transient ConcurrentPublished<TransitLayer> realtimeTransitLayer = new ConcurrentPublished<>();

  private final transient Deduplicator deduplicator;

  private final CalendarServiceData calendarServiceData = new CalendarServiceData();

  private transient TransitModelIndex index;
  private transient TimetableSnapshotProvider timetableSnapshotProvider = null;
  private ZoneId timeZone = null;
  private boolean timeZoneExplicitlySet = false;

  private transient GraphUpdaterManager updaterManager = null;

  private boolean hasTransit = false;

  private boolean hasFrequencyService = false;
  private boolean hasScheduledService = false;

  private final Map<FeedScopedId, TripPattern> tripPatternForId = new HashMap<>();
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDates = new HashMap<>();

  private final Map<FeedScopedId, FlexTrip<?, ?>> flexTripsById = new HashMap<>();

  private transient TransitLayer transitLayer;
  private transient TransitLayerUpdater transitLayerUpdater;

  private transient TransitAlertService transitAlertService;

  @Inject
  public TransitModel(StopModel stopModel, Deduplicator deduplicator) {
    this.stopModel = Objects.requireNonNull(stopModel);
    this.deduplicator = deduplicator;
  }

  /** Constructor for deserialization. */
  public TransitModel() {
    this(new StopModel(), new Deduplicator());
  }

  /**
   * Perform indexing on timetables, and create transient data structures. This used to be done in
   * readObject methods upon deserialization, but stand-alone mode now allows passing graphs from
   * graphbuilder to server in memory, without a round trip through serialization.
   */
  public void index() {
    if (index == null) {
      LOG.info("Index transit model...");
      // the transit model indexing updates the stop model index (flex stops added to the stop index)
      this.index = new TransitModelIndex(this);
      LOG.info("Index transit model complete.");
    }
  }

  public TimetableSnapshot getTimetableSnapshot() {
    return timetableSnapshotProvider == null
      ? null
      : timetableSnapshotProvider.getTimetableSnapshot();
  }

  public void initTimetableSnapshotProvider(TimetableSnapshotProvider timetableSnapshotProvider) {
    if (this.timetableSnapshotProvider != null) {
      throw new IllegalArgumentException(
        "We support only one timetableSnapshotSource, there are two implementation; one for " +
        "GTFS and one for Netex/Siri. They need to be refactored to work together. This cast " +
        "will fail if updaters try setup both."
      );
    }
    this.timetableSnapshotProvider = timetableSnapshotProvider;
  }

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  public TransitLayer getTransitLayer() {
    return transitLayer;
  }

  public void setTransitLayer(TransitLayer transitLayer) {
    this.transitLayer = transitLayer;
  }

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  public TransitLayer getRealtimeTransitLayer() {
    return realtimeTransitLayer.get();
  }

  public void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer) {
    this.realtimeTransitLayer.publish(realtimeTransitLayer);
  }

  public boolean hasRealtimeTransitLayer() {
    return realtimeTransitLayer != null;
  }

  public DefaultTransferService getTransferService() {
    return transferService;
  }

  // Check to see if we have transit information for a given date
  public boolean transitFeedCovers(Instant time) {
    return (
      !time.isBefore(this.transitServiceStarts.toInstant()) &&
      time.isBefore(this.transitServiceEnds.toInstant())
    );
  }

  /**
   * Adds mode of transport to transit modes in graph
   */
  public void addTransitMode(TransitMode mode) {
    invalidateIndex();
    transitModes.add(mode);
  }

  /** List of transit modes that are availible in GTFS data used in this graph **/
  public HashSet<TransitMode> getTransitModes() {
    return transitModes;
  }

  public CalendarService getCalendarService() {
    // No need to cache the CalendarService, it is a thin wrapper around the data
    return new CalendarServiceImpl(calendarServiceData);
  }

  public void updateCalendarServiceData(
    boolean hasActiveTransit,
    CalendarServiceData data,
    DataImportIssueStore issueStore
  ) {
    invalidateIndex();
    updateTransitFeedValidity(data, issueStore);
    calendarServiceData.add(data);

    updateHasTransit(hasActiveTransit);
  }

  /**
   * Get or create a serviceId for a given date. This method is used when a new trip is added from a
   * realtime data update. It make sure the date is in the existing transit service period.
   * <p>
   * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
   *           - this when doing the issue #3030.
   *
   * @param serviceDate service date for the added service id
   * @return service-id for date if it exist or is created. If the given service date is outside the
   * service period {@code null} is returned.
   */
  @Nullable
  public FeedScopedId getOrCreateServiceIdForDate(@Nonnull LocalDate serviceDate) {
    // Start of day
    ZonedDateTime time = ServiceDateUtils.asStartOfService(serviceDate, getTimeZone());

    if (!transitFeedCovers(time.toInstant())) {
      return null;
    }

    // We make an explicit cast here to avoid adding the 'getOrCreateServiceIdForDate(..)'
    // method to the {@link CalendarService} interface. We do not want to expose it because it
    // is not thread-safe - and we want to limit the usage. See JavaDoc above as well.
    FeedScopedId serviceId =
      ((CalendarServiceImpl) getCalendarService()).getOrCreateServiceIdForDate(serviceDate);

    if (!serviceCodes.containsKey(serviceId)) {
      // Calculating new unique serviceCode based on size (!)
      final int serviceCode = serviceCodes.size();
      serviceCodes.put(serviceId, serviceCode);

      index
        .getServiceCodesRunningForDate()
        .computeIfAbsent(serviceDate, ignored -> new TIntHashSet())
        .add(serviceCode);
    }
    return serviceId;
  }

  public Collection<String> getFeedIds() {
    return feedIds;
  }

  public Collection<Agency> getAgencies() {
    return agencies;
  }

  public FeedInfo getFeedInfo(String feedId) {
    return feedInfoForId.get(feedId);
  }

  public void addAgency(Agency agency) {
    invalidateIndex();
    agencies.add(agency);
    this.feedIds.add(agency.getId().getFeedId());
  }

  public void addFeedInfo(FeedInfo info) {
    invalidateIndex();
    this.feedInfoForId.put(info.getId(), info);
  }

  /**
   * Returns the time zone for the transit model. This is used to interpret times in API requests.
   * Ideally we would want to interpret times in the time zone of the geographic location where the
   * origin/destination vertex or board/alight event is located. This may become necessary when we
   * start making graphs with long distance train, boat, or air services.
   */
  public ZoneId getTimeZone() {
    return timeZone;
  }

  /**
   * Initialize the time zone, if it has not been set previously.
   */
  public void initTimeZone(ZoneId timeZone) {
    if (timeZone == null || timeZone.equals(this.timeZone)) {
      return;
    }
    invalidateIndex();
    this.timeZone = ObjectUtils.requireNotInitialized(this.timeZone, timeZone);
    this.timeZoneExplicitlySet = true;
  }

  /**
   * Returns the time zone for the transit model. This is either configured in the build config, or
   * from the agencies in the data, if they are on the same time zone. This is used to interpret
   * times in API requests. Ideally we would want to interpret times in the time zone of the
   * geographic location where the origin/destination vertex or board/alight event is located. This
   * may become necessary when we start making graphs with long distance train, boat, or air
   * services.
   */
  public Set<ZoneId> getAgencyTimeZones() {
    Set<ZoneId> ret = new HashSet<>();
    for (Agency agency : agencies) {
      ret.add(agency.getTimezone());
    }
    return ret;
  }

  public Collection<Operator> getOperators() {
    return operators;
  }

  /**
   * OTP doesn't currently support multiple time zones in a single graph, unless explicitly
   * configured. Check that the time zone of the added agencies are the same as the current.
   * At least this way we catch the error and log it instead of silently ignoring because the
   * time zone from the first agency is used
   */
  public void validateTimeZones() {
    if (!timeZoneExplicitlySet) {
      Collection<ZoneId> zones = getAgencyTimeZones();
      if (zones.size() > 1) {
        throw new IllegalStateException(
          (
            "The graph contains agencies with different time zones: %s. " +
            "Please configure the one to be used in the %s"
          ).formatted(zones, BUILD_CONFIG_FILENAME)
        );
      }
    }
  }

  /** transit feed validity information in seconds since epoch */
  public ZonedDateTime getTransitServiceStarts() {
    return transitServiceStarts;
  }

  public ZonedDateTime getTransitServiceEnds() {
    return transitServiceEnds;
  }

  /**
   * Allows a notice element to be attached to an object in the OTP model by its id and then
   * retrieved by the API when navigating from that object. The map key is entity id:
   * {@link AbstractTransitEntity#getId()}. The notice is part of the static transit data.
   */
  public Multimap<AbstractTransitEntity, Notice> getNoticesByElement() {
    return noticesByElement;
  }

  public void addNoticeAssignments(Multimap<AbstractTransitEntity, Notice> noticesByElement) {
    invalidateIndex();
    this.noticesByElement.putAll(noticesByElement);
  }

  public TransitAlertService getTransitAlertService() {
    if (transitAlertService == null) {
      transitAlertService = new DelegatingTransitAlertServiceImpl(this);
    }
    return transitAlertService;
  }

  public TripPattern getTripPatternForId(FeedScopedId id) {
    return tripPatternForId.get(id);
  }

  public Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDates() {
    return tripOnServiceDates;
  }

  /**
   * Map from GTFS ServiceIds to integers close to 0. Allows using BitSets instead of
   * {@code Set<Object>}. An empty Map is created before the Graph is built to allow registering IDs
   * from multiple feeds.
   */
  public Map<FeedScopedId, Integer> getServiceCodes() {
    return serviceCodes;
  }

  /** Pre-generated transfers between all stops. */
  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return transfersByStop.get(stop);
  }

  public StopModel getStopModel() {
    return stopModel;
  }

  public void addTripPattern(FeedScopedId id, TripPattern tripPattern) {
    invalidateIndex();
    tripPatternForId.put(id, tripPattern);
  }

  /**
   * TripPatterns used to be reached through hop edges, but we're not creating on-board transit
   * vertices/edges anymore.
   */
  public Collection<TripPattern> getAllTripPatterns() {
    return tripPatternForId.values();
  }

  public Collection<TripOnServiceDate> getAllTripOnServiceDates() {
    return tripOnServiceDates.values();
  }

  /**
   * Manages all updaters of this graph. Is created by the GraphUpdaterConfigurator when there are
   * graph updaters defined in the configuration.
   *
   * @see UpdaterConfigurator
   */
  public GraphUpdaterManager getUpdaterManager() {
    return updaterManager;
  }

  public TransitLayerUpdater getTransitLayerUpdater() {
    return transitLayerUpdater;
  }

  public Deduplicator getDeduplicator() {
    return deduplicator;
  }

  public Collection<PathTransfer> getAllPathTransfers() {
    return transfersByStop.values();
  }

  public Collection<FlexTrip<?, ?>> getAllFlexTrips() {
    return flexTripsById.values();
  }

  /** True if there are active transit services loaded into this Graph. */
  public boolean hasTransit() {
    return hasTransit;
  }

  public Optional<Agency> findAgencyById(FeedScopedId id) {
    return agencies.stream().filter(a -> a.getId().equals(id)).findAny();
  }

  private void updateHasTransit(boolean hasTransit) {
    this.hasTransit = this.hasTransit || hasTransit;
  }

  public void setTransitLayerUpdater(TransitLayerUpdater transitLayerUpdater) {
    this.transitLayerUpdater = transitLayerUpdater;
  }

  /**
   * Updating the stop model is only allowed during graph build
   */
  public void mergeStopModels(StopModel childStopModel) {
    invalidateIndex();
    this.stopModel = this.stopModel.merge(childStopModel);
  }

  public void addFlexTrip(FeedScopedId id, FlexTrip<?, ?> flexTrip) {
    invalidateIndex();
    flexTripsById.put(id, flexTrip);
  }

  public void setUpdaterManager(GraphUpdaterManager updaterManager) {
    this.updaterManager = updaterManager;
  }

  public void addAllTransfersByStops(Multimap<StopLocation, PathTransfer> transfersByStop) {
    invalidateIndex();
    this.transfersByStop.putAll(transfersByStop);
  }

  /**
   * True if frequency-based services exist in this Graph (GTFS frequencies with exact_times = 0).
   */
  public boolean hasFrequencyService() {
    return hasFrequencyService;
  }

  public void setHasFrequencyService(boolean hasFrequencyService) {
    this.hasFrequencyService = hasFrequencyService;
  }

  /**
   * True if schedule-based services exist in this Graph (including GTFS frequencies with
   * exact_times = 1).
   */
  public boolean hasScheduledService() {
    return hasScheduledService;
  }

  public void setHasScheduledService(boolean hasScheduledService) {
    this.hasScheduledService = hasScheduledService;
  }

  /**
   * The caller is responsible for calling the {@link #index()} method if it is a
   * possibility that the index is not initialized (during graph build).
   */
  public @Nullable TransitModelIndex getTransitModelIndex() {
    return index;
  }

  public boolean hasFlexTrips() {
    return !flexTripsById.isEmpty();
  }

  public FlexTrip getFlexTrip(FeedScopedId tripId) {
    return flexTripsById.get(tripId);
  }

  private void invalidateIndex() {
    this.index = null;
  }

  /**
   * Infer the time period covered by the transit feed
   */
  private void updateTransitFeedValidity(
    CalendarServiceData data,
    @Nullable DataImportIssueStore issueStore
  ) {
    Instant now = Instant.now();
    HashSet<String> agenciesWithFutureDates = new HashSet<>();
    HashSet<String> agencies = new HashSet<>();
    initTimeZone();

    for (FeedScopedId sid : data.getServiceIds()) {
      agencies.add(sid.getFeedId());
      for (LocalDate sd : data.getServiceDatesForServiceId(sid)) {
        // Adjust for timezone, assuming there is only one per graph.

        ZonedDateTime t = ServiceDateUtils.asStartOfService(sd, getTimeZone());
        if (t.toInstant().isAfter(now)) {
          agenciesWithFutureDates.add(sid.getFeedId());
        }
        // assume feed is unreliable after midnight on last service day
        ZonedDateTime u = t.plusDays(1);
        if (t.isBefore(this.transitServiceStarts)) {
          this.transitServiceStarts = t;
        }
        if (u.isAfter(this.transitServiceEnds)) {
          this.transitServiceEnds = u;
        }
      }
    }
    if (issueStore != null) {
      for (String agency : agencies) {
        if (!agenciesWithFutureDates.contains(agency)) {
          issueStore.add(new NoFutureDates(agency));
        }
      }
    }
  }

  private void initTimeZone() {
    if (timeZone == null) {
      if (agencies.isEmpty()) {
        timeZone = ZoneId.of("GMT");
        LOG.warn("graph contains no agencies (yet); API request times will be interpreted as GMT.");
      } else {
        timeZone = getAgencyTimeZones().iterator().next();
        LOG.debug("graph time zone set to {}", timeZone);
      }
    }
  }
}
