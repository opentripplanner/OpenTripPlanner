package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Parameters applied to OSM ways, usually based on their tags: - Which modes can traverse it -
 * Dangerousness on a bicycle in both directions (OSM ways can be bidirectional).
 *
 * @author novalis
 */
public class WayProperties {

  /**
   * A multiplicative parameter expressing how much less safe this way is than the default, in terms
   * of something like DALYs lost per meter. The first element safety in the direction of the way
   * and the second is safety in the opposite direction.
   * TODO change the identifiers to make it clear that this reflects danger, not safety.
   */
  private static final P2<Double> defaultSafetyFeatures = new P2<>(1.0, 1.0);
  private final StreetTraversalPermission permission;
  private final P2<Double> bicycleSafetyFeatures;
  private final P2<Double> walkSafetyFeatures;

  public WayProperties(WayPropertiesBuilder wayPropertiesBuilder) {
    permission = wayPropertiesBuilder.getPermission();
    bicycleSafetyFeatures = wayPropertiesBuilder.getBicycleSafetyFeatures();
    walkSafetyFeatures = wayPropertiesBuilder.getWalkSafetyFeatures();
  }

  public P2<Double> getBicycleSafetyFeatures() {
    return bicycleSafetyFeatures;
  }

  public P2<Double> getWalkSafetyFeatures() {
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
