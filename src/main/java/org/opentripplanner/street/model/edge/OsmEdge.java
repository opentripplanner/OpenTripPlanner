package org.opentripplanner.street.model.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.geometry.DirectionUtils;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.vertex.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for edges derived from OSM data.
 */
public abstract class OsmEdge extends Edge {

  private static final Logger LOG = LoggerFactory.getLogger(OsmEdge.class);

  /**
   * The angle at the start of the edge geometry. Internal representation is -180 to +179 integer
   * degrees mapped to -128 to +127 (brads)
   */
  private byte inAngle;

  /** The angle at the start of the edge geometry. Internal representation like that of inAngle. */
  private byte outAngle;

  protected OsmEdge(Vertex v1, Vertex v2, LineString geometry) {
    super(v1, v2);
    if (geometry != null) {
      try {
        for (Coordinate c : geometry.getCoordinates()) {
          if (Double.isNaN(c.x)) {
            System.out.println("X DOOM");
          }
          if (Double.isNaN(c.y)) {
            System.out.println("Y DOOM");
          }
        }
        // Conversion from radians to internal representation as a single signed byte.
        // We also reorient the angles since OTP seems to use South as a reference
        // while the azimuth functions use North.
        // FIXME Use only North as a reference, not a mix of North and South!
        // Range restriction happens automatically due to Java signed overflow behavior.
        // 180 degrees exists as a negative rather than a positive due to the integer range.
        outAngle = calculateAngle(DirectionUtils.getLastAngle(geometry));
        inAngle = calculateAngle(DirectionUtils.getFirstAngle(geometry));
      } catch (IllegalArgumentException iae) {
        LOG.error(
          "exception while determining street edge angles. setting to zero. there is probably something wrong with this street segment's geometry."
        );
        inAngle = 0;
        outAngle = 0;
      }
    }
  }

  /**
   * Set the bicycle safety factor.
   */
  public abstract void setBicycleSafetyFactor(float bicycleSafety);

  /**
   * Set the walk safety factor.
   */
  public abstract void setWalkSafetyFactor(float walkSafety);

  /**
   * Set if this edge is no-thru for motor vehicles.
   */
  public abstract void setMotorVehicleNoThruTraffic(boolean motorVehicleNoThrough);

  /**
   * Set if this edge is no-thru for bicycles.
   */
  public abstract void setBicycleNoThruTraffic(boolean bicycleNoThrough);

  public abstract void setWalkNoThruTraffic(boolean walkNoThrough);

  /**
   * Return the azimuth of the last segment in this edge in integer degrees clockwise from South.
   */
  public int getOutAngle() {
    return (int) Math.round(this.outAngle * 180 / 128.0);
  }

  /**
   * Return the azimuth of the first segment in this edge in integer degrees clockwise from South.
   * TODO change everything to clockwise from North
   */
  public int getInAngle() {
    return (int) Math.round(this.inAngle * 180 / 128.0);
  }

  /**
   * Add a turn restriction to this edge.
   */
  public abstract void addTurnRestriction(TurnRestriction restriction);

  /**
   * Deduplicate data of two edges (possibly there and back) in memory.
   */
  public void shareData(Edge reversedEdge) {}

  private static byte calculateAngle(double lastAngle) {
    return (byte) Math.round(lastAngle * 128 / Math.PI + 128);
  }
}
