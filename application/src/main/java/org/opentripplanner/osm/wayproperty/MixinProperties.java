package org.opentripplanner.osm.wayproperty;

import javax.annotation.Nullable;
import org.opentripplanner.osm.TraverseDirection;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

/**
 * Mixins are like {@link WayProperties} but they only contain walk and bicycle safety features (not
 * modes).
 * <p>
 * They don't override other properties but their safety values are multiplied with the existing
 * values. As opposed to way properties, more than one mixins can apply to a single way.
 */
public record MixinProperties(
  OsmSpecifier specifier,
  double walkSafety,
  double bicycleSafety,
  double forwardWalkSafety,
  double forwardBicycleSafety,
  double backwardWalkSafety,
  double backwardBicycleSafety
) {
  double getWalkSafety(@Nullable TraverseDirection direction) {
    return direction == null
      ? walkSafety
      : switch (direction) {
        case FORWARD -> forwardWalkSafety;
        case BACKWARD -> backwardWalkSafety;
      };
  }

  double getBicycleSafety(@Nullable TraverseDirection direction) {
    return direction == null
      ? bicycleSafety
      : switch (direction) {
        case FORWARD -> forwardBicycleSafety;
        case BACKWARD -> backwardBicycleSafety;
      };
  }
}
