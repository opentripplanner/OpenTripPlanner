package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

/**
 * Choose a speed that should be applied to a given segment
 */
public record SpeedPicker(OsmSpecifier specifier, float speed) {}
