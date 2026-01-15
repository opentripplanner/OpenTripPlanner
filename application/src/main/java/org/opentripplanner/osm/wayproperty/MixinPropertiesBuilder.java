package org.opentripplanner.osm.wayproperty;

import java.util.function.Consumer;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;
import org.opentripplanner.street.model.StreetTraversalPermission;

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

  /**
   * Add the same permission to all directions
   */
  public MixinPropertiesBuilder addPermission(StreetTraversalPermission permission) {
    this.defaultBuilder.addPermission(permission);
    this.forwardBuilder.addPermission(permission);
    this.backwardBuilder.addPermission(permission);
    return this;
  }

  /**
   * Remove the same permission to all directions
   */
  public MixinPropertiesBuilder removePermission(StreetTraversalPermission permission) {
    this.defaultBuilder.removePermission(permission);
    this.forwardBuilder.removePermission(permission);
    this.backwardBuilder.removePermission(permission);
    return this;
  }

  public MixinPropertiesBuilder directional(
    TraverseDirection direction,
    Consumer<MixinDirectionalPropertiesBuilder> action
  ) {
    var builder =
      switch (direction) {
        case DIRECTIONLESS -> defaultBuilder;
        case FORWARD -> forwardBuilder;
        case BACKWARD -> backwardBuilder;
      };
    action.accept(builder);
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
