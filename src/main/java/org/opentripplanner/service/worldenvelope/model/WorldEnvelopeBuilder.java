package org.opentripplanner.service.worldenvelope.model;

import java.util.Collection;
import java.util.function.Function;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Calculates lower/upper right/left latitude and longitude of all the coordinates.
 * <p>
 * This takes into account that envelope can extend over 180th meridian
 */
public class WorldEnvelopeBuilder {

  private static final Double MIN_NOT_SET = 9999d;
  private static final Double MAX_NOT_SET = -9999d;

  /**
   * We need to set a centroid for the WorldEnvelope when there is no data. So, we choose a random
   * place in europe for this - not in the middle of the see outside Africa (0.0, 0.0). This might
   * be confusing to some, but if you read the logs it should be obvious that you have bigger
   * problems ... no stops exist.
   */
  private static final WgsCoordinate A_PLACE_IN_EUROPE = new WgsCoordinate(47.101, 9.611);

  private double minLat = MIN_NOT_SET;
  private double maxLat = MAX_NOT_SET;
  private double minLonWest = MIN_NOT_SET;
  private double maxLonWest = MAX_NOT_SET;
  private double minLonEast = MIN_NOT_SET;
  private double maxLonEast = MAX_NOT_SET;

  private WgsCoordinate transitMedianCenter = null;

  public WorldEnvelopeBuilder expandToIncludeStreetEntities(double latitude, double longitude) {
    return this.expandToInclude(latitude, longitude);
  }

  /**
   * Calculates the center from median of coordinates of the elements in the given collection.
   * <p>
   * This speeds up calculation, but problem is that median needs to have all latitudes/longitudes
   * in memory, this can become problematic in large installations. It works without a issues on New
   * York State.
   */
  public <T> WorldEnvelopeBuilder expandToIncludeTransitEntities(
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
    if (minLonWest == MIN_NOT_SET && minLonEast == MIN_NOT_SET) {
      return new WorldEnvelope(-90.0, -180, 90, 180, A_PLACE_IN_EUROPE);
    }
    if (minLonWest == MIN_NOT_SET) {
      return new WorldEnvelope(minLat, minLonEast, maxLat, maxLonEast, transitMedianCenter);
    }
    if (minLonEast == MIN_NOT_SET) {
      return new WorldEnvelope(minLat, minLonWest, maxLat, maxLonWest, transitMedianCenter);
    }
    // Envelope intersects with either 0º or 180º
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

  private WorldEnvelopeBuilder expandToInclude(double latitude, double longitude) {
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
