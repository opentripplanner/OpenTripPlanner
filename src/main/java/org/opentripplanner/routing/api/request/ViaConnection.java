package org.opentripplanner.routing.api.request;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A ViaConnection is a reference to a location or a coordinate. Supported locations are stop,
 * station, multimodal-station and group-of-stations. Either the {@code locationId} or the
 * {@code coordinate} must be set.
 * <p>
 * Earlier the coordinate was used as a fallback for the location-id, this is not the case anymore.
 * Any inconsistencies in location ids between the client and the server should be detected
 * and fixed - not automatically patched. If the client wants to provide a fallback, it could
 * fire a new request with the coordinate set instead.
 *
 */
public class ViaConnection {

  private final FeedScopedId locationId;
  private final WgsCoordinate coordinate;

  private ViaConnection(@Nullable FeedScopedId locationId, @Nullable WgsCoordinate coordinate) {
    this.locationId = locationId;
    this.coordinate = coordinate;
  }

  public ViaConnection(FeedScopedId locationId) {
    this(Objects.requireNonNull(locationId), null);
  }

  public ViaConnection(WgsCoordinate coordinate) {
    this(null, Objects.requireNonNull(coordinate));
  }

  public static List<ViaConnection> connections(List<FeedScopedId> ids) {
    return ids.stream().map(ViaConnection::new).toList();
  }

  public boolean hasLocationId() {
    return locationId != null;
  }

  public boolean hasCoordinate() {
    return coordinate != null;
  }

  public FeedScopedId locationId() {
    return locationId;
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ViaConnection that = (ViaConnection) o;
    return (
      Objects.equals(locationId, that.locationId) && Objects.equals(coordinate, that.coordinate)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(locationId, coordinate);
  }

  @SuppressWarnings("DataFlowIssue")
  @Override
  public String toString() {
    return locationId != null ? locationId.toString() : coordinate.toString();
  }
}
