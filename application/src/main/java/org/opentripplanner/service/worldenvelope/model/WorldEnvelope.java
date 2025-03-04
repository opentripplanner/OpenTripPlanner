package org.opentripplanner.service.worldenvelope.model;

import java.io.Serializable;
import java.util.Optional;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class calculates borders of envelopes that can be also on 180th meridian.
 */
public class WorldEnvelope implements Serializable {

  private final WgsCoordinate lowerLeft;
  private final WgsCoordinate upperRight;
  private final WgsCoordinate meanCenter;
  private final WgsCoordinate transitMedianCenter;

  private WorldEnvelope(
    WgsCoordinate lowerLeft,
    WgsCoordinate upperRight,
    WgsCoordinate transitMedianCenter
  ) {
    this.transitMedianCenter = transitMedianCenter;
    this.lowerLeft = lowerLeft;
    this.upperRight = upperRight;
    this.meanCenter = calculateMeanCenter(lowerLeft, upperRight);
  }

  WorldEnvelope(
    double lowerLeftLatitude,
    double lowerLeftLongitude,
    double upperRightLatitude,
    double upperRightLongitude,
    WgsCoordinate transitMedianCenter
  ) {
    this(
      new WgsCoordinate(lowerLeftLatitude, lowerLeftLongitude),
      new WgsCoordinate(upperRightLatitude, upperRightLongitude),
      transitMedianCenter
    );
  }

  public static WorldEnvelopeBuilder of() {
    return new WorldEnvelopeBuilder();
  }

  public WgsCoordinate lowerLeft() {
    return lowerLeft;
  }

  public WgsCoordinate upperRight() {
    return upperRight;
  }

  /**
   * If transit data exist, then this is the median center of the transit stops. The median
   * is computed independently for the longitude and latitude.
   * <p>
   * If not transit data exist this return `empty`.
   */
  public WgsCoordinate center() {
    return medianCenter().orElse(meanCenter);
  }

  /**
   * This is the center of the Envelope including both street vertexes and transit stops
   * if they exist.
   */
  public WgsCoordinate meanCenter() {
    return meanCenter;
  }

  /**
   * Return the transit median center [if it exist] or the mean center.
   */
  public Optional<WgsCoordinate> medianCenter() {
    return Optional.ofNullable(transitMedianCenter);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(WorldEnvelope.class)
      .addObj("lowerLeft", lowerLeft)
      .addObj("upperRight", upperRight)
      .addObj("meanCenter", meanCenter)
      .addObj("transitMedianCenter", transitMedianCenter)
      .toString();
  }

  private static WgsCoordinate calculateMeanCenter(
    WgsCoordinate lowerLeft,
    WgsCoordinate upperRight
  ) {
    var llLatitude = lowerLeft.latitude();
    var llLongitude = lowerLeft.longitude();
    var urLatitude = upperRight.latitude();
    var urLongitude = upperRight.longitude();

    double centerLatitude = llLatitude + (urLatitude - llLatitude) / 2.0;

    // Split normally at 180 degrees
    double centerLongitude = (llLongitude < urLongitude)
      ? llLongitude + (urLongitude - llLongitude) / 2.0
      : llLongitude + (360 - llLongitude + urLongitude) / 2.0;

    return new WgsCoordinate(centerLatitude, centerLongitude);
  }
}
