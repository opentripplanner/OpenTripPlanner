package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

public record SlopeOverridePicker(OsmSpecifier specifier, boolean override) {}
