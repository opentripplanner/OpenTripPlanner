package org.opentripplanner.place.nearbystopfinder;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.MaxCountTerminationStrategy;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.LinkingContextRequest;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;

public class StreetNearbyStopFinder implements NearbyStopFinder {

  private final LinkingContextFactory linkingContextFactory;
  private final Duration durationLimit;
  private final int maxStopCount;
  private final Collection<ExtensionRequestContext> extensionRequestContexts;
  private final Set<Vertex> ignoreVertices;

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   *
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the maxStopCount
   *                     we will always return all the directly connected stops.
   * @param ignoreVertices   A set of stop vertices to ignore and not return NearbyStops for.
   */
  private StreetNearbyStopFinder(
    @Nullable LinkingContextFactory linkingContextFactory,
    Duration durationLimit,
    int maxStopCount,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    Set<Vertex> ignoreVertices
  ) {
    // This is temporarily nullable as we don't need it when we don't link coordinates, but soon
    // setting this everywhere will be easier once construction is moved to dagger
    this.linkingContextFactory = linkingContextFactory;
    // TODO move request specific parameters to method
    this.durationLimit = requireNonNull(durationLimit);
    this.maxStopCount = requireNonNull(maxStopCount);
    this.extensionRequestContexts = requireNonNull(extensionRequestContexts);
    this.ignoreVertices = requireNonNull(ignoreVertices);
  }

  public static Builder of(LinkingContextFactory linkingContextFactory) {
    return new Builder(linkingContextFactory);
  }

  /**
   * Build a NearbyStopFinder for the given graph and search radius, defined by the
   * {@code durationLimit}.
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the
   *                     maxStopCount we will always return all the directly connected stops.
   */
  public static Builder of(
    LinkingContextFactory linkingContextFactory,
    Duration durationLimit,
    int maxStopCount
  ) {
    return new Builder(linkingContextFactory, durationLimit, maxStopCount);
  }

  @Override
  public List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters) {
    StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor(radiusMeters);
    findClosestUsingStreets(
      coordinate.getY(),
      coordinate.getX(),
      visitor,
      visitor.getSkipEdgeStrategy()
    );
    return visitor.stopsFound();
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it; this characteristic
   * is essential for associating the correct stop with each trip pattern in the vicinity.
   */
  @Override
  public Collection<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetMode streetMode,
    boolean reverseDirection
  ) {
    return findNearbyStops(Set.of(vertex), routingRequest, streetMode, reverseDirection);
  }

  /**
   * TODO we probably should get rid of this method and use the one below this. This is copied from
   * {@link org.opentripplanner.place.placefinder.StreetNearbyPlaceFinder}.
   */
  private void findClosestUsingStreets(
    double lat,
    double lon,
    TraverseVisitor<State, Edge> visitor,
    SkipEdgeStrategy<State, Edge> skipEdgeStrategy
  ) {
    // RR dateTime defaults to currentTime.
    // If elapsed time is not capped, searches are very slow.
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var from = GenericLocation.fromCoordinate(lat, lon);
      var linkingRequest = LinkingContextRequest.of()
        .withFrom(from)
        .withDirectMode(StreetMode.WALK)
        .build();
      var linkerContext = linkingContextFactory.create(temporaryVerticesContainer, linkingRequest);
      // Make a normal OTP routing request so we can traverse edges and use GenericAStar
      // TODO make a function that builds normal routing requests from profile requests
      // TODO: This is incorrect, the configured defaults are not used.
      var request = StreetSearchRequest.of()
        .withWalk(it -> it.withSpeed(1))
        .build();
      StreetSearchBuilder.of()
        .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
        .withSkipEdgeStrategy(skipEdgeStrategy)
        .withTraverseVisitor(visitor)
        .withDominanceFunction(new DominanceFunctions.ShortestDistance())
        .withRequest(request)
        .withFrom(linkerContext.findVertices(from))
        .run();
    }
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it.
   *
   * @param originVertices   the origin point of the street search.
   * @param reverseDirection if true the paths returned instead originate at the nearby stops and
   *                         have the originVertex as the destination.
   */
  public Collection<NearbyStop> findNearbyStops(
    Set<Vertex> originVertices,
    RouteRequest request,
    StreetMode streetMode,
    boolean reverseDirection
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    List<NearbyStop> stopsFound = NearbyStopFactory.nearbyStopsForTransitStopVerticesFiltered(
      Sets.difference(originVertices, ignoreVertices),
      reverseDirection,
      request,
      streetMode
    );

    // Return only the origin vertices if there are no valid street modes
    if (
      streetMode == StreetMode.NOT_SET || (maxStopCount > 0 && stopsFound.size() >= maxStopCount)
    ) {
      return stopsFound;
    }
    stopsFound = new ArrayList<>(stopsFound);

    var visitor = new NearbyStopFinderVisitor(originVertices, ignoreVertices, reverseDirection);

    var streetSearch = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(durationLimit))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withTraverseVisitor(visitor)
      .withRequest(
        StreetSearchRequestMapper.map(request)
          .withMode(streetMode)
          .withExtensionRequestContexts(extensionRequestContexts)
          .build()
      )
      .withArriveBy(reverseDirection)
      .withFrom(reverseDirection ? null : originVertices)
      .withTo(reverseDirection ? originVertices : null);

    if (maxStopCount > 0) {
      streetSearch.withTerminationStrategy(
        new MaxCountTerminationStrategy<>(maxStopCount, this::hasReachedStop)
      );
    }

    streetSearch.getShortestPathTree();

    stopsFound.addAll(visitor.transitStopsFound());

    if (OTPFeature.FlexRouting.isOn()) {
      for (var statesForAreaStopIds : visitor.statesForAreaStopIds()) {
        var areaStopId = statesForAreaStopIds.getKey();
        var min = statesForAreaStopIds.getValue();

        // If the best state for this AreaStop is a SplitterVertex, we want to get the
        // TemporaryStreetLocation instead. This allows us to reach SplitterVertices in both
        // directions when routing later.
        if (min.getBackState().getVertex() instanceof TemporaryStreetLocation) {
          min = min.getBackState();
        }

        stopsFound.add(NearbyStop.nearbyStopForState(min, areaStopId));
      }
    }

    return stopsFound;
  }

  /**
   * Checks if the {@code state} is at a transit vertex and if it's final, which means that the state
   * can actually board a vehicle.
   * <p>
   * This is important because there can be cases where states that cannot actually board the vehicle
   * can dominate those that can thereby leading to zero found stops when this predicate is used with
   * the {@link MaxCountTerminationStrategy}.
   * <p>
   * An example of this would be an egress/reverse search with a very high walk reluctance where the
   * states that speculatively rent a vehicle move the walk states down the A* priority queue until
   * the required number of stops are reached to abort the search, leading to zero egress results.
   */
  private boolean hasReachedStop(State state) {
    var vertex = state.getVertex();
    return (
      vertex instanceof TransitStopVertex && state.isFinal() && !ignoreVertices.contains(vertex)
    );
  }

  public static class Builder {

    private final LinkingContextFactory linkingContextFactory;
    // TODO these should be moved to be method parameters instead of constructor
    private Duration durationLimit = Duration.ofHours(10000);
    private int maxStopCount = 1000;
    private Collection<ExtensionRequestContext> extensionRequestContexts = List.of();
    private Set<Vertex> ignoreVertices = Set.of();

    public Builder(LinkingContextFactory linkingContextFactory) {
      this.linkingContextFactory = linkingContextFactory;
    }

    public Builder(
      LinkingContextFactory linkingContextFactory,
      Duration durationLimit,
      int maxStopCount
    ) {
      this.linkingContextFactory = linkingContextFactory;
      this.durationLimit = durationLimit;
      this.maxStopCount = maxStopCount;
    }

    /**
     * The search can adjusted using extensions. Each extention may provide its own request context.
     * Set the context here.
     * <p>
     * @see org.opentripplanner.street.model.edge.StreetEdgeCostExtension
     */
    public Builder withExtensionRequestContexts(
      Collection<ExtensionRequestContext> extensionRequestContexts
    ) {
      this.extensionRequestContexts = extensionRequestContexts;
      return this;
    }

    /**
     * Specify a set of stop vertices to ignore and not return NearbyStops for.
     */
    public Builder withIgnoreVertices(Set<Vertex> ignoreVertices) {
      this.ignoreVertices = ignoreVertices;
      return this;
    }

    public StreetNearbyStopFinder build() {
      return new StreetNearbyStopFinder(
        linkingContextFactory,
        durationLimit,
        maxStopCount,
        extensionRequestContexts,
        ignoreVertices
      );
    }
  }
}
