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
  MixinDirectionalProperties defaultProperties,
  MixinDirectionalProperties forwardProperties,
  MixinDirectionalProperties backwardProperties
) {
  MixinDirectionalProperties getDirectionalProperties(@Nullable TraverseDirection direction) {
    return direction == null
      ? defaultProperties
      : switch (direction) {
        case FORWARD -> forwardProperties;
        case BACKWARD -> backwardProperties;
      };
  }
}
