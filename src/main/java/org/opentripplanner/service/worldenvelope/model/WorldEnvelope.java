package org.opentripplanner.service.worldenvelope.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.framework.tostring.ToStringBuilder;

/**
 * This class calculates borders of envelopes that can be also on 180th meridian The same way as it
 * was previously calculated in GraphMetadata constructor
 */
public class WorldEnvelope implements Serializable {

  private final WgsCoordinate lowerLeft;
  private final WgsCoordinate upperRight;
  private final WgsCoordinate meanCenter;
  private final WgsCoordinate transitMedianCenter;

  private WorldEnvelope(
    double lowerLeftLatitude,
    double lowerLeftLongitude,
    double upperRightLatitude,
    double upperRightLongitude,
    WgsCoordinate transitMedianCenter
  ) {
    this.transitMedianCenter = transitMedianCenter;
    this.lowerLeft = new WgsCoordinate(lowerLeftLatitude, lowerLeftLongitude);
    this.upperRight = new WgsCoordinate(upperRightLatitude, upperRightLongitude);
    this.meanCenter = calculateMeanCenter(lowerLeft, upperRight);
  }

  public static WorldEnvelope.Builder of() {
    return new Builder();
  }

  public WgsCoordinate lowerLeft() {
    return lowerLeft;
  }

  public WgsCoordinate upperRight() {
    return upperRight;
  }

  /**
   * This is the center of the Envelope including both street vertexes and transit stops
   * if they exist.
   */
  public WgsCoordinate meanCenter() {
    return meanCenter;
  }

  /**
   * If transit data exist, then this is the median center of the transit stops. The median
   * is computed independently for the longitude and latitude.
   * <p>
   * If not transit data exist this return `empty`.
   */
  public WgsCoordinate center() {
    return transitMedianCenter().orElse(meanCenter);
  }

  /**
   * Return the transit median center [if it exist] or the mean center.
   */
  public Optional<WgsCoordinate> transitMedianCenter() {
    return Optional.ofNullable(transitMedianCenter);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(WorldEnvelope.class)
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

    private WgsCoordinate transitMedianCenter = null;

    public Builder expandToIncludeStreetEntities(double latitude, double longitude) {
      return this.expandToInclude(latitude, longitude);
    }

    /**
     * Calculates the center from median of coordinates of the elements in the given collection.
     * <p>
     * This speeds up calculation, but problem is that median needs to have all latitudes/longitudes
     * in memory, this can become problematic in large installations. It works without a issues on
     * New York State.
     */
    public <T> Builder expandToIncludeTransitEntities(
      Collection<T> collection,
      Function<T, Double> latProvider,
      Function<T, Double> lonProvider
    ) {
      if (collection.isEmpty()) {
        return this;
      }

      // Expand Envelope
      for (T it : collection) {
        expandToInclude(latProvider.apply(it), lonProvider.apply(it));
      }

      // we need this check because there could be only AreaStops (which don't have vertices)
      // in the graph
      var medianCalculator = new MedianCalcForDoubles(collection.size());

      collection.forEach(v -> medianCalculator.add(lonProvider.apply(v)));
      double lon = medianCalculator.median();

      medianCalculator.reset();
      collection.forEach(v -> medianCalculator.add(latProvider.apply(v)));
      double lat = medianCalculator.median();

      this.transitMedianCenter = new WgsCoordinate(lat, lon);
      return this;
    }

    public WorldEnvelope build() {
      if (minLonWest == MIN_NOT_SET) {
        return new WorldEnvelope(minLat, minLonEast, maxLat, maxLonEast, transitMedianCenter);
      } else if (minLonEast == MIN_NOT_SET) {
        return new WorldEnvelope(minLat, minLonWest, maxLat, maxLonWest, transitMedianCenter);
      } else {
        double dist0 = minLonEast - minLonWest;
        double dist180 = 360d - maxLonEast + minLonWest;

        // A small gap between the east and west longitude at 0 degrees implies that the Envelope
        // should include the 0 degrees longitude(meridian), and be split at 180 degrees.
        if (dist0 < dist180) {
          return new WorldEnvelope(minLat, maxLonWest, maxLat, maxLonEast, transitMedianCenter);
        } else {
          return new WorldEnvelope(minLat, minLonEast, maxLat, minLonWest, transitMedianCenter);
        }
      }
    }

    private Builder expandToInclude(double latitude, double longitude) {
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
  }
}
