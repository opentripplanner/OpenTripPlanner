package org.opentripplanner.inspector.raster;

import org.locationtech.jts.geom.Envelope;

/**
 * Container for a map tile bounds and size
 */
public record MapTile(Envelope bbox, int width, int height) {}
