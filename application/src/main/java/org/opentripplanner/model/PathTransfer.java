package org.opentripplanner.model;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.List;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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

  public final StopLocation from;

  public final StopLocation to;

  private final double distanceMeters;

  private final List<Edge> edges;

  private final EnumSet<StreetMode> modes;

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

  public String getName() {
    return from + " => " + to;
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
}
