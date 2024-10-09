package org.opentripplanner.openstreetmap.wayproperty;

import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;

/**
 * Mixins are like {@link WayProperties} but they only contain walk and bicycle safety features (not
 * modes).
 * <p>
 * They don't override other properties but their safety values are multiplied with the existing
 * values. As opposed to way properties, more than one mixins can apply to a single way.
 */
public record MixinProperties(
  OsmSpecifier specifier,
  SafetyFeatures walkSafety,
  SafetyFeatures bicycleSafety
) {}
