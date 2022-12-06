package org.opentripplanner.framework.geometry;

import static org.opentripplanner.framework.lang.OtpNumberFormat.formatTwoDecimals;

import java.io.Serializable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * This class calculates borders of envelopes that can be also on 180th meridian The same way as it
 * was previously calculated in GraphMetadata constructor
 */
public class WorldEnvelope implements Serializable {

  private static final double LAT_MIN = -90;
  private static final double LAT_MAX = 90;
  private static final double LON_MIN = -180;
  private static final double LON_MAX = 180;

  private final double lowerLeftLatitude;
  private final double lowerLeftLongitude;
  private final double upperRightLatitude;
  private final double upperRightLongitude;

  private final double centerLatitude;
  private final double centerLongitude;

  private WorldEnvelope(
    double lowerLeftLongitude,
    double lowerLeftLatitude,
    double upperRightLongitude,
    double upperRightLatitude
  ) {
    this.lowerLeftLatitude =
      DoubleUtils.assertInRange(lowerLeftLatitude, LAT_MIN, LAT_MAX, "lowerLeftLatitude");
    this.lowerLeftLongitude =
      DoubleUtils.assertInRange(lowerLeftLongitude, LON_MIN, LON_MAX, "lowerLeftLongitude");
    this.upperRightLatitude =
      DoubleUtils.assertInRange(upperRightLatitude, LAT_MIN, LAT_MAX, "upperRightLatitude");
    this.upperRightLongitude =
      DoubleUtils.assertInRange(upperRightLongitude, LON_MIN, LON_MAX, "upperRightLongitude");
    this.centerLatitude = lowerLeftLatitude + (upperRightLatitude - lowerLeftLatitude) / 2.0;

    // Split normally at 180 degrees
    this.centerLongitude =
      (lowerLeftLongitude < upperRightLongitude)
        ? lowerLeftLongitude + (upperRightLongitude - lowerLeftLongitude) / 2.0
        : lowerLeftLongitude + (360 - lowerLeftLongitude + upperRightLongitude) / 2.0;
  }

  public static WorldEnvelope.Builder of() {
    return new Builder();
  }

  public double getLowerLeftLongitude() {
    return lowerLeftLongitude;
  }

  public double getLowerLeftLatitude() {
    return lowerLeftLatitude;
  }

  public double getUpperRightLongitude() {
    return upperRightLongitude;
  }

  public double getUpperRightLatitude() {
    return upperRightLatitude;
  }

  public double centerLatitude() {
    return centerLatitude;
  }

  public double centerLongitude() {
    return centerLongitude;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(WorldEnvelope.class)
      .addObj("lowerLeft", coordinateToString(lowerLeftLatitude, lowerLeftLongitude))
      .addObj("upperRight", coordinateToString(upperRightLatitude, upperRightLongitude))
      .addObj("center", coordinateToString(centerLatitude, centerLongitude))
      .toString();
  }

  private static String coordinateToString(double lat, double lon) {
    return "(" + formatTwoDecimals(lat) + " " + formatTwoDecimals(lon) + ")";
  }

  /**
   * Calculates lower/upper right/left latitude and longitude of all the coordinates.
   * <p>
   * This takes into account that envelope can extend over 180th meridian
   */
  public static class Builder {

    private static final Double MIN_NOT_SET = 9999d;
    private static final Double MAX_NOT_SET = -9999d;

    private double minLat = MIN_NOT_SET;
    private double maxLat = MAX_NOT_SET;
    private double minLonWest = MIN_NOT_SET;
    private double maxLonWest = MAX_NOT_SET;
    private double minLonEast = MIN_NOT_SET;
    private double maxLonEast = MAX_NOT_SET;

    public Builder expandToInclude(Coordinate c) {
      return this.expandToInclude(c.y, c.x);
    }

    // TODO - Swap args
    public Builder expandToInclude(double latitude, double longitude) {
      minLat = Math.min(minLat, latitude);
      maxLat = Math.max(maxLat, latitude);

      if (longitude < 0) {
        minLonWest = Math.min(minLonWest, longitude);
        maxLonWest = Math.max(maxLonWest, longitude);
      } else {
        minLonEast = Math.min(minLonEast, longitude);
        maxLonEast = Math.max(maxLonEast, longitude);
      }
      return this;
    }

    public WorldEnvelope build() {
      if (minLonWest == MIN_NOT_SET) {
        return new WorldEnvelope(minLonEast, minLat, maxLonEast, maxLat);
      } else if (minLonEast == MIN_NOT_SET) {
        return new WorldEnvelope(minLonWest, minLat, maxLonWest, maxLat);
      } else {
        double dist0 = minLonEast - minLonWest;
        double dist180 = 360d - maxLonEast + minLonWest;

        // A small gap between the east and west longitude at 0 degrees implies that the Envelope
        // should include the 0 degrees longitude(meridian), and be split at 180 degrees.
        if (dist0 < dist180) {
          return new WorldEnvelope(maxLonWest, minLat, maxLonEast, maxLat);
        } else {
          return new WorldEnvelope(minLonEast, minLat, minLonWest, maxLat);
        }
      }
    }
  }
}
