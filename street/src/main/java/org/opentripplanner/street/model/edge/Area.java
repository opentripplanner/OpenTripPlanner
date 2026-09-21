package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.StreetTraversalPermission;

/**
 * Area is a subset of an area group with a certain set of properties (name, safety, etc).
 */
public final class Area implements Serializable {

  private final Geometry geometry;
  private final I18NString name;
  private final float bicycleSafety;
  private final float walkSafety;
  private final StreetTraversalPermission permission;
  private final boolean wheelchairAccessible;

  private Area(Builder builder) {
    this.geometry = builder.geometry;
    this.name = builder.name;
    this.bicycleSafety = builder.bicycleSafety;
    this.walkSafety = builder.walkSafety;
    this.permission = builder.permission;
    this.wheelchairAccessible = builder.wheelchairAccessible;
  }

  public static Builder of() {
    return new Builder();
  }

  public I18NString getName() {
    return name;
  }

  public Geometry getGeometry() {
    return geometry;
  }

  public float getBicycleSafety() {
    return bicycleSafety;
  }

  public float getWalkSafety() {
    return walkSafety;
  }

  public StreetTraversalPermission getPermission() {
    return permission;
  }

  public boolean isWheelchairAccessible() {
    return wheelchairAccessible;
  }

  /**
   * We use this class as a map key, but it has no clear equality operation so we delegate to
   * object identity instead.
   */
  @Override
  public final boolean equals(Object obj) {
    return super.equals(obj);
  }

  /**
   * We use this class as a map key, but it has no clear hashcode so we delegate to
   * object identity instead.
   */
  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  public static class Builder {

    private Geometry geometry;
    private I18NString name;
    private float bicycleSafety;
    private float walkSafety;
    private StreetTraversalPermission permission;
    private boolean wheelchairAccessible = true;

    private Builder() {}

    public Builder withGeometry(Geometry geometry) {
      this.geometry = geometry;
      return this;
    }

    public Builder withName(I18NString name) {
      this.name = name;
      return this;
    }

    public Builder withBicycleSafety(float bicycleSafety) {
      this.bicycleSafety = bicycleSafety;
      return this;
    }

    public Builder withWalkSafety(float walkSafety) {
      this.walkSafety = walkSafety;
      return this;
    }

    public Builder withPermission(StreetTraversalPermission permission) {
      this.permission = permission;
      return this;
    }

    public Builder withWheelchairAccessible(boolean wheelchairAccessible) {
      this.wheelchairAccessible = wheelchairAccessible;
      return this;
    }

    public Area build() {
      return new Area(this);
    }
  }
}
