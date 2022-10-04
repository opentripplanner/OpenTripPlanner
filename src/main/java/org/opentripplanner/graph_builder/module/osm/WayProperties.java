package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

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
    return bicycleSafetyFeatures.hashCode() + walkSafetyFeatures.hashCode() + permission.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof WayProperties) {
      WayProperties other = (WayProperties) o;
      return (
        bicycleSafetyFeatures.equals(other.bicycleSafetyFeatures) &&
        walkSafetyFeatures.equals(other.walkSafetyFeatures) &&
        permission == other.permission
      );
    }
    return false;
  }

  public WayPropertiesBuilder mutate() {
    return new WayPropertiesBuilder(this);
  }
}
