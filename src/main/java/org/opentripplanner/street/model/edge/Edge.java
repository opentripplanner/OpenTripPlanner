package org.opentripplanner.street.model.edge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the standard implementation of an edge with fixed from and to Vertex instances; all
 * standard OTP edges are subclasses of this.
 */
public abstract class Edge implements AStarEdge<State, Edge, Vertex>, Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(Edge.class);

  protected enum ConnectToGraph {
    CONNECT,
    TEMPORARY_EDGE_NOT_CONNECTED_TO_GRAPH,
  }

  protected final Vertex fromv;

  protected final Vertex tov;

  /**
   * Recommended constructor for creating an edge.
   * The edge is automatically added to the graph by updating the outgoing edge list of the
   * origin ("from") vertex and the incoming edge list of the destination ("to") vertex.
   * @param v1 origin vertex
   * @param v2 destination vertex
   */
  protected Edge(Vertex v1, Vertex v2) {
    this(v1, v2, ConnectToGraph.CONNECT);
  }

  /**
   * Constructor for creating an edge optionally disconnected from the graph.
   * This constructor should be used only for the special case of a
   * {@link org.opentripplanner.ext.flex.edgetype.FlexTripEdge}
   * that is intended to remain disconnected from the graph.
   * Use {@link Edge#Edge(Vertex, Vertex)} for the general use case.
   * The edge is optionally added to the graph by updating the outgoing edge list of the
   * origin ("from") vertex and the incoming edge list of the destination ("to") vertex.
   * @param v1 origin vertex
   * @param v2 destination vertex
   * @param connectToGraph if the edge should be connected to the graph
   */
  protected Edge(Vertex v1, Vertex v2, ConnectToGraph connectToGraph) {
    Objects.requireNonNull(connectToGraph);
    if (v1 == null || v2 == null) {
      String err = String.format(
        "%s constructed with null vertex : %s %s",
        this.getClass(),
        v1,
        v2
      );
      throw new IllegalStateException(err);
    }
    this.fromv = v1;
    this.tov = v2;
    if (connectToGraph == ConnectToGraph.CONNECT) {
      fromv.addOutgoing(this);
      tov.addIncoming(this);
    }
  }

  public final Vertex getFromVertex() {
    return fromv;
  }

  public final Vertex getToVertex() {
    return tov;
  }

  /**
   * Checks equivalency to another edge. Default implementation is trivial equality, but subclasses
   * may want to do something more tricky.
   */
  public boolean isEquivalentTo(Edge e) {
    return this == e;
  }

  /**
   * Returns true if this edge is the reverse of another.
   */
  public boolean isReverseOf(Edge e) {
    return (this.getFromVertex() == e.getToVertex() && this.getToVertex() == e.getFromVertex());
  }

  /**
   * While in destructive splitting mode (during graph construction rather than handling routing
   * requests), we remove edges that have been split and may then re-split the resulting segments
   * recursively, so parts of them are also removed. Newly created edge fragments are added to the
   * spatial index; the edges that were split are removed (disconnected) from the graph but were
   * previously not removed from the spatial index, so for all subsequent splitting operations we
   * had to check whether any edge coming out of the spatial index had been "soft deleted".
   * <p>
   * I believe this was compensating for the fact that STRTrees are optimized at construction and
   * read-only. That restriction no longer applies since we've been using our own hash grid spatial
   * index instead of the STRTree. So rather than filtering out soft deleted edges, this is now an
   * assertion that the system behaves as intended, and will log an error if the spatial index is
   * returning edges that have been disconnected from the graph.
   */
  public boolean isReachableFromGraph() {
    boolean edgeReachableFromGraph = tov.getIncoming().contains(this);
    if (!edgeReachableFromGraph) {
      LOG.warn(
        "Edge {} returned from spatial index is no longer reachable from graph. That is not expected.",
        this
      );
    }
    return edgeReachableFromGraph;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromv, tov);
  }

  public String toString() {
    return String.format("%s (%s -> %s)", getClass().getName(), fromv, tov);
  }

  /**
   * Edges are not roundabouts by default.
   */
  public boolean isRoundabout() {
    return false;
  }

  /**
   * Returns the default name of the edge
   */
  public String getDefaultName() {
    I18NString name = getName();
    return name != null ? name.toString() : null;
  }

  /**
   * Returns the name of the edge
   */
  public abstract I18NString getName();

  // TODO Add comments about what a "bogus name" is.
  public boolean hasBogusName() {
    return false;
  }

  // The next few functions used to live in EdgeNarrative, which has now been
  // removed
  // @author mattwigway

  public LineString getGeometry() {
    return null;
  }

  public double getDistanceMeters() {
    return 0;
  }

  /**
   * The distance to walk adjusted for elevation and obstacles. This is used together with the
   * walking speed to find the actual walking transfer time. This plus {@link
   * #getDistanceIndependentTime()} is used to calculate the actual-transfer-time given a walking
   * speed.
   * <p>
   * Unit: meters. Default: 0.
   */
  public double getEffectiveWalkDistance() {
    return 0;
  }

  /**
   * This is the transfer time(duration) spent NOT moving like time in in elevators, escalators and
   * waiting on read light when crossing a street. This is used together with {@link
   * #getEffectiveWalkDistance()} to calculate the actual-transfer-time.
   * <p>
   * Unit: seconds. Default: 0.
   */
  public int getDistanceIndependentTime() {
    return 0;
  }

  public void remove() {
    for (Edge edge : this.fromv.getIncoming()) {
      edge.removeTurnRestrictionsTo(this);
    }
    this.fromv.removeOutgoing(this);
    this.tov.removeIncoming(this);
  }

  public void removeTurnRestrictionsTo(Edge origin) {}

  /* SERIALIZATION */

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // edge lists are transient, reconstruct them
    fromv.addOutgoing(this);
    tov.addIncoming(this);
  }

  private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
    out.defaultWriteObject();
  }
}
