package org.opentripplanner.graph_builder.module.nearbystops;

import static java.util.Objects.requireNonNull;

import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.MaxCountTerminationStrategy;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.NearbyStopFactory;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;

public class StreetNearbyStopFinder implements NearbyStopFinder {

  private final Duration durationLimit;
  private final int maxStopCount;
  private final Set<Vertex> ignoreVertices;

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   *
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the maxStopCount
   *                     we will always return all the directly connected stops.
   * @param ignoreVertices   A set of stop vertices to ignore and not return NearbyStops for.
   */
  private StreetNearbyStopFinder(
    Duration durationLimit,
    int maxStopCount,
    Set<Vertex> ignoreVertices
  ) {
    this.durationLimit = requireNonNull(durationLimit);
    this.maxStopCount = requireNonNull(maxStopCount);
    this.ignoreVertices = requireNonNull(ignoreVertices);
  }

  /**
   * Build a NearbyStopFinder for the given graph and search radius, defined by the
   * {@code durationLimit}.
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the
   *                     maxStopCount we will always return all the directly connected stops.
   */
  public static Builder of(Duration durationLimit, int maxStopCount) {
    return new Builder(durationLimit, maxStopCount);
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it; this characteristic
   * is essential for associating the correct stop with each trip pattern in the vicinity.
   */
  @Override
  public Collection<NearbyStop> findNearbyStops(Vertex vertex, StreetSearchRequest request) {
    return findNearbyStops(Set.of(vertex), request);
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it.
   *
   * @param originVertices   the origin point of the street search.
   */
  public Collection<NearbyStop> findNearbyStops(
    Set<Vertex> originVertices,
    StreetSearchRequest request
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    var requestWithoutEnvelopes = request
      .copyOf()
      .withFromEnvelope(null)
      .withToEnvelope(null)
      .build();
    List<NearbyStop> stopsFound = NearbyStopFactory.nearbyStopsForTransitStopVerticesFiltered(
      Sets.difference(originVertices, ignoreVertices),
      requestWithoutEnvelopes
    );

    // Return only the origin vertices if there are no valid street modes
    if (
      request.mode() == StreetMode.NOT_SET ||
      (maxStopCount > 0 && stopsFound.size() >= maxStopCount)
    ) {
      return stopsFound;
    }
    stopsFound = new ArrayList<>(stopsFound);

    var visitor = new NearbyStopFinderVisitor(originVertices, ignoreVertices, request.arriveBy());

    var streetSearch = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(durationLimit))
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withTraverseVisitor(visitor)
      .withRequest(request)
      .withArriveBy(request.arriveBy())
      .withFrom(request.arriveBy() ? null : originVertices)
      .withTo(request.arriveBy() ? originVertices : null);

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

    private final Duration durationLimit;
    private final int maxStopCount;
    private Set<Vertex> ignoreVertices = Set.of();

    public Builder(Duration durationLimit, int maxStopCount) {
      this.durationLimit = durationLimit;
      this.maxStopCount = maxStopCount;
    }

    /**
     * Specify a set of stop vertices to ignore and not return NearbyStops for.
     */
    public Builder withIgnoreVertices(Set<Vertex> ignoreVertices) {
      this.ignoreVertices = ignoreVertices;
      return this;
    }

    public StreetNearbyStopFinder build() {
      return new StreetNearbyStopFinder(durationLimit, maxStopCount, ignoreVertices);
    }
  }
}
