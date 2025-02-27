package org.opentripplanner.routing.via.model;

import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.raptor.api.model.RaptorValueFormatter;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * This class will act as a {@link RaptorTransfer} during the Raptor routing and carry enough
 * information to create an itinerary after the routing is done.

 * When routing through a coordinate, we need to generate transfers for each pair of stops
 * connected to it. In this class we keep a reference to the incoming and outgoing paths, instead
 * of connecting the two paths together into one. This approach reduces memory usage during
 * routing. Connecting paths is left to the itinerary mapping.
 */
public class ViaCoordinateTransfer implements RaptorTransfer {

  private final WgsCoordinate coordinate;
  private final int fromStopIndex;
  private final int toStopIndex;
  private final List<Edge> fromEdges;
  private final List<Edge> toEdges;
  private final int durationInSeconds;
  private final int raptorCost;

  /**
   * @param coordinate the coordinate of given via point.
   * @param fromStopIndex The Raptor stop index to use for the arriving stop.
   * @param toStopIndex The Raptor stop index to use for the boarding stop.
   * @param fromEdges the street path FROM the alighting stop to the given via point.
   * @param toEdges the street path TO the departing stop from the given via point.
   * @param durationInSeconds How long it takes the traverse the from and to edges, exclusive
   *                          via `minimum-wait-time`.
   * @param generalizedCostInSeconds The total cost traversing the from and to edges in the OTP
   *                                 domain generalized-cost. The unit is equivalent to seconds.
   *                                 The cost will be converted to Raptor units and cached for
   *                                 optimal performance.
   */
  public ViaCoordinateTransfer(
    WgsCoordinate coordinate,
    int fromStopIndex,
    int toStopIndex,
    List<Edge> fromEdges,
    List<Edge> toEdges,
    int durationInSeconds,
    double generalizedCostInSeconds
  ) {
    this.coordinate = coordinate;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.fromEdges = fromEdges;
    this.toEdges = toEdges;
    this.durationInSeconds = durationInSeconds;
    this.raptorCost = RaptorCostConverter.toRaptorCost(generalizedCostInSeconds);
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public int fromStopIndex() {
    return fromStopIndex;
  }

  public List<Edge> fromEdges() {
    return fromEdges;
  }

  public List<Edge> toEdges() {
    return toEdges;
  }

  @Override
  public int stop() {
    return toStopIndex;
  }

  @Override
  public int c1() {
    return raptorCost;
  }

  @Override
  public int durationInSeconds() {
    return durationInSeconds;
  }

  @Override
  public String toString() {
    return (
      "{" +
      coordinate +
      " " +
      fromStopIndex +
      " ~ " +
      toStopIndex +
      " " +
      DurationUtils.durationToStr(durationInSeconds) +
      " " +
      RaptorValueFormatter.formatC1(raptorCost) +
      "}"
    );
  }
}
