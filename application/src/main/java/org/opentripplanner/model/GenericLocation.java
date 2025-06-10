package org.opentripplanner.model;

import java.util.Objects;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.StringUtils;
import org.opentripplanner.utils.tostring.ValueObjectToStringBuilder;

/**
 * Represents a location that is to be used in a routing request. It can be either a from, to, or
 * intermediate location. This has to be resolved to a vertex or a collection of vertices before
 * routing can start.
 */
public class GenericLocation {

  public static final GenericLocation UNKNOWN = new GenericLocation(null, null, null, null);

  /**
   * A label for the place, if provided. This is pass-through information and does not affect
   * routing in any way.
   */
  @Nullable
  public final String label;

  /**
   * Refers to a specific element in the OTP model. This can currently be a regular stop, area stop,
   * group stop, station, multi-modal station or group of stations.
   */
  @Nullable
  public final FeedScopedId stopId;

  /**
   * Coordinates of the location. These can be used by themselves or as a fallback if placeId is not
   * found.
   */
  @Nullable
  public final Double lat;

  @Nullable
  public final Double lng;

  public GenericLocation(
    @Nullable String label,
    @Nullable FeedScopedId stopId,
    @Nullable Double lat,
    @Nullable Double lng
  ) {
    this.label = label;
    this.stopId = stopId;
    this.lat = lat;
    this.lng = lng;
  }

  public static GenericLocation fromStopId(String name, String feedId, String stopId) {
    return new GenericLocation(name, new FeedScopedId(feedId, stopId), null, null);
  }

  /**
   * Create a new location based on a coordinate - the input is primitive doubles to prevent
   * inserting {@code null} values.
   */
  public static GenericLocation fromCoordinate(double lat, double lng) {
    return new GenericLocation(null, null, lat, lng);
  }

  /**
   * Returns this as a Coordinate object.
   */
  @Nullable
  public Coordinate getCoordinate() {
    if (this.lat == null || this.lng == null) {
      return null;
    }
    return new Coordinate(this.lng, this.lat);
  }

  public boolean isSpecified() {
    return stopId != null || (lat != null && lng != null);
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
      Objects.equals(lat, that.lat) &&
      Objects.equals(lng, that.lng)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, stopId, lat, lng);
  }

  @Override
  public String toString() {
    if (UNKNOWN.equals(this)) {
      return "Unknown location";
    }

    ValueObjectToStringBuilder buf = ValueObjectToStringBuilder.of().skipNull();
    if (StringUtils.hasValue(label)) {
      buf.addText(label).addText(" ");
    }
    buf.addObj(stopId);
    buf.addCoordinate(lat, lng);
    return buf.toString();
  }
}
