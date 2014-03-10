package org.opentripplanner.osm;


/**
 * Load an entire PBF file into an OSM object, with no filtering. If we want to filter by tags, we
 * still need to read all ways before reading all nodes, so we just use the spatially filtered
 * loader with a NodeTracker that accepts any node.
 * 
 * This should probably just be merged into the abstract superclass.
 */
public class FullParser extends Parser {

    public FullParser() {
        osm = new OSM(true);
    }

}
