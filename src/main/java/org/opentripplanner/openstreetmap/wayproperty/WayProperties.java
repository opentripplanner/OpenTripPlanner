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
    bicycleSafetyFeatures = wayPropertiesBuilder.getBicycleSafetyFeatures();
    walkSafetyFeatures = wayPropertiesBuilder.getWalkSafetyFeatures();
  }

  @Nonnull
  public SafetyFeatures bicycleSafety() {
    return Objects.requireNonNullElse(bicycleSafetyFeatures, SafetyFeatures.DEFAULT);
  }

  @Nonnull
  public SafetyFeatures walkSafety() {
    return Objects.requireNonNullElse(walkSafetyFeatures, SafetyFeatures.DEFAULT);
  }

  @Nonnull
  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public WayPropertiesBuilder mutate() {
    return new WayPropertiesBuilder(this);
  }

  @Nonnull
  public Optional<SafetyFeatures> walkSafetyOpt() {
    return Optional.ofNullable(walkSafetyFeatures);
  }

  @Nonnull
  public Optional<SafetyFeatures> bicycleSafetyOpt() {
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
}
