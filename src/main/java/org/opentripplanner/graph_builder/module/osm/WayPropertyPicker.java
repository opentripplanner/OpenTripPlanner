package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.graph_builder.module.osm.specifier.OsmSpecifier;

/**
 * Associates an OSMSpecifier with some WayProperties. The WayProperties will be applied an OSM way
 * when the OSMSpecifier matches it better than any other OSMSpecifier in the same WayPropertySet.
 * WayPropertyPickers may be mixins, in which case they do not need to beat out all the other
 * WayPropertyPickers. Instead, their safety values will be applied to all ways that they match
 * multiplicatively.
 *
 * @param safetyMixin If this value is true, and this picker's specifier applies to a given way,
 *                    then this picker is never chosen as the most applicable value, and the final
 *                    safety will be multiplied by this value. More than one mixin may apply.
 */
public record WayPropertyPicker(
  OsmSpecifier specifier,
  WayProperties properties,
  boolean safetyMixin
) {}
