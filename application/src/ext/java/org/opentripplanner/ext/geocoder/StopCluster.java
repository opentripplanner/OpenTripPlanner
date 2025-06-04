package org.opentripplanner.ext.geocoder;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A result of a fuzzy stop cluster geocoding search. A cluster is defined as a group of stops that
 * are related to one another.
 * <p>
 * Specifically this means that:
 * <p>
 *  - if a stop has a parent station only the parent is returned
 *  - if stops are closer than 10 meters to each and have an identical name, only one is returned
 */
record StopCluster(Location primary, Collection<Location> secondaries) {
  /**
   * Easily serializable version of a coordinate
   */
  public record Coordinate(double lat, double lon) {}

  /**
   * Easily serializable version of an agency
   */
  public record Agency(
    @JsonSerialize(using = FeedScopedIdSerializer.class) FeedScopedId id,
    String name
  ) {}

  /**
   * Easily serializable version of a feed publisher
   */
  public record FeedPublisher(String name) {}

  public enum LocationType {
    STATION,
    STOP,
  }

  public record Location(
    @JsonSerialize(using = FeedScopedIdSerializer.class) FeedScopedId id,
    @Nullable String code,
    LocationType type,
    String name,
    Coordinate coordinate,
    Collection<String> modes,
    List<Agency> agencies,
    @Nullable FeedPublisher feedPublisher
  ) {
    public Location {
      Objects.requireNonNull(id);
      Objects.requireNonNull(name);
      Objects.requireNonNull(type);
      Objects.requireNonNull(coordinate);
      Objects.requireNonNull(modes);
      Objects.requireNonNull(agencies);
    }
  }

  private static class FeedScopedIdSerializer extends JsonSerializer<FeedScopedId> {

    @Override
    public void serialize(FeedScopedId value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
      gen.writeString(value.toString());
    }
  }
}
