package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.common.model.P2;
import org.opentripplanner.graph_builder.module.osm.exception.OSMProcessingException;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

/**
 * Parameters applied to OSM ways, usually based on their tags: - Which modes can traverse it -
 * Dangerousness on a bicycle in both directions (OSM ways can be bidirectional).
 *
 * @author novalis
 */
public class WayProperties implements Cloneable {

  /**
   * A multiplicative parameter expressing how much less safe this way is than the default, in terms
   * of something like DALYs lost per meter. The first element safety in the direction of the way
   * and the second is safety in the opposite direction.
   * TODO change all these identifiers so it's clear that this only applies to bicycles.
   * TODO change the identifiers to make it clear that this reflects danger, not safety.
   * TODO I believe the weights are rescaled later in graph building to be >= 1, but verify.
   */
  private static final P2<Double> defaultSafetyFeatures = new P2<>(1.0, 1.0);
  private StreetTraversalPermission permission;
  private P2<Double> safetyFeatures = defaultSafetyFeatures;

  public P2<Double> getSafetyFeatures() {
    return safetyFeatures;
  }

  public void setSafetyFeatures(P2<Double> safetyFeatures) {
    this.safetyFeatures = safetyFeatures;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public void setPermission(StreetTraversalPermission permission) {
    this.permission = permission;
  }

  public int hashCode() {
    return safetyFeatures.hashCode() + permission.hashCode();
  }

  public boolean equals(Object o) {
    if (o instanceof WayProperties) {
      WayProperties other = (WayProperties) o;
      return safetyFeatures.equals(other.safetyFeatures) && permission == other.permission;
    }
    return false;
  }

  public WayProperties clone() {
    WayProperties result;
    try {
      result = (WayProperties) super.clone();
      result.setSafetyFeatures(new P2<>(safetyFeatures.first, safetyFeatures.second));
      return result;
    } catch (CloneNotSupportedException e) {
      // unreached
      throw new OSMProcessingException(e);
    }
  }
}
