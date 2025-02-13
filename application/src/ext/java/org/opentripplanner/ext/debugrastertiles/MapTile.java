package org.opentripplanner.ext.debugrastertiles;

import org.locationtech.jts.geom.Envelope;

/**
 * Container for a map tile bounds and size
 */
public record MapTile(Envelope bbox, int width, int height) {}
