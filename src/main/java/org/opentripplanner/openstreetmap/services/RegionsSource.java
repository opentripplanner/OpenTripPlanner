package org.opentripplanner.openstreetmap.services;

import org.locationtech.jts.geom.Envelope;

/**
 * A RegionSource represents a set of rectangular regions. It's used by
 * OpenStreetMapGraphBuilderImpl to choose an area of the map to download
 * and build.
 * 
 * @author novalis
 * 
 */
public interface RegionsSource {
    public Iterable<Envelope> getRegions();
}
