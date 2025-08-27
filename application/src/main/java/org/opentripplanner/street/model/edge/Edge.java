package org.opentripplanner.street.model.edge;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nullable;
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

  protected final Vertex fromv;

  protected final Vertex tov;

  /**
   * Create an edge from the origin vertex to the destination vertex.
   * The created edge is disconnected from the graph.
   * Call {@link #connectToGraph()} to connect the edge to the graph.
   * @param v1 origin vertex
   * @param v2 destination vertex
   */
  protected Edge(Vertex v1, Vertex v2) {
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
  public final boolean isReverseOf(Edge e) {
    return (
      this.getFromVertex().getParent() == e.getToVertex().getParent() &&
      this.getToVertex().getParent() == e.getFromVertex().getParent()
    );
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

  @Override
  public String toString() {
    return buildToString(null, b -> {});
  }

  /**
   * Use this to construct a {@code toString()} in derived classes. The result will be:
   * {@code "%simple-class-name%(%name%, %fromv% -> %tov%" + body + ")"}. Note! There is no space
   * between {@code tov} and {@code body}. Both name and body are optional.
   *
   * @param name Will add name to the string if not null.
   * @param body Use this callback to add local content.
   */
  protected String buildToString(@Nullable String name, Consumer<StringBuilder> body) {
    var buf = new StringBuilder();
    buf.append(getClass().getSimpleName()).append("(");
    if (name != null) {
      buf.append(name).append(", ");
    }
    buf.append(fromv).append(" -> ").append(tov);
    body.accept(buf);
    return buf.append(')').toString();
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

  /**
   * Returns true if this edge has a generated name that is derived from its properties,
   * like "path", "stairs" or "tunnel".
   * <p>
   * Returns false if the field reflects the real world name, like "Fifth Avenue",
   * "Hauptstraße" or "Øvre Holmegate".
   */
  public boolean nameIsDerived() {
    return false;
  }

  public LineString getGeometry() {
    return null;
  }

  public boolean hasGeometry() {
    return getGeometry() != null;
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

  public void remove() {
    this.fromv.removeOutgoing(this);
    this.tov.removeIncoming(this);
  }

  /**
   * Connect the edge to the graph by adding it to the list of outgoing edges of the origin vertex
   * and the list of incoming edges of the destination vertex. Once connected, the edge becomes
   * visible from other threads. This should not be done inside the constructor, otherwise the edge
   * might become reachable before being fully constructed.
   */
  protected void connectToGraph() {
    fromv.addOutgoing(this);
    tov.addIncoming(this);
  }

  protected static <T extends Edge> T connectToGraph(T edge) {
    edge.connectToGraph();
    return edge;
  }

  /* SERIALIZATION */

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // edge lists are transient, reconstruct them
    connectToGraph();
  }

  private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
    out.defaultWriteObject();
  }
}
