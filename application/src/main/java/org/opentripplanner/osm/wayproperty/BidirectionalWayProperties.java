package org.opentripplanner.osm.wayproperty;

/// The [WayProperties] (traversal permissions and bicycle/walk safety) of an OSM way in both
/// directions, since OSM ways can have different permissions and safety values depending on the
/// direction of travel.
public record BidirectionalWayProperties(WayProperties forward, WayProperties backward) {}
