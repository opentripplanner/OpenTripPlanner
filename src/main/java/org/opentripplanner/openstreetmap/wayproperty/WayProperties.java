package org.opentripplanner.openstreetmap.wayproperty;

import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Parameters applied to OSM ways, usually based on their tags: - Which modes can traverse it -
 * Dangerousness on a bicycle in both directions (OSM ways can be bidirectional).
 *
 */
public class WayProperties {

  @Nonnull
  private final StreetTraversalPermission permission;

  @Nullable
  private final SafetyFeatures bicycleSafetyFeatures;

  @Nullable
  private final SafetyFeatures walkSafetyFeatures;

  WayProperties(WayPropertiesBuilder wayPropertiesBuilder) {
    permission = Objects.requireNonNull(wayPropertiesBuilder.getPermission());
    bicycleSafetyFeatures = wayPropertiesBuilder.bicycleSafety();
    walkSafetyFeatures = wayPropertiesBuilder.walkSafety();
  }

  /**
   * The value for the bicycle safety. If none has been set a default value of 1 is returned.
   */
  @Nonnull
  public SafetyFeatures bicycleSafety() {
    return Objects.requireNonNullElse(bicycleSafetyFeatures, SafetyFeatures.DEFAULT);
  }

  /**
   * The value for the walk safety. If none has been set a default value of 1 is returned.
   */
  @Nonnull
  public SafetyFeatures walkSafety() {
    return Objects.requireNonNullElse(walkSafetyFeatures, SafetyFeatures.DEFAULT);
  }

  @Nonnull
  public StreetTraversalPermission getPermission() {
    return permission;
  }

  /**
   * An optional value for the walk safety. If none has been set an empty Optional is returned.
   */
  @Nonnull
  protected Optional<SafetyFeatures> walkSafetyOpt() {
    return Optional.ofNullable(walkSafetyFeatures);
  }

  /**
   * An optional value for the bicycle safety. If none has been set an empty Optional is returned.
   */
  @Nonnull
  protected Optional<SafetyFeatures> bicycleSafetyOpt() {
    return Optional.ofNullable(bicycleSafetyFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(bicycleSafetyFeatures, walkSafetyFeatures, permission);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof WayProperties other) {
      return (
        Objects.equals(bicycleSafetyFeatures, other.bicycleSafetyFeatures) &&
        Objects.equals(walkSafetyFeatures, other.walkSafetyFeatures) &&
        permission == other.permission
      );
    }
    return false;
  }

  public WayPropertiesBuilder mutate() {
    return new WayPropertiesBuilder(this);
  }
}
