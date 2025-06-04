package org.opentripplanner.street.model.vertex;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.RentalRestrictionExtension;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.AreaStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A vertex in the graph. Each vertex has a longitude/latitude location, as well as a set of
 * incoming and outgoing edges.
 */
public abstract class Vertex implements AStarVertex<State, Edge, Vertex>, Serializable, Cloneable {

  public static final I18NString NO_NAME = I18NString.of("(no name provided)");
  private static final Logger LOG = LoggerFactory.getLogger(Vertex.class);

  private final double x;
  private final double y;

  private transient Edge[] incoming = new Edge[0];

  private transient Edge[] outgoing = new Edge[0];
  private RentalRestrictionExtension rentalRestrictions = RentalRestrictionExtension.NO_RESTRICTION;

  /* CONSTRUCTORS */

  protected Vertex(double x, double y) {
    this.x = x;
    this.y = y;
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
    if (!rentalRestrictions.toList().isEmpty()) {
      sb.append(", traversalExtension=").append(rentalRestrictions);
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

  /**
   * Longer human-readable name for the client
   */
  public abstract I18NString getName();

  /**
   * If this vertex is located on only one street, get that street's name in default localization
   */
  public String getDefaultName() {
    return getName().toString();
  }

  /**
   *  Every vertex has a label which is globally unique.
   * <p>
   *  The name "label" is taken from graph theory: https://en.wikipedia.org/wiki/Graph_labeling
   */
  public abstract VertexLabel getLabel();

  /**
   * Return the label of the vertex converted to a string.
   *
   * @see Vertex#getLabel()
   */
  public String getLabelString() {
    return getLabel().toString();
  }

  /**
   * If applying turn restrictions to a graph has generated multiple instances of a vertex,
   * one of them is the parent, and the others are subsidiary vertices. Calling getParent()
   * on any of these will always return the same parent, which is used for example for
   * Edge.isReverseOf(Edge), so that it does not have to operate on geographical coordinates
   * and trust them for equality.
   *
   * @return The representative parent Vertex of a group of vertices that are same for
   *         most purposes.
   */
  public Vertex getParent() {
    return this;
  }

  /**
   * Return the position of the vertex as a WgsCoordinate.
   */
  public WgsCoordinate toWgsCoordinate() {
    return new WgsCoordinate(y, x);
  }

  public Coordinate getCoordinate() {
    return new Coordinate(getX(), getY());
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
   * Returns true if vertex is connected to another one by an edge
   */
  public boolean isConnected(Vertex v) {
    for (Edge e : outgoing) {
      if (e.getToVertex() == v) {
        return true;
      }
    }
    for (Edge e : incoming) {
      if (e.getFromVertex() == v) {
        return true;
      }
    }
    return false;
  }

  /**
   * Compare two vertices and return {@code true} if they are close together - have the same
   * location.
   * @see org.opentripplanner.framework.geometry.WgsCoordinate#sameLocation(WgsCoordinate)
   **/
  public boolean sameLocation(Vertex other) {
    return new WgsCoordinate(getLat(), getLon()).sameLocation(
      new WgsCoordinate(other.getLat(), other.getLon())
    );
  }

  public boolean rentalTraversalBanned(State currentState) {
    return rentalRestrictions.traversalBanned(currentState);
  }

  public void addRentalRestriction(RentalRestrictionExtension ext) {
    rentalRestrictions = rentalRestrictions.add(ext);
  }

  public RentalRestrictionExtension rentalRestrictions() {
    return rentalRestrictions;
  }

  public boolean rentalDropOffBanned(State currentState) {
    return rentalRestrictions.dropOffBanned(currentState);
  }

  public void removeRentalRestriction(RentalRestrictionExtension ext) {
    rentalRestrictions = rentalRestrictions.remove(ext);
  }

  /**
   * Returns the (flex) area stops that this vertex is inside.
   */
  public Set<AreaStop> areaStops() {
    return Set.of();
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
   * A helper method to avoid repeated code for outgoing and incoming lists. Synchronization
   * must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
   */
  private Edge[] removeEdge(Edge[] existing, Edge e) {
    int nfound = 0;
    for (Edge edge : existing) {
      if (edge == e) nfound++;
    }
    if (nfound == 0) {
      LOG.debug(
        "The edge {} has already been removed from this vertex {}, skipping removal",
        e,
        this
      );
      return existing;
    }
    if (nfound > 1) {
      LOG.warn(
        "There are multiple copies of the edge {} to be removed from this vertex {}",
        e,
        this
      );
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
