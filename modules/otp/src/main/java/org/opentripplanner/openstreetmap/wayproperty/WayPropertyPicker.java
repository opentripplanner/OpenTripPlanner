package org.opentripplanner.openstreetmap.wayproperty;

import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;

/**
 * Associates an OSMSpecifier with some WayProperties. The WayProperties will be applied an OSM way
 * when the OSMSpecifier matches it better than any other OSMSpecifier in the same WayPropertySet.
 */
public record WayPropertyPicker(OsmSpecifier specifier, WayProperties properties) {}
