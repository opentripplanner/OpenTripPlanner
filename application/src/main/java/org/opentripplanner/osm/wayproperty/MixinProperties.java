package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.model.TraverseDirection;
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
  MixinDirectionalProperties directionlessProperties,
  MixinDirectionalProperties forwardProperties,
  MixinDirectionalProperties backwardProperties
) {
  MixinDirectionalProperties getDirectionalProperties(TraverseDirection direction) {
    return switch (direction) {
      case DIRECTIONLESS -> directionlessProperties;
      case FORWARD -> forwardProperties;
      case BACKWARD -> backwardProperties;
    };
  }
}
