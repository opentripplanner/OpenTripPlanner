package org.opentripplanner.transit.service;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NoFutureDates;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.transfer.DefaultTransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.util.ConcurrentPublished;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.opentripplanner.utils.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TimetableRepository groups together all instances making up OTP's primary internal representation
 * of the public transportation network. Although the names of many entities are derived from
 * GTFS concepts, these are actually independent of the data source from which they are loaded.
 * Both GTFS and NeTEx entities are mapped to these same internal OTP entities. If a concept exists
 * in both GTFS and NeTEx, the GTFS name is used in the internal model. For concepts that exist
 * only in NeTEx, the NeTEx name is used in the internal model.
 *
 * A TimetableRepository instance also includes references to some transient indexes of its contents, to
 * the RaptorTransitData derived from it, and to some other services and utilities that operate upon
 * its contents.
 *
 * The TimetableRepository stands in opposition to two other aggregates: the Graph (representing the
 * street network) and the RaptorTransitData (representing many of the same things in the TimetableRepository
 * but rearranged to be more efficient for Raptor routing).
 *
 * At this point the TimetableRepository is not often read directly. Many requests will look at the
 * RaptorTransitData rather than the TimetableRepository it's derived from. Both are often accessed via the
 * TransitService rather than directly reading the fields of TimetableRepository or RaptorTransitData.
 */
public class TimetableRepository implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TimetableRepository.class);

  private final Collection<Agency> agencies = new ArrayList<>();
  private final Collection<Operator> operators = new ArrayList<>();
  private final Collection<String> feedIds = new HashSet<>();
  private final Map<String, FeedInfo> feedInfoForId = new HashMap<>();

  private final Multimap<AbstractTransitEntity, Notice> noticesByElement = HashMultimap.create();
  private final DefaultTransferService transferService = new DefaultTransferService();

  private final HashSet<TransitMode> transitModes = new HashSet<>();

  private final Map<FeedScopedId, Integer> serviceCodes = new HashMap<>();

  private final Multimap<StopLocation, PathTransfer> transfersByStop = HashMultimap.create();

  private SiteRepository siteRepository;
  private ZonedDateTime transitServiceStarts = LocalDate.MAX.atStartOfDay(ZoneId.systemDefault());
  private ZonedDateTime transitServiceEnds = LocalDate.MIN.atStartOfDay(ZoneId.systemDefault());

  /**
   * The RaptorTransitData representation (optimized and rearranged for Raptor) of this TimetableRepository's
   * scheduled (non-realtime) contents.
   */
  private transient RaptorTransitData raptorTransitData;

  /**
   * An optionally present second RaptorTransitData representing the contents of this TimetableRepository plus
   * the results of realtime updates in the latest TimetableSnapshot.
   */
  private final transient ConcurrentPublished<RaptorTransitData> realtimeRaptorTransitData =
    new ConcurrentPublished<>();

  private final transient Deduplicator deduplicator;

  private final CalendarServiceData calendarServiceData = new CalendarServiceData();

  private transient TimetableRepositoryIndex index;
  private ZoneId timeZone = null;
  private boolean timeZoneExplicitlySet = false;

  private transient GraphUpdaterManager updaterManager = null;

  private boolean hasTransit = false;

  private boolean hasFrequencyService = false;
  private boolean hasScheduledService = false;

  private final Map<FeedScopedId, TripPattern> tripPatternForId = new HashMap<>();
  private final Map<FeedScopedId, TripOnServiceDate> tripOnServiceDates = new HashMap<>();

  private final Map<FeedScopedId, FlexTrip<?, ?>> flexTripsById = new HashMap<>();

  private transient TransitAlertService transitAlertService;

  private final Map<FeedScopedId, RegularStop> stopsByScheduledStopPointRefs = new HashMap<>();

  @Inject
  public TimetableRepository(SiteRepository siteRepository, Deduplicator deduplicator) {
    this.siteRepository = Objects.requireNonNull(siteRepository);
    this.deduplicator = deduplicator;
  }

  /** No-argument constructor, required for deserialization. */
  public TimetableRepository() {
    this(new SiteRepository(), new Deduplicator());
  }

  /**
   * Perform indexing on timetables, and create transient data structures. This used to be done
   * inline in readObject methods upon deserialization, but it is now possible to pass transit data
   * from the graph builder to the server in memory, without a round trip through serialization.
   */
  public void index() {
    if (index == null) {
      LOG.info("Index timetable repository...");
      this.index = new TimetableRepositoryIndex(this);
      LOG.info("Index timetable repository complete.");
    }
  }

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  public RaptorTransitData getRaptorTransitData() {
    return raptorTransitData;
  }

  public void setRaptorTransitData(RaptorTransitData raptorTransitData) {
    this.raptorTransitData = raptorTransitData;
  }

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  @Nullable
  public RaptorTransitData getRealtimeRaptorTransitData() {
    return realtimeRaptorTransitData.get();
  }

  /**
   * Publish the latest snapshot of the real-time transit layer.
   * Should be called only when creating a new RaptorTransitData, from the graph writer thread.
   */
  public void setRealtimeRaptorTransitData(RaptorTransitData realtimeRaptorTransitData) {
    this.realtimeRaptorTransitData.publish(realtimeRaptorTransitData);
  }

  /**
   * Return true if a real-time transit layer is present.
   * The real-time transit layer is optional,
   * it is present only when real-time updaters are configured.
   */
  public boolean hasRealtimeRaptorTransitData() {
    return realtimeRaptorTransitData != null;
  }

  public DefaultTransferService getTransferService() {
    return transferService;
  }

  /**
   * Returns true if this repository contains any transit data at the given instant.
   */
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

  /** List of transit modes that are available in GTFS data used in this graph **/
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
   * realtime data update. It makes sure the date is in the existing transit service period.
   * <p>
   *
   * @param serviceDate service date for the added service id
   * @return service-id for date if it exists or is created. If the given service date is outside the
   * service period {@code null} is returned.
   */
  @Nullable
  public FeedScopedId getOrCreateServiceIdForDate(LocalDate serviceDate) {
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
    return Collections.unmodifiableCollection(operators);
  }

  public void addOperators(Collection<Operator> operators) {
    this.operators.addAll(operators);
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
          ("The graph contains agencies with different time zones: %s. " +
            "Please configure the one to be used in the %s").formatted(zones, BUILD_CONFIG_FILENAME)
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

  /**
   * Returns the alert service. If no updaters are configured an empty instance is returned.
   * See  {@link TimetableRepository#setUpdaterManager(GraphUpdaterManager)}.
   */
  public TransitAlertService getTransitAlertService() {
    if (transitAlertService == null) {
      transitAlertService = new DelegatingTransitAlertServiceImpl(this);
    }
    return transitAlertService;
  }

  public TripPattern getTripPatternForId(FeedScopedId id) {
    return tripPatternForId.get(id);
  }

  public void addTripOnServiceDate(TripOnServiceDate tripOnServiceDate) {
    invalidateIndex();
    tripOnServiceDates.put(tripOnServiceDate.getId(), tripOnServiceDate);
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

  /** Pre-generated transfers between all stops filtered based on the modes in the PathTransfer. */
  public List<PathTransfer> findTransfers(StreetMode mode) {
    return transfersByStop
      .values()
      .stream()
      .filter(pathTransfer -> pathTransfer.getModes().contains(mode))
      .toList();
  }

  public SiteRepository getSiteRepository() {
    return siteRepository;
  }

  public void addTripPattern(FeedScopedId id, TripPattern tripPattern) {
    invalidateIndex();
    tripPatternForId.put(id, tripPattern);
  }

  public void addScheduledStopPointMapping(Map<FeedScopedId, RegularStop> mapping) {
    stopsByScheduledStopPointRefs.putAll(mapping);
  }

  /**
   * Return the stop that is associated with the NeTEx concept of a scheduled stop point.
   * <p>
   * The scheduled stop point is a "location-independent" stop that schedule systems provide
   * which in turn can be later be resolved to an actual stop.
   * <p>
   * This way two schedule systems can use their own IDs for scheduled stop points but the stop (the
   * actual physical infrastructure) is the same.
   * <p>
   * SIRI feeds are encouraged to refer to scheduled stop points in an EstimatedCall's stopPointRef
   * but the specs are unclear and the reality on the ground very mixed.
   *
   * @link <a href="https://public.3.basecamp.com/p/TcEEP5WrNZJPBxrJU9GAjint">NeTEx Basecamp discussion</a>
   */
  public Optional<RegularStop> findStopByScheduledStopPoint(FeedScopedId scheduledStopPoint) {
    return Optional.ofNullable(stopsByScheduledStopPointRefs.get(scheduledStopPoint));
  }

  /**
   * TripPatterns used to be reached through hop edges, but we're not creating on-board transit
   * vertices/edges anymore.
   */
  public Collection<TripPattern> getAllTripPatterns() {
    return tripPatternForId.values();
  }

  public TripOnServiceDate getTripOnServiceDateById(FeedScopedId tripOnServiceDateId) {
    return tripOnServiceDates.get(tripOnServiceDateId);
  }

  public Collection<TripOnServiceDate> getAllTripsOnServiceDates() {
    return Collections.unmodifiableCollection(tripOnServiceDates.values());
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

  /**
   * Updating the site repository is only allowed during graph build
   */
  public void mergeSiteRepositories(SiteRepository childSiteRepository) {
    invalidateIndex();
    this.siteRepository = this.siteRepository.merge(childSiteRepository);
  }

  public void addFlexTrip(FeedScopedId id, FlexTrip<?, ?> flexTrip) {
    invalidateIndex();
    flexTripsById.put(id, flexTrip);
  }

  /**
   * Sets the updater manager for this repository and makes sure the configured updaters
   * are correctly applied to {@code transitAlertService}.
   * <p>
   * Note: before this method is called an empty {@code transitAlertService} is returned instead.
   * <p>
   * This logic is unfortunate and quite brittle. We would like to improve it in the future.
   */
  public void setUpdaterManager(GraphUpdaterManager updaterManager) {
    this.updaterManager = updaterManager;
    this.transitAlertService = null;
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
  @Nullable
  TimetableRepositoryIndex getTimetableRepositoryIndex() {
    return index;
  }

  /**
   * For all dates in the system get the service codes that run on it.
   */
  public Map<LocalDate, TIntSet> getServiceCodesRunningForDate() {
    return Collections.unmodifiableMap(index.getServiceCodesRunningForDate());
  }

  public boolean isIndexed() {
    return index != null;
  }

  public boolean hasFlexTrips() {
    return !flexTripsById.isEmpty();
  }

  public FlexTrip getFlexTrip(FeedScopedId tripId) {
    return flexTripsById.get(tripId);
  }

  /**
   * The stops that are used by transit capable of transporting cars need to be
   * connected to the road network (e.g. car ferries). This method returns the
   * stops that are used by trips that allow cars.
   * @return set of stop locations that are used for trips that allow cars
   */
  public Set<StopLocation> getStopLocationsUsedForCarsAllowedTrips() {
    Set<StopLocation> stopLocations = getAllTripPatterns()
      .stream()
      .filter(t ->
        t
          .getScheduledTimetable()
          .getTripTimes()
          .stream()
          .anyMatch(tt -> tt.getTrip().getCarsAllowed() == CarAccess.ALLOWED)
      )
      .flatMap(t -> t.getStops().stream())
      .collect(Collectors.toSet());

    stopLocations.addAll(
      stopLocations
        .stream()
        .filter(GroupStop.class::isInstance)
        .map(GroupStop.class::cast)
        .flatMap(g -> g.getChildLocations().stream().filter(RegularStop.class::isInstance))
        .toList()
    );
    return stopLocations;
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
