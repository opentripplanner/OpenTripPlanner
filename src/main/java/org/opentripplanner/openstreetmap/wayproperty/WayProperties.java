package org.opentripplanner.openstreetmap.wayproperty;

import java.util.Objects;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Parameters applied to OSM ways, usually based on their tags: - Which modes can traverse it -
 * Dangerousness on a bicycle in both directions (OSM ways can be bidirectional).
 *
 * @author novalis
 */
public class WayProperties {

  private final StreetTraversalPermission permission;
  private final SafetyFeatures bicycleSafetyFeatures;
  private final SafetyFeatures walkSafetyFeatures;

  public WayProperties(WayPropertiesBuilder wayPropertiesBuilder) {
    permission = wayPropertiesBuilder.getPermission();
    bicycleSafetyFeatures = wayPropertiesBuilder.getBicycleSafetyFeatures();
    walkSafetyFeatures = wayPropertiesBuilder.getWalkSafetyFeatures();
  }

  public SafetyFeatures getBicycleSafetyFeatures() {
    return bicycleSafetyFeatures;
  }

  public SafetyFeatures getWalkSafetyFeatures() {
    return walkSafetyFeatures;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public int hashCode() {
    return Objects.hash(bicycleSafetyFeatures, walkSafetyFeatures, permission);
  }

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
