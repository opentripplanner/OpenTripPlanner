package org.opentripplanner.osm.wayproperty;

import java.util.Objects;
import java.util.OptionalDouble;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Parameters applied to OSM ways, usually based on their tags: - Which modes can traverse it -
 * Dangerousness on a bicycle in both directions (OSM ways can be bidirectional).
 *
 */
public class WayProperties {

  private final StreetTraversalPermission permission;

  @Nullable
  private final Double bicycleSafety;

  @Nullable
  private final Double walkSafety;

  WayProperties(WayPropertiesBuilder wayPropertiesBuilder) {
    permission = Objects.requireNonNull(wayPropertiesBuilder.getPermission());
    bicycleSafety = wayPropertiesBuilder.bicycleSafety();
    walkSafety = wayPropertiesBuilder.walkSafety();
  }

  /**
   * The value for the bicycle safety. If none has been set a default value of 1 is returned.
   */
  public double bicycleSafety() {
    return Objects.requireNonNullElse(bicycleSafety, 1.0);
  }

  /**
   * The value for the walk safety. If none has been set a default value of 1 is returned.
   */
  public double walkSafety() {
    return Objects.requireNonNullElse(walkSafety, 1.0);
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  /**
   * An optional value for the walk safety. If none has been set an empty Optional is returned.
   */
  protected OptionalDouble walkSafetyOpt() {
    return walkSafety == null ? OptionalDouble.empty() : OptionalDouble.of(walkSafety);
  }

  /**
   * An optional value for the bicycle safety. If none has been set an empty Optional is returned.
   */
  protected OptionalDouble bicycleSafetyOpt() {
    return bicycleSafety == null ? OptionalDouble.empty() : OptionalDouble.of(bicycleSafety);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bicycleSafety, walkSafety, permission);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof WayProperties other) {
      return (
        Objects.equals(bicycleSafety, other.bicycleSafety) &&
        Objects.equals(walkSafety, other.walkSafety) &&
        permission == other.permission
      );
    }
    return false;
  }

  public WayPropertiesBuilder mutate() {
    return new WayPropertiesBuilder(this);
  }
}
