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

/**
 * This is the standard implementation of an edge with fixed from and to Vertex instances; all
 * standard OTP edges are subclasses of this.
 */
public abstract class Edge implements AStarEdge<State, Edge, Vertex>, Serializable {

  protected Vertex fromv;

  protected Vertex tov;

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
    fromv.addOutgoing(this);
    tov.addIncoming(this);
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
   * Traverse this edge.
   *
   * @param s0 The State coming into the edge.
   * @return The State upon exiting the edge.
   */
  public abstract State traverse(State s0);

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
    if (this.fromv != null) {
      for (Edge edge : this.fromv.getIncoming()) {
        edge.removeTurnRestrictionsTo(this);
      }

      this.fromv.removeOutgoing(this);
      this.fromv = null;
    }

    if (this.tov != null) {
      this.tov.removeIncoming(this);
      this.tov = null;
    }
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
    if (fromv == null) {
      System.out.printf("fromv null %s \n", this);
    }
    if (tov == null) {
      System.out.printf("tov null %s \n", this);
    }
    out.defaultWriteObject();
  }
}
