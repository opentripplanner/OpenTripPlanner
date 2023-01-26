package org.opentripplanner.street.model.vertex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.state.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A vertex in the graph. Each vertex has a longitude/latitude location, as well as a set of
 * incoming and outgoing edges.
 */
public abstract class Vertex implements AStarVertex<State, Edge, Vertex>, Serializable, Cloneable {

  private static final Logger LOG = LoggerFactory.getLogger(Vertex.class);

  /**
   * Short debugging name. This is a graph mathematical term as in https://en.wikipedia.org/wiki/Graph_labeling
   */
  private final String label;
  private final double x;
  private final double y;
  /* Longer human-readable name for the client */
  private I18NString name;
  private transient Edge[] incoming = new Edge[0];

  private transient Edge[] outgoing = new Edge[0];

  /* CONSTRUCTORS */

  protected Vertex(Graph g, String label, double x, double y) {
    this.label = label;
    this.x = x;
    this.y = y;
    // null graph means temporary vertex
    if (g != null) {
      g.addVertex(this);
    }
    this.name = new NonLocalizedString("(no name provided)");
  }

  protected Vertex(Graph g, String label, double x, double y, I18NString name) {
    this(g, label, x, y);
    this.name = name;
  }

  /* PUBLIC METHODS */

  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("{").append(this.getLabel());
    if (this.getCoordinate() != null) {
      sb.append(" lat,lng=").append(this.getCoordinate().y);
      sb.append(",").append(this.getCoordinate().x);
    }
    sb.append("}");
    return sb.toString();
  }

  public void initEdgeLists() {
    this.outgoing = new Edge[0];
    this.incoming = new Edge[0];
  }

  /* EDGE UTILITY METHODS (use arrays to eliminate copy-on-write set objects) */

  public void addOutgoing(Edge edge) {
    synchronized (this) {
      outgoing = addEdge(outgoing, edge);
    }
  }

  /** @return whether the edge was found and removed. */
  public boolean removeOutgoing(Edge edge) {
    synchronized (this) {
      int n = outgoing.length;
      outgoing = removeEdge(outgoing, edge);
      return (outgoing.length < n);
    }
  }

  public void addIncoming(Edge edge) {
    synchronized (this) {
      incoming = addEdge(incoming, edge);
    }
  }

  /** @return whether the edge was found and removed. */
  public boolean removeIncoming(Edge edge) {
    synchronized (this) {
      int n = incoming.length;
      incoming = removeEdge(incoming, edge);
      return (incoming.length < n);
    }
  }

  public Collection<Edge> getOutgoing() {
    return Arrays.asList(outgoing);
  }

  public Collection<Edge> getIncoming() {
    return Arrays.asList(incoming);
  }

  public int getDegreeOut() {
    return outgoing.length;
  }

  public int getDegreeIn() {
    return incoming.length;
  }

  /** Get the longitude of the vertex */
  public final double getX() {
    return getLon();
  }

  /** Get the latitude of the vertex */
  public final double getY() {
    return getLat();
  }

  /** Get the longitude of the vertex */
  public final double getLon() {
    return x;
  }

  /** Get the latitude of the vertex */
  public final double getLat() {
    return y;
  }

  /** If this vertex is located on only one street, get that street's name */
  public I18NString getName() {
    return this.name;
  }

  /**
   * If this vertex is located on only one street, get that street's name in default localization
   */
  public String getDefaultName() {
    return this.name.toString();
  }

  /** Every vertex has a label which is globally unique. */
  public String getLabel() {
    return label;
  }

  public Coordinate getCoordinate() {
    return new Coordinate(getX(), getY());
  }

  /** Get the bearing, in degrees, between this vertex and another coordinate. */
  public double azimuthTo(Coordinate other) {
    return DirectionUtils.getAzimuth(getCoordinate(), other);
  }

  /** Get the bearing, in degrees, between this vertex and another. */
  public double azimuthTo(Vertex other) {
    return azimuthTo(other.getCoordinate());
  }

  public List<StreetEdge> getIncomingStreetEdges() {
    List<StreetEdge> result = new ArrayList<>();
    for (Edge out : this.getIncoming()) {
      if (!(out instanceof StreetEdge)) {
        continue;
      }
      result.add((StreetEdge) out);
    }
    return result;
  }

  public List<StreetEdge> getOutgoingStreetEdges() {
    List<StreetEdge> result = new ArrayList<>();
    for (Edge out : this.getOutgoing()) {
      if (!(out instanceof StreetEdge)) {
        continue;
      }
      result.add((StreetEdge) out);
    }
    return result;
  }

  /**
   * A static helper method to avoid repeated code for outgoing and incoming lists. Synchronization
   * must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
   */
  private static Edge[] addEdge(Edge[] existing, Edge e) {
    Edge[] copy = new Edge[existing.length + 1];
    int i;
    for (i = 0; i < existing.length; i++) {
      if (existing[i] == e) {
        LOG.error("repeatedly added edge {}", e);
        return existing;
      }
      copy[i] = existing[i];
    }
    copy[i] = e; // append the new edge to the copy of the existing array
    return copy;
  }

  /**
   * A static helper method to avoid repeated code for outgoing and incoming lists. Synchronization
   * must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
   */
  private static Edge[] removeEdge(Edge[] existing, Edge e) {
    int nfound = 0;
    for (int i = 0, j = 0; i < existing.length; i++) {
      if (existing[i] == e) nfound++;
    }
    if (nfound == 0) {
      LOG.error("Requested removal of an edge which isn't connected to this vertex.");
      return existing;
    }
    if (nfound > 1) {
      LOG.error("There are multiple copies of the edge to be removed.)");
    }
    Edge[] copy = new Edge[existing.length - nfound];
    for (int i = 0, j = 0; i < existing.length; i++) {
      if (existing[i] != e) {
        copy[j++] = existing[i];
      }
    }
    return copy;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    // edge lists are transient
    out.defaultWriteObject();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    this.incoming = new Edge[0];
    this.outgoing = new Edge[0];
  }
}
