package org.opentripplanner.openstreetmap.wayproperty;

import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;

/**
 * Builder for {@link MixinProperties}. If you don't set the safety features they will have a default
 * value of 1, which means no change.
 */
public class MixinPropertiesBuilder {

  private SafetyFeatures bicycleSafety = SafetyFeatures.DEFAULT;
  private SafetyFeatures walkSafety = SafetyFeatures.DEFAULT;

  public static MixinPropertiesBuilder ofWalkSafety(double safety) {
    return new MixinPropertiesBuilder().walkSafety(safety);
  }

  public static MixinPropertiesBuilder ofBicycleSafety(double safety) {
    return new MixinPropertiesBuilder().bicycleSafety(safety, safety);
  }

  public static MixinPropertiesBuilder ofBicycleSafety(double forward, double back) {
    return new MixinPropertiesBuilder().bicycleSafety(forward, back);
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinPropertiesBuilder bicycleSafety(double forward, double back) {
    this.bicycleSafety = new SafetyFeatures(forward, back);
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinPropertiesBuilder walkSafety(double walkSafety) {
    this.walkSafety = new SafetyFeatures(walkSafety, walkSafety);
    return this;
  }

  public MixinProperties build(OsmSpecifier spec) {
    return new MixinProperties(spec, walkSafety, bicycleSafety);
  }
}
