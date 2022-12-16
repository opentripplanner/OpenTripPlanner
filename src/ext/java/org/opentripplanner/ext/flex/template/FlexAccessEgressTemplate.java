package org.opentripplanner.ext.flex.template;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;

public abstract class FlexAccessEgressTemplate {

  protected final NearbyStop accessEgress;
  protected final FlexTrip trip;
  public final int fromStopIndex;
  public final int toStopIndex;
  protected final StopLocation transferStop;
  protected final int secondsFromStartOfTime;
  public final LocalDate serviceDate;
  protected final FlexPathCalculator calculator;
  private final FlexConfig flexConfig;

  /**
   * @param accessEgress  Path from origin to the point of boarding for this flex trip
   * @param trip          The FlexTrip used for this Template
   * @param fromStopIndex Stop sequence index where this FlexTrip is boarded
   * @param toStopIndex   The stop where this FlexTrip alights
   * @param transferStop  The stop location where this FlexTrip alights
   * @param date          The service date of this FlexTrip
   * @param calculator    Calculates the path and duration of the FlexTrip
   */
  FlexAccessEgressTemplate(
    NearbyStop accessEgress,
    FlexTrip trip,
    int fromStopIndex,
    int toStopIndex,
    StopLocation transferStop,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    this.accessEgress = accessEgress;
    this.trip = trip;
    this.fromStopIndex = fromStopIndex;
    this.toStopIndex = toStopIndex;
    this.transferStop = transferStop;
    this.secondsFromStartOfTime = date.secondsFromStartOfTime;
    this.serviceDate = date.serviceDate;
    this.calculator = calculator;
    this.flexConfig = config;
  }

  public StopLocation getTransferStop() {
    return transferStop;
  }

  public StopLocation getAccessEgressStop() {
    return accessEgress.stop;
  }

  /**
   * This method is very much the hot code path in the flex access/egress search so any optimization
   * here will lead to noticeable speedups.
   */
  public Stream<FlexAccessEgress> createFlexAccessEgressStream(
    Graph graph,
    TransitService transitService
  ) {
    if (transferStop instanceof RegularStop stop) {
      TransitStopVertex flexVertex = graph.getStopVertexForStopId(stop.getId());
      return Stream
        .of(getFlexAccessEgress(new ArrayList<>(), flexVertex, (RegularStop) transferStop))
        .filter(Objects::nonNull);
    }
    // transferStop is Location Area/Line
    else {
      double maxDistanceMeters =
        flexConfig.maxTransferDuration().getSeconds() *
        accessEgress.state.getRequest().preferences().walk().speed();

      return getTransfersFromTransferStop(transitService)
        .stream()
        .filter(pathTransfer -> pathTransfer.getDistanceMeters() <= maxDistanceMeters)
        .filter(transfer -> getFinalStop(transfer) != null)
        .map(transfer -> {
          List<Edge> edges = getTransferEdges(transfer);
          return getFlexAccessEgress(edges, getFlexVertex(edges.get(0)), getFinalStop(transfer));
        })
        .filter(Objects::nonNull);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(FlexAccessEgressTemplate.class)
      .addObj("accessEgress", accessEgress)
      .addObj("trip", trip)
      .addNum("fromStopIndex", fromStopIndex)
      .addNum("toStopIndex", toStopIndex)
      .addObj("transferStop", transferStop)
      .addServiceTime("secondsFromStartOfTime", secondsFromStartOfTime)
      .addDate("serviceDate", serviceDate)
      .addObj("calculator", calculator)
      .addObj("flexConfig", flexConfig)
      .toString();
  }

  /**
   * Get a list of edges used for transferring to and from the scheduled transit network. The edges
   * should be in the order of traversal of the state in the NearbyStop
   */
  protected abstract List<Edge> getTransferEdges(PathTransfer transfer);

  /**
   * Get the {@Link Stop} where the connection to the scheduled transit network is made.
   */
  protected abstract RegularStop getFinalStop(PathTransfer transfer);

  /**
   * Get the transfers to/from stops in the scheduled transit network from the beginning/end of the
   * flex ride for the access/egress.
   */
  protected abstract Collection<PathTransfer> getTransfersFromTransferStop(
    TransitService transitService
  );

  /**
   * Get the {@Link Vertex} where the flex ride ends/begins for the access/egress.
   */
  protected abstract Vertex getFlexVertex(Edge edge);

  /**
   * Get the times in seconds, before during and after the flex ride.
   */
  protected abstract int[] getFlexTimes(FlexTripEdge flexEdge, State state);

  /**
   * Get the FlexTripEdge for the flex ride.
   */
  protected abstract FlexTripEdge getFlexEdge(Vertex flexFromVertex, StopLocation transferStop);

  protected FlexAccessEgress getFlexAccessEgress(
    List<Edge> transferEdges,
    Vertex flexVertex,
    RegularStop stop
  ) {
    FlexTripEdge flexEdge = getFlexEdge(flexVertex, transferStop);

    // this code is a little repetitive but needed as a performance improvement. previously
    // the flex path was checked before this method was called. this meant that every path
    // was traversed twice leading to a noticeable slowdown.
    State state = flexEdge.traverse(accessEgress.state);
    if (state == null) {
      return null;
    }
    for (Edge e : transferEdges) {
      state = e.traverse(state);
      if (state == null) {
        return null;
      }
    }

    int[] times = getFlexTimes(flexEdge, state);

    return new FlexAccessEgress(
      stop,
      times[0],
      times[1],
      times[2],
      fromStopIndex,
      toStopIndex,
      secondsFromStartOfTime,
      trip,
      state,
      transferEdges.isEmpty()
    );
  }
}
