package org.opentripplanner.transit.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import gnu.trove.set.hash.TIntHashSet;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NoFutureDates;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TripOnServiceDate;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.util.ConcurrentPublished;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitEntity;
import org.opentripplanner.transit.model.network.TransitMode;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.Stop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.util.time.ServiceDateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository for Transit entities.
 */
public class TransitModel implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TransitModel.class);

  private static final long serialVersionUID = 1L;

  private final Collection<Agency> agencies = new ArrayList<>();
  private final Collection<Operator> operators = new ArrayList<>();
  private final Collection<String> feedIds = new HashSet<>();
  private final Map<String, FeedInfo> feedInfoForId = new HashMap<>();

  /**
   * Allows a notice element to be attached to an object in the OTP model by its id and then
   * retrieved by the API when navigating from that object. The map key is entity id:
   * {@link TransitEntity#getId()}. The notice is part of the static transit data.
   */
  private final Multimap<TransitEntity, Notice> noticesByElement = HashMultimap.create();
  private final Map<Class<?>, Serializable> services = new HashMap<>();
  private final TransferService transferService = new TransferService();

  /** List of transit modes that are availible in GTFS data used in this graph **/
  private final HashSet<TransitMode> transitModes = new HashSet<>();

  /**
   * Map from GTFS ServiceIds to integers close to 0. Allows using BitSets instead of
   * {@code Set<Object>}. An empty Map is created before the Graph is built to allow registering IDs
   * from multiple feeds.
   */
  private final Map<FeedScopedId, Integer> serviceCodes = Maps.newHashMap();

  /** Pre-generated transfers between all stops. */
  public final Multimap<StopLocation, PathTransfer> transfersByStop = HashMultimap.create();

  private StopModel stopModel;
  // transit feed validity information in seconds since epoch
  private ZonedDateTime transitServiceStarts = LocalDate.MAX.atStartOfDay(ZoneId.systemDefault());
  private ZonedDateTime transitServiceEnds = LocalDate.MIN.atStartOfDay(ZoneId.systemDefault());

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  private final transient ConcurrentPublished<TransitLayer> realtimeTransitLayer = new ConcurrentPublished<>();

  public final transient Deduplicator deduplicator;
  private transient CalendarService calendarService;

  public transient TransitModelIndex index;
  private transient TimetableSnapshotProvider timetableSnapshotProvider = null;
  private transient ZoneId timeZone = null;

  /**
   * Manages all updaters of this graph. Is created by the GraphUpdaterConfigurator when there are
   * graph updaters defined in the configuration.
   *
   * @see GraphUpdaterConfigurator
   */
  public transient GraphUpdaterManager updaterManager = null;

  /** True if GTFS data was loaded into this Graph. */
  public boolean hasTransit = false;

  /** True if direct single-edge transfers were generated between transit stops in this Graph. */
  public boolean hasDirectTransfers = false;
  /**
   * True if frequency-based services exist in this Graph (GTFS frequencies with exact_times = 0).
   */
  public boolean hasFrequencyService = false;
  /**
   * True if schedule-based services exist in this Graph (including GTFS frequencies with
   * exact_times = 1).
   */
  public boolean hasScheduledService = false;

  /**
   * TripPatterns used to be reached through hop edges, but we're not creating on-board transit
   * vertices/edges anymore.
   */
  public Map<FeedScopedId, TripPattern> tripPatternForId = Maps.newHashMap();
  public Map<FeedScopedId, TripOnServiceDate> tripOnServiceDates = Maps.newHashMap();

  public Map<FeedScopedId, FlexTrip> flexTripsById = new HashMap<>();

  /** Data model for Raptor routing, with realtime updates applied (if any). */
  private transient TransitLayer transitLayer;
  public transient TransitLayerUpdater transitLayerUpdater;

  private transient TransitAlertService transitAlertService;

  public TransitModel(StopModel stopModel, Deduplicator deduplicator) {
    this.stopModel = stopModel;
    this.deduplicator = deduplicator;
  }

  // Constructor for deserialization.
  public TransitModel() {
    deduplicator = new Deduplicator();
  }

  /**
   * Perform indexing on timetables, and create transient data structures. This used to be done in
   * readObject methods upon deserialization, but stand-alone mode now allows passing graphs from
   * graphbuilder to server in memory, without a round trip through serialization.
   */
  public void index() {
    LOG.info("Index transit model...");
    this.getStopModel().index();
    // the transit model indexing updates the stop model index (flex stops added to the stop index)
    this.index = new TransitModelIndex(this);
    LOG.info("Index transit model complete.");
  }

  public TimetableSnapshot getTimetableSnapshot() {
    return timetableSnapshotProvider == null
      ? null
      : timetableSnapshotProvider.getTimetableSnapshot();
  }

  /**
   * TODO OTP2 - This should be replaced by proper dependency injection
   */
  @SuppressWarnings("unchecked")
  public <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(
    Function<TransitModel, T> creator
  ) {
    if (timetableSnapshotProvider == null) {
      timetableSnapshotProvider = creator.apply(this);
    }
    try {
      return (T) timetableSnapshotProvider;
    } catch (ClassCastException e) {
      throw new IllegalArgumentException(
        "We support only one timetableSnapshotSource, there are two implementation; one for GTFS and one " +
        "for Netex/Siri. They need to be refactored to work together. This cast will fail if updaters " +
        "try setup both.",
        e
      );
    }
  }

  public TransitLayer getTransitLayer() {
    return transitLayer;
  }

  public void setTransitLayer(TransitLayer transitLayer) {
    this.transitLayer = transitLayer;
  }

  public TransitLayer getRealtimeTransitLayer() {
    return realtimeTransitLayer.get();
  }

  public void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer) {
    this.realtimeTransitLayer.publish(realtimeTransitLayer);
  }

  public boolean hasRealtimeTransitLayer() {
    return realtimeTransitLayer != null;
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable> T putService(Class<T> serviceType, T service) {
    return (T) services.put(serviceType, service);
  }

  public boolean hasService(Class<? extends Serializable> serviceType) {
    return services.containsKey(serviceType);
  }

  @SuppressWarnings("unchecked")
  public <T extends Serializable> T getService(Class<T> serviceType) {
    return (T) services.get(serviceType);
  }

  public <T extends Serializable> T getService(Class<T> serviceType, boolean autoCreate) {
    T t = (T) services.get(serviceType);
    if (t == null && autoCreate) {
      try {
        t = serviceType.getDeclaredConstructor().newInstance();
      } catch (
        IllegalAccessException
        | InvocationTargetException
        | NoSuchMethodException
        | InstantiationException e
      ) {
        throw new RuntimeException(e);
      }
      services.put(serviceType, t);
    }
    return t;
  }

  public TransferService getTransferService() {
    return transferService;
  }

  // Infer the time period covered by the transit feed
  public void updateTransitFeedValidity(CalendarServiceData data, DataImportIssueStore issueStore) {
    Instant now = Instant.now();
    HashSet<String> agenciesWithFutureDates = new HashSet<>();
    HashSet<String> agencies = new HashSet<>();
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
    for (String agency : agencies) {
      if (!agenciesWithFutureDates.contains(agency)) {
        issueStore.add(new NoFutureDates(agency));
      }
    }
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
    transitModes.add(mode);
  }

  public HashSet<TransitMode> getTransitModes() {
    return transitModes;
  }

  public CalendarService getCalendarService() {
    if (calendarService == null) {
      CalendarServiceData data = this.getService(CalendarServiceData.class);
      if (data != null) {
        this.calendarService = new CalendarServiceImpl(data);
      }
    }
    return this.calendarService;
  }

  public CalendarServiceData getCalendarDataService() {
    CalendarServiceData calendarServiceData;
    if (this.hasService(CalendarServiceData.class)) {
      calendarServiceData = this.getService(CalendarServiceData.class);
    } else {
      calendarServiceData = new CalendarServiceData();
    }
    return calendarServiceData;
  }

  public void clearCachedCalenderService() {
    this.calendarService = null;
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

  public void addAgency(String feedId, Agency agency) {
    agencies.add(agency);
    this.feedIds.add(feedId);
  }

  public void addFeedInfo(FeedInfo info) {
    this.feedInfoForId.put(info.getId(), info);
  }

  /**
   * Returns the time zone for the first agency in this graph. This is used to interpret times in
   * API requests. The JVM default time zone cannot be used because we support multiple graphs on
   * one server via the routerId. Ideally we would want to interpret times in the time zone of the
   * geographic location where the origin/destination vertex or board/alight event is located. This
   * may become necessary when we start making graphs with long distance train, boat, or air
   * services.
   */
  public ZoneId getTimeZone() {
    if (timeZone == null) {
      if (agencies.size() == 0) {
        timeZone = ZoneId.of("GMT");
        LOG.warn("graph contains no agencies (yet); API request times will be interpreted as GMT.");
      } else {
        CalendarService cs = this.getCalendarService();
        for (Agency agency : agencies) {
          ZoneId tz = cs.getTimeZoneForAgencyId(agency.getId());
          if (timeZone == null) {
            LOG.debug("graph time zone set to {}", tz);
            timeZone = tz;
          } else if (!timeZone.equals(tz)) {
            LOG.error("agency time zone differs from graph time zone: {}", tz);
          }
        }
      }
    }
    return timeZone;
  }

  public Collection<Operator> getOperators() {
    return operators;
  }

  /**
   * The timezone is cached by the graph. If you've done something to the graph that has the
   * potential to change the time zone, you should call this to ensure it is reset.
   */
  public void clearTimeZone() {
    this.timeZone = null;
  }

  public ZonedDateTime getTransitServiceStarts() {
    return transitServiceStarts;
  }

  public ZonedDateTime getTransitServiceEnds() {
    return transitServiceEnds;
  }

  public Multimap<TransitEntity, Notice> getNoticesByElement() {
    return noticesByElement;
  }

  public void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement) {
    this.noticesByElement.putAll(noticesByElement);
  }

  public TransitAlertService getTransitAlertService() {
    if (transitAlertService == null) {
      transitAlertService = new DelegatingTransitAlertServiceImpl(this);
    }
    return transitAlertService;
  }

  public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
    Collection<Notice> res = getNoticesByElement().get(entity);
    return res == null ? Collections.emptyList() : res;
  }

  public TripPattern getTripPatternForId(FeedScopedId id) {
    return tripPatternForId.get(id);
  }

  public Collection<TripPattern> getTripPatterns() {
    return tripPatternForId.values();
  }

  public Map<FeedScopedId, TripOnServiceDate> getTripOnServiceDates() {
    return tripOnServiceDates;
  }

  public Collection<Notice> getNotices() {
    return getNoticesByElement().values();
  }

  /**
   * Finds a {@link StopLocation} by id.
   */
  public StopLocation getStopLocationById(FeedScopedId id) {
    var stop = stopModel.getStopModelIndex().getStopForId(id);
    if (stop != null) {
      return stop;
    }

    return getAllFlexStopsFlat()
      .stream()
      .filter(stopLocation -> stopLocation.getId().equals(id))
      .findAny()
      .orElse(null);
  }

  /**
   * Returns all {@link StopLocation}s present in this graph, including normal and flex locations.
   */
  public Stream<StopLocation> getAllStopLocations() {
    return Stream.concat(
      getStopModel().getStopModelIndex().getAllStops().stream(),
      getAllFlexStopsFlat().stream()
    );
  }

  public Map<FeedScopedId, Integer> getServiceCodes() {
    return serviceCodes;
  }

  public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
    return transfersByStop.get(stop);
  }

  /**
   * Gets all the flex stop locations, including the elements of FlexLocationGroups.
   */
  public Set<StopLocation> getAllFlexStopsFlat() {
    Set<StopLocation> stopLocations = flexTripsById
      .values()
      .stream()
      .flatMap(t -> t.getStops().stream())
      .collect(Collectors.toSet());

    stopLocations.addAll(
      stopLocations
        .stream()
        .filter(s -> s instanceof FlexLocationGroup)
        .flatMap(g -> ((FlexLocationGroup) g).getLocations().stream().filter(e -> e instanceof Stop)
        )
        .collect(Collectors.toList())
    );

    return stopLocations;
  }

  public void calculateTransitCenter() {
    stopModel.calculateTransitCenter();
  }

  public StopModel getStopModel() {
    return stopModel;
  }

  public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
    return stopModel.getStopModelIndex().getStopSpatialIndex();
  }

  private void readObject(ObjectInputStream inputStream)
    throws ClassNotFoundException, IOException {
    inputStream.defaultReadObject();
  }
}
