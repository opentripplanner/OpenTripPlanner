package org.opentripplanner.transfer.regular.model;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.WalkRequest;
import org.opentripplanner.street.search.state.EdgeTraverser;
import org.opentripplanner.street.search.state.StateEditor;
import org.opentripplanner.transfer.constrained.model.ConstrainedTransfer;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.logging.Throttle;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a transfer for a set of modes between stops with the street network path attached to it.
 * <p>
 * Do not confuse this with {@link ConstrainedTransfer}.
 *
 * <p>
 * TODO Should we just store the NearbyStop as a field here, or even switch to using it instead
 *      where this class is used
 */
public class PathTransfer implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(PathTransfer.class);

  public final StopLocation from;

  public final StopLocation to;

  private final double distanceMeters;

  private final List<Edge> edges;

  private final EnumSet<StreetMode> modes;

  private static final Throttle THROTTLE_COST_EXCEEDED = Throttle.ofOneSecond();

  protected static final int MAX_TRANSFER_COST = 2_000_000;

  public PathTransfer(
    StopLocation from,
    StopLocation to,
    double distanceMeters,
    List<Edge> edges,
    EnumSet<StreetMode> modes
  ) {
    this.from = from;
    this.to = to;
    this.distanceMeters = distanceMeters;
    this.edges = edges;
    this.modes = modes;
  }

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public List<Edge> getEdges() {
    return edges;
  }

  public EnumSet<StreetMode> getModes() {
    return EnumSet.copyOf(modes);
  }

  /** Create a new PathTransfer based on the current one with the mode added to the valid modes. */
  public PathTransfer withAddedMode(StreetMode mode) {
    EnumSet<StreetMode> newModes = EnumSet.copyOf(modes);
    newModes.add(mode);
    return new PathTransfer(from, to, distanceMeters, edges, newModes);
  }

  public LineString getGeometry() {
    if (edges == null) {
      return GeometryUtils.getGeometryFactory().createLineString();
    } else {
      return GeometryUtils.concatenateLineStrings(edges, Edge::getGeometry);
    }
  }

  /** Check if the given mode is a valid mode for the transfer. */
  public boolean allowsMode(StreetMode mode) {
    return modes.contains(mode);
  }

  public Optional<DefaultRaptorTransfer> asRaptorTransfer(StreetSearchRequest request) {
    if (edges == null || edges.isEmpty()) {
      WalkRequest walkReq = request.walk();
      double durationSeconds = distanceMeters / walkReq.speed();
      final double domainCost = costLimitSanityCheck(durationSeconds * walkReq.reluctance());
      return Optional.of(
        new DefaultRaptorTransfer(
          to.getIndex(),
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
        to.getIndex(),
        (int) s.getElapsedTimeSeconds(),
        RaptorCostConverter.toRaptorCost(costLimitSanityCheck(s.getWeight())),
        this
      )
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(getClass())
      .addObj("from", from)
      .addObj("to", to)
      .addNum("distance", distanceMeters)
      .addColSize("edges", edges)
      .addColSize("modes", modes)
      .toString();
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
}
