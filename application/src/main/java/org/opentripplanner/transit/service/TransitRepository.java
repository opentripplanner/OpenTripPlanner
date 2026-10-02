package org.opentripplanner.transit.service;

import static org.opentripplanner.framework.application.OtpFileNames.BUILD_CONFIG_FILENAME;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import jakarta.inject.Inject;
import java.io.Serializable;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.transfer.constrained.ConstrainedTransferService;
import org.opentripplanner.transfer.constrained.internal.DefaultConstrainedTransferService;
import org.opentripplanner.transit.model.basic.Notice;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.updater.configure.UpdaterConfigurator;
import org.opentripplanner.utils.lang.ObjectUtils;
import org.opentripplanner.utils.logging.PowerOfTwoThrottle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TransitRepository groups together all instances making up OTP's primary internal representation
 * of the public transportation network. Although the names of many entities are derived from
 * GTFS concepts, these are actually independent of the data source from which they are loaded.
 * Both GTFS and NeTEx entities are mapped to these same internal OTP entities. If a concept exists
 * in both GTFS and NeTEx, the GTFS name is used in the internal model. For concepts that exist
 * only in NeTEx, the NeTEx name is used in the internal model.
 * <p>
 * The scheduled trip patterns, trips and trip calendars are not part of this repository. At
 * runtime the timetable repository owns them as
 * {@link org.opentripplanner.transit.repository.ScheduledTimetableData}.
 * <p>
 * A TransitRepository instance also includes references to some transient indexes of its contents, to
 * the RaptorTransitData derived from it, and to some other services and utilities that operate upon
 * its contents.
 * <p>
 * The TransitRepository stands in opposition to two other aggregates: the Graph (representing the
 * street network) and the RaptorTransitData (representing many of the same things in the TransitRepository
 * but rearranged to be more efficient for Raptor routing).
 * <p>
 * At this point the TransitRepository is not often read directly. Many requests will look at the
 * RaptorTransitData rather than the TransitRepository it's derived from. Both are often accessed via the
 * TransitService rather than directly reading the fields of TransitRepository or RaptorTransitData.
 */
public class TransitRepository implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(TransitRepository.class);
  private static final PowerOfTwoThrottle FREEZE_LOG_THROTTLE = new PowerOfTwoThrottle();

  private final Collection<Agency> agencies = new ArrayList<>();
  private final Collection<Operator> operators = new ArrayList<>();
  private final Collection<String> feedIds = new HashSet<>();
  private final Map<String, FeedInfo> feedInfoForId = new HashMap<>();

  private final Multimap<AbstractTransitEntity, Notice> noticesByElement = HashMultimap.create();
  private final ConstrainedTransferService constrainedTransferService =
    new DefaultConstrainedTransferService();

  private SiteRepository siteRepository;

  /**
   * The RaptorTransitData representation (optimized and rearranged for Raptor) of the scheduled
   * (non-realtime) timetable data, mapped at server startup.
   * <p>
   * TODO: This does not belong here. The scheduled timetable data itself is no longer part of this
   *       repository (see ScheduledTimetableData, owned by the timetable repository), and the
   *       timetable repository already owns the real-time Raptor data seeded from a copy of this.
   *       The candidate owner is the timetable repository, exposing the scheduled Raptor data via
   *       its snapshots. It is not ScheduledTimetableData: the Raptor data is also derived from
   *       transfers, stops and tuning parameters, contains mutable caches, and is mapped from the
   *       scheduled data. Left here until the Raptor-data creation at startup is refactored.
   */
  private transient RaptorTransitData raptorTransitData;

  private transient TransitRepositoryIndex index;
  private ZoneId timeZone = null;
  private boolean timeZoneExplicitlySet = false;

  private transient GraphUpdaterManager updaterManager = null;

  private boolean hasFrequencyService = false;
  private boolean hasScheduledService = false;

  private final Map<FeedScopedId, RegularStop> stopsByScheduledStopPointRefs = new HashMap<>();

  /// Updates are not allowed after the repository is frozen. All realtime updates should be
  /// applied to the TimetableRepository. The repository is modifiable during graph build then
  /// frozen when the server is started.
  private boolean frozen = false;

  @Inject
  public TransitRepository(SiteRepository siteRepository) {
    this.siteRepository = Objects.requireNonNull(siteRepository);
  }

  /** No-argument constructor, required for deserialization. */
  public TransitRepository() {
    this(new SiteRepository());
  }

  /**
   * Perform indexing on timetables, and create transient data structures. This used to be done
   * inline in readObject methods upon deserialization, but it is now possible to pass transit data
   * from the graph builder to the server in memory, without a round trip through serialization.
   */
  public void index() {
    assertModificationsAllowed();
    if (index == null) {
      LOG.info("Index timetable repository...");
      this.index = new TransitRepositoryIndex(this);
      LOG.info("Index timetable repository complete.");
    }
  }

  /**
   * Make the Timetable repository immutable when the otp server is started. After this point,
   * all modifications should be done to the TimetableRepository.
   */
  public void freeze() {
    index();
    this.frozen = true;
  }

  /** Data model for Raptor routing of the scheduled data, without real-time updates. */
  public RaptorTransitData getRaptorTransitData() {
    return raptorTransitData;
  }

  public void initRaptorTransitData(RaptorTransitData raptorTransitData) {
    // TODO Enforce this is initialized once with ObjectUtils.requireNotInitialized()
    //      Currently there is tests which violates this.
    assertModificationsAllowed();
    this.raptorTransitData = raptorTransitData;
  }

  public ConstrainedTransferService getConstrainedTransferService() {
    return constrainedTransferService;
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
    assertModificationsAllowed();
    invalidateIndex();
    agencies.add(agency);
    feedIds.add(agency.getId().getFeedId());

    if (!timeZoneExplicitlySet) {
      // We use the agency timezone unless the timezone is specifically set.
      if (timeZone == null) {
        timeZone = agency.getTimezone();
      } else if (!timeZone.equals(agency.getTimezone())) {
        /*
         * OTP doesn't currently support multiple time zones in a single graph, unless explicitly
         * configured. Check that the time zone of the added agencies are the same as the current.
         * At least this way we catch the error and log it instead of silently ignoring because the
         * time zone from the first agency is used
         */
        throw new IllegalStateException(
          String.format(
            "The graph contains agencies with different time zones: %s != %s. Please configure the one to be used in the %s",
            timeZone,
            agency.getTimezone(),
            BUILD_CONFIG_FILENAME
          )
        );
      }
    }
  }

  public void addFeedInfo(FeedInfo info) {
    assertModificationsAllowed();
    invalidateIndex();
    this.feedInfoForId.put(info.getId(), info);
  }

  /**
   * Returns the time zone for the transit model. This is used to interpret times in API requests.
   * Ideally we would want to interpret times in the time zone of the geographic location where the
   * origin/destination vertex or board/alight event is located. This may become necessary when we
   * start making graphs with long distance train, boat, or air services.
   * <p>
   * Defaults to GMT if not set and there are no agencies specified.
   */
  public ZoneId getTimeZone() {
    if (timeZone != null) {
      return timeZone;
    }
    LOG.warn("graph contains no agencies (yet); API request times will be interpreted as GMT.");
    return ZoneId.of("GMT");
  }

  /**
   * Initialize the time zone, if it has not been set previously.
   */
  public void initTimeZone(ZoneId timeZone) {
    assertModificationsAllowed();
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
    assertModificationsAllowed();
    this.operators.addAll(operators);
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
    assertModificationsAllowed();
    invalidateIndex();
    this.noticesByElement.putAll(noticesByElement);
  }

  public SiteRepository getSiteRepository() {
    return siteRepository;
  }

  public void addScheduledStopPointMapping(Map<FeedScopedId, RegularStop> mapping) {
    assertModificationsAllowed();
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
   * Sets the updater manager for this repository and makes sure the configured updaters
   * are correctly applied to {@code transitAlertService}.
   * <p>
   * Note: before this method is called an empty {@code transitAlertService} is returned instead.
   * <p>
   * TODO: This logic is unfortunate and quite brittle. We would like to improve it in the future.
   *       The UpdateManager should live in a DI context(Dagger), not here.
   */
  public void initUpdaterManager(GraphUpdaterManager updaterManager) {
    this.updaterManager = ObjectUtils.requireNotInitialized(
      "updaterManager",
      this.updaterManager,
      updaterManager
    );
  }

  /**
   * Manages all updaters of this graph. Is created by the GraphUpdaterConfigurator when there are
   * graph updaters defined in the configuration. This is {@code null} if no updaters are
   * configured or not yet initialized.
   *
   * @see UpdaterConfigurator
   */
  @Nullable
  public GraphUpdaterManager getUpdaterManager() {
    return updaterManager;
  }

  public Optional<Agency> findAgencyById(FeedScopedId id) {
    return agencies
      .stream()
      .filter(a -> a.getId().equals(id))
      .findAny();
  }

  /**
   * Updating the site repository is only allowed during graph build
   */
  public void mergeSiteRepositories(SiteRepository childSiteRepository) {
    assertModificationsAllowed();
    invalidateIndex();
    this.siteRepository = this.siteRepository.merge(childSiteRepository);
  }

  /**
   * True if frequency-based services exist in this Graph (GTFS frequencies with exact_times = 0).
   */
  public boolean hasFrequencyService() {
    return hasFrequencyService;
  }

  public void setHasFrequencyService(boolean hasFrequencyService) {
    assertModificationsAllowed();
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
    assertModificationsAllowed();
    this.hasScheduledService = hasScheduledService;
  }

  /**
   * The caller is responsible for calling the {@link #index()} method if it is a
   * possibility that the index is not initialized (during graph build).
   */
  @Nullable
  TransitRepositoryIndex getTransitRepositoryIndex() {
    return index;
  }

  public boolean isIndexed() {
    return index != null;
  }

  private void invalidateIndex() {
    this.index = null;
  }

  private void assertModificationsAllowed() {
    if (frozen) {
      FREEZE_LOG_THROTTLE.throttle(n ->
        LOG.warn(
          """
          THIS SHOULD NOT HAPPEN
          Attempting to modify TransitRepository after it has been frozen.
          Count: {}
          """,
          n,
          new RuntimeException("StackTrace included to trace the source of the error.")
        )
      );
    }
  }
}
