package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WalkPreferences;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.utils.logging.Throttle;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Transfer {

  private static final Logger LOG = LoggerFactory.getLogger(Transfer.class);
  private static final Throttle THROTTLE_COST_EXCEEDED = Throttle.ofOneSecond();

  protected static final int MAX_TRANSFER_COST = 2_000_000;

  private final int toStop;

  private final int distanceMeters;

  /** EnumSet is modifiable, please do not modify or expose this outside the class */
  private final EnumSet<StreetMode> modes;

  private final List<Edge> edges;

  private Transfer(int toStop, int distanceMeters, EnumSet<StreetMode> modes, List<Edge> edges) {
    this.toStop = toStop;
    this.distanceMeters = distanceMeters;
    this.modes = EnumSet.copyOf(modes);
    this.edges = edges;
  }

  public Transfer(int toStop, List<Edge> edges, EnumSet<StreetMode> modes) {
    this(toStop, (int) edges.stream().mapToDouble(Edge::getDistanceMeters).sum(), modes, edges);
  }

  public Transfer(int toStopIndex, int distanceMeters, EnumSet<StreetMode> modes) {
    this(toStopIndex, distanceMeters, modes, null);
  }

  public List<Coordinate> getCoordinates() {
    List<Coordinate> coordinates = new ArrayList<>();
    if (edges == null) {
      return coordinates;
    }
    for (Edge edge : edges) {
      if (edge.getGeometry() != null) {
        coordinates.addAll((Arrays.asList(edge.getGeometry().getCoordinates())));
      }
    }
    return coordinates;
  }

  public int getToStop() {
    return toStop;
  }

  public int getDistanceMeters() {
    return distanceMeters;
  }

  public List<Edge> getEdges() {
    return edges;
  }

  /** Check if the given mode is a valid mode for the transfer. */
  public boolean allowsMode(StreetMode mode) {
    return modes.contains(mode);
  }

  public Optional<DefaultRaptorTransfer> asRaptorTransfer(StreetSearchRequest request) {
    WalkPreferences walkPreferences = request.preferences().walk();
    if (edges == null || edges.isEmpty()) {
      double durationSeconds = distanceMeters / walkPreferences.speed();
      final double domainCost = costLimitSanityCheck(
        durationSeconds * walkPreferences.reluctance()
      );
      return Optional.of(
        new DefaultRaptorTransfer(
          this.toStop,
          (int) Math.ceil(durationSeconds),
          RaptorCostConverter.toRaptorCost(domainCost),
          this
        )
      );
    }

    StateEditor se = new StateEditor(edges.get(0).getFromVertex(), request);
    se.setTimeSeconds(0);

    var state = EdgeTraverser.traverseEdges(se.makeState(), edges);

    return state.map(s ->
      new DefaultRaptorTransfer(
        this.toStop,
        (int) s.getElapsedTimeSeconds(),
        RaptorCostConverter.toRaptorCost(costLimitSanityCheck(s.getWeight())),
        this
      )
    );
  }

  /**
   * This return "WALK" or "[BIKE, WALK]". Intended for debugging and logging, do not parse.
   */
  public String modesAsString() {
    return modes.size() == 1 ? modes.stream().findFirst().get().toString() : modes.toString();
  }

  /**
   * Since transfer costs are not computed through a full A* with pruning they can incur an
   * absurdly high cost that overflows the integer cost inside RAPTOR
   * (https://github.com/opentripplanner/OpenTripPlanner/issues/5509).
   * <p>
   * An example would be a transfer using lots of stairs being used on a wheelchair when no
   * wheelchair-specific one has been generated.
   * (see https://docs.opentripplanner.org/en/dev-2.x/Accessibility/).
   * <p>
   * For this reason there is this sanity limit that makes sure that the transfer cost stays below a
   * limit that is still very high (several days of transit-equivalent cost) but far away from the
   * integer overflow.
   *
   * @see EdgeTraverser
   * @see RaptorCostConverter
   */
  private int costLimitSanityCheck(double cost) {
    if (cost >= 0 && cost <= MAX_TRANSFER_COST) {
      return (int) cost;
    } else {
      THROTTLE_COST_EXCEEDED.throttle(() ->
        LOG.warn(
          "Transfer exceeded maximum cost. Please consider changing the transfer cost calculation. More information: https://github.com/opentripplanner/OpenTripPlanner/pull/5516#issuecomment-1819138078"
        )
      );
      return MAX_TRANSFER_COST;
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(Transfer.class)
      .addNum("toStop", toStop)
      .addNum("distance", distanceMeters, "m")
      .toString();
  }
}
