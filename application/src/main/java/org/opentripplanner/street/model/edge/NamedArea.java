package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * A named area is a subset of an area with a certain set of properties (name, safety, etc).
 */

public class NamedArea implements Serializable {

  private Geometry geometry;
  private I18NString name;
  private double bicycleSafetyMultiplier;
  private double walkSafetyMultiplier;
  private StreetTraversalPermission permission;

  public I18NString getName() {
    return name;
  }

  public void setName(I18NString name) {
    this.name = name;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  public void setOriginalEdges(Geometry geometry) {
    this.geometry = geometry;
  }

  public double getBicycleSafetyMultiplier() {
    return bicycleSafetyMultiplier;
  }

  public void setBicycleSafetyMultiplier(double bicycleSafetyMultiplier) {
    this.bicycleSafetyMultiplier = bicycleSafetyMultiplier;
  }

  public double getWalkSafetyMultiplier() {
    return walkSafetyMultiplier;
  }

  public void setWalkSafetyMultiplier(double walkSafetyMultiplier) {
    this.walkSafetyMultiplier = walkSafetyMultiplier;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public void setPermission(StreetTraversalPermission permission) {
    this.permission = permission;
  }
}
