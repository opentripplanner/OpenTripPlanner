package org.opentripplanner.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.api.request.TripLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * Represents a location that is to be used in a routing request. It can be either a from, to, or
 * intermediate location. This has to be resolved to a vertex or a collection of vertices before
 * routing can start.
 */
public class GenericLocation {

  @Nullable
  private final String label;

  @Nullable
  private final FeedScopedId stopId;

  @Nullable
  private final WgsCoordinate coordinate;

  @Nullable
  private final TripLocation tripLocation;

  private GenericLocation(
    @Nullable String label,
    @Nullable FeedScopedId stopId,
    @Nullable WgsCoordinate coordinate,
    @Nullable TripLocation tripLocation
  ) {
    if (stopId == null && coordinate == null && tripLocation == null) {
      throw new IllegalArgumentException(
        "GenericLocation requires either a stop id, a coordinate or a trip location"
      );
    }
    this.label = label;
    this.stopId = stopId;
    this.coordinate = coordinate;
    this.tripLocation = tripLocation;
  }

  public static GenericLocation fromStopId(FeedScopedId id) {
    Objects.requireNonNull(id);
    return new GenericLocation(null, id, null, null);
  }

  public static GenericLocation fromStopId(FeedScopedId id, @Nullable String label) {
    Objects.requireNonNull(id);
    return new GenericLocation(label, id, null, null);
  }

  /// Create a GenericLocation of a stop id with fallback coordinates if the id is not found.
  public static GenericLocation fromStopIdWithFallback(
    FeedScopedId id,
    double lat,
    double lng,
    @Nullable String label
  ) {
    Objects.requireNonNull(id);
    return new GenericLocation(label, id, new WgsCoordinate(lat, lng), null);
  }

  public static GenericLocation fromCoordinate(WgsCoordinate coordinate) {
    return new GenericLocation(null, null, coordinate, null);
  }

  public static GenericLocation fromCoordinate(WgsCoordinate coordinate, @Nullable String label) {
    return new GenericLocation(label, null, coordinate, null);
  }

  /**
   * Create a new location based on a coordinate - the input is primitive doubles to prevent
   * inserting {@code null} values.
   */
  public static GenericLocation fromCoordinate(double lat, double lng) {
    return new GenericLocation(null, null, new WgsCoordinate(lat, lng), null);
  }

  public static GenericLocation fromCoordinate(double lat, double lng, @Nullable String label) {
    return new GenericLocation(label, null, new WgsCoordinate(lat, lng), null);
  }

  /**
   * Create a new location based on a trip location. See {@link #tripLocation}.
   */
  public static GenericLocation fromTripLocation(TripLocation tripLocation) {
    return new GenericLocation(null, null, null, tripLocation);
  }

  public static GenericLocation fromTripLocation(
    TripLocation tripLocation,
    @Nullable String label
  ) {
    return new GenericLocation(label, null, null, tripLocation);
  }

  /**
   * Coordinates of the location. These can be used by themselves or as a fallback if placeId is not
   * found.
   */
  @Nullable
  public Coordinate getCoordinate() {
    if (this.coordinate == null) {
      return null;
    }
    return coordinate.asJtsCoordinate();
  }

  /**
   * Returns true if this location represents a position on-board a transit vehicle
   * rather than a geographic location. On-board locations have no coordinates and
   * are not linked to the street network.
   */
  public boolean isOnBoard() {
    return tripLocation != null;
  }

  /**
   * Coordinates of the location. These can be used by themselves or as a fallback if placeId is not
   * found.
   */
  @Nullable
  public WgsCoordinate wgsCoordinate() {
    return coordinate;
  }

  /**
   * Refers to a specific element in the OTP model. This can currently be a regular stop, area stop,
   * group stop, station, multi-modal station or group of stations.
   */
  @Nullable
  public FeedScopedId stopId() {
    return stopId;
  }

  /**
   * When set, the location is on-board a specific trip.
   */
  @Nullable
  public TripLocation tripLocation() {
    return tripLocation;
  }

  /**
   * A label for the place, if provided. This is pass-through information and does not affect
   * routing in any way.
   */
  @Nullable
  public String label() {
    return label;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    var that = (GenericLocation) o;
    return (
      Objects.equals(label, that.label) &&
      Objects.equals(stopId, that.stopId) &&
      Objects.equals(coordinate, that.coordinate) &&
      Objects.equals(tripLocation, that.tripLocation)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, stopId, coordinate, tripLocation);
  }

  @Override
  public String toString() {
    ValueObjectToStringBuilder buf = ValueObjectToStringBuilder.of().skipNull();
    if (StringUtils.hasValue(label)) {
      buf.addText(label).addText(" ");
    }
    buf.addObj(stopId);
    if (coordinate != null) {
      buf.addCoordinate(coordinate.latitude(), coordinate.longitude());
    }
    buf.addObj(tripLocation);
    return buf.toString();
  }
}
