package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

/**
 * Builder for {@link MixinProperties}. If you don't set the safety features they will have a default
 * value of 1, which means no change.
 */
public class MixinPropertiesBuilder {

  private double walkSafety = 1;
  private double bicycleSafety = 1;
  private double forwardBicycleSafety = 1;
  private double backwardBicycleSafety = 1;
  private double forwardWalkSafety = 1;
  private double backwardWalkSafety = 1;

  public static MixinPropertiesBuilder ofWalkSafety(double safety) {
    return new MixinPropertiesBuilder().walkSafety(safety);
  }

  public static MixinPropertiesBuilder ofBicycleSafety(double safety) {
    return new MixinPropertiesBuilder().bicycleSafety(safety, safety, safety);
  }

  public static MixinPropertiesBuilder ofBicycleSafety(double value, double forward, double back) {
    return new MixinPropertiesBuilder().bicycleSafety(value, forward, back);
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinPropertiesBuilder bicycleSafety(double value, double forward, double back) {
    this.bicycleSafety = value;
    this.forwardBicycleSafety = forward;
    this.backwardBicycleSafety = back;
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinPropertiesBuilder walkSafety(double walkSafety) {
    this.walkSafety = walkSafety;
    this.forwardWalkSafety = walkSafety;
    this.backwardWalkSafety = walkSafety;
    return this;
  }

  public MixinProperties build(OsmSpecifier spec) {
    return new MixinProperties(
      spec,
      walkSafety,
      bicycleSafety,
      forwardWalkSafety,
      forwardBicycleSafety,
      backwardWalkSafety,
      backwardBicycleSafety
    );
  }
}
