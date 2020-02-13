package org.opentripplanner.routing.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class GraphIndex {

  private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);

  // TODO: consistently key on model object or id string
  private final Map<String, Map<String, Agency>> agenciesForFeedId = Maps.newHashMap();
  private final Map<FeedScopedId, Operator> operatorForId = Maps.newHashMap();
  private final Map<String, FeedInfo> feedInfoForId = Maps.newHashMap();
  private final Map<FeedScopedId, Stop> stopForId = Maps.newHashMap();
  private final Map<FeedScopedId, Trip> tripForId = Maps.newHashMap();
  private final Map<FeedScopedId, Route> routeForId = Maps.newHashMap();
  private final Map<Stop, TransitStopVertex> stopVertexForStop = Maps.newHashMap();
  private final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
  private final Multimap<String, TripPattern> patternsForFeedId = ArrayListMultimap.create();
  private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
  private final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
  private final Map<Station, MultiModalStation> multiModalStationForStations = Maps.newHashMap();
  private final HashGridSpatialIndex<TransitStopVertex> stopSpatialIndex = new HashGridSpatialIndex<>();
  private final Map<ServiceDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();
  /* Should eventually be replaced with new serviceId indexes. */
  private final CalendarService calendarService;

  public GraphIndex(Graph graph) {
    LOG.info("Indexing graph...");

    CompactElevationProfile.setDistanceBetweenSamplesM(graph.getDistanceBetweenElevationSamples());

    for (String feedId : graph.getFeedIds()) {
      for (Agency agency : graph.getAgencies(feedId)) {
        Map<String, Agency> agencyForId = agenciesForFeedId.getOrDefault(feedId, new HashMap<>());
        agencyForId.put(agency.getId(), agency);
        this.agenciesForFeedId.put(feedId, agencyForId);
      }
      this.feedInfoForId.put(feedId, graph.getFeedInfo(feedId));
    }

    for (Operator operator : graph.getOperators()) {
      this.operatorForId.put(operator.getId(), operator);
    }

    /* We will keep a separate set of all vertices in case some have the same label.
     * Maybe we should just guarantee unique labels. */
    for (Vertex vertex : graph.getVertices()) {
      if (vertex instanceof TransitStopVertex) {
        TransitStopVertex stopVertex = (TransitStopVertex) vertex;
        Stop stop = stopVertex.getStop();
        stopForId.put(stop.getId(), stop);
        stopVertexForStop.put(stop, stopVertex);
      }
    }
    for (TransitStopVertex stopVertex : stopVertexForStop.values()) {
      Envelope envelope = new Envelope(stopVertex.getCoordinate());
      stopSpatialIndex.insert(envelope, stopVertex);
    }
    for (TripPattern pattern : graph.tripPatternForId.values()) {
      patternsForFeedId.put(pattern.getFeedId(), pattern);
      patternsForRoute.put(pattern.route, pattern);
      for (Trip trip : pattern.getTrips()) {
        patternForTrip.put(trip, pattern);
        tripForId.put(trip.getId(), trip);
      }
      for (Stop stop: pattern.getStops()) {
        patternsForStop.put(stop, pattern);
      }
    }
    for (Route route : patternsForRoute.asMap().keySet()) {
      routeForId.put(route.getId(), route);
    }
    for (MultiModalStation multiModalStation : graph.multiModalStationById.values()) {
      for (Station childStation : multiModalStation.getChildStations()) {
        multiModalStationForStations.put(childStation, multiModalStation);
      }
    }

    // Copy these two service indexes from the graph until we have better ones.
    calendarService = graph.getCalendarService();

    initalizeServiceCodesForDate(graph);

    LOG.info("Done indexing graph.");
  }

  public Map<String, Map<String, Agency>> getAgenciesForFeedId() {
    return agenciesForFeedId;
  }

  public Map<FeedScopedId, Operator> getOperatorForId() {
    return operatorForId;
  }

  public Map<String, FeedInfo> getFeedInfoForId() {
    return feedInfoForId;
  }

  public Map<FeedScopedId, Stop> getStopForId() {
    return stopForId;
  }

  public Map<FeedScopedId, Trip> getTripForId() {
    return tripForId;
  }

  public Map<FeedScopedId, Route> getRouteForId() {
    return routeForId;
  }

  public Map<Stop, TransitStopVertex> getStopVertexForStop() {
    return stopVertexForStop;
  }

  public Map<Trip, TripPattern> getPatternForTrip() {
    return patternForTrip;
  }

  public Multimap<String, TripPattern> getPatternsForFeedId() {
    return patternsForFeedId;
  }

  public Multimap<Route, TripPattern> getPatternsForRoute() {
    return patternsForRoute;
  }

  public Multimap<Stop, TripPattern> getPatternsForStop() {
    return patternsForStop;
  }

  public Map<Station, MultiModalStation> getMultiModalStationForStations() {
    return multiModalStationForStations;
  }

  public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
    return stopSpatialIndex;
  }

  public Map<ServiceDate, TIntSet> getServiceCodesRunningForDate() {
    return serviceCodesRunningForDate;
  }

  private void initalizeServiceCodesForDate(Graph graph) {

    if (calendarService == null) { return; }

    // CalendarService has one main implementation (CalendarServiceImpl) which contains a
    // CalendarServiceData which can easily supply all of the dates. But it's impossible to
    // actually see those dates without modifying the interfaces and inheritance. So we have
    // to work around this abstraction and reconstruct the CalendarData.
    // Note the "multiCalendarServiceImpl" which has docs saying it expects one single
    // CalendarData. It seems to merge the calendar services from multiple GTFS feeds, but
    // its only documentation says it's a hack.
    // TODO OTP2 - This cleanup is added to the 'Final cleanup OTP2' issue #2757

    // Reconstruct set of all dates where service is defined, keeping track of which services
    // run on which days.
    Multimap<ServiceDate, FeedScopedId> serviceIdsForServiceDate = HashMultimap.create();

    for (FeedScopedId serviceId : calendarService.getServiceIds()) {
      Set<ServiceDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(serviceId);
      for (ServiceDate serviceDate : serviceDatesForService) {
        serviceIdsForServiceDate.put(serviceDate, serviceId);
      }
    }
    for (ServiceDate serviceDate : serviceIdsForServiceDate.keySet()) {
      TIntSet serviceCodesRunning = new TIntHashSet();
      for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
        serviceCodesRunning.add(graph.serviceCodes.get(serviceId));
      }
      serviceCodesRunningForDate.put(
          serviceDate,
          serviceCodesRunning
      );
    }
  }

  /**
   * Fetch an agency by its string ID, ignoring the fact that this ID should be scoped by a feedId.
   * This is a stopgap (i.e. hack) method for fetching agencies where no feed scope is available.
   * I am creating this method only to allow merging pull request #2032 which adds GraphQL.
   * Note that if the same agency ID is defined in several feeds, this will return one of them
   * at random. That is obviously not the right behavior. The problem is that agencies are
   * not currently keyed on an FeedScopedId object, but on separate feedId and id Strings.
   * A real fix will involve replacing or heavily modifying the OBA GTFS loader, which is now
   * possible since we have forked it.
   */
  public Agency getAgencyWithoutFeedId(String agencyId) {
    // Iterate over the agency map for each feed.
    for (Map<String, Agency> agencyForId : agenciesForFeedId.values()) {
      Agency agency = agencyForId.get(agencyId);
      if (agency != null) {
        return agency;
      }
    }
    return null;
  }
}
