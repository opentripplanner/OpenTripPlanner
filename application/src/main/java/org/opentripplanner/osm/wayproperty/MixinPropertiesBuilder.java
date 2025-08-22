package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

/**
 * Builder for {@link MixinProperties}. If you don't set the safety features they will have a default
 * value of 1, which means no change.
 */
public class MixinPropertiesBuilder {

  final MixinDirectionalPropertiesBuilder defaultBuilder = new MixinDirectionalPropertiesBuilder();
  final MixinDirectionalPropertiesBuilder forwardBuilder = new MixinDirectionalPropertiesBuilder();
  final MixinDirectionalPropertiesBuilder backwardBuilder = new MixinDirectionalPropertiesBuilder();

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
    this.defaultBuilder.withBicycleSafety(value);
    this.forwardBuilder.withBicycleSafety(forward);
    this.backwardBuilder.withBicycleSafety(back);
    return this;
  }

  /**
   * Sets the same safety value for normal and back edge.
   * <p>
   * Note that the safeties here will be adjusted such that the safest street has a safety value of
   * 1, with all others scaled proportionately.
   */
  public MixinPropertiesBuilder walkSafety(double walkSafety) {
    this.defaultBuilder.withWalkSafety(walkSafety);
    this.forwardBuilder.withWalkSafety(walkSafety);
    this.backwardBuilder.withWalkSafety(walkSafety);
    return this;
  }

  public MixinProperties build(OsmSpecifier spec) {
    return new MixinProperties(
      spec,
      defaultBuilder.build(),
      forwardBuilder.build(),
      backwardBuilder.build()
    );
  }
}
