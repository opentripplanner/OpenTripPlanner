package org.opentripplanner.osm;

import java.util.List;

import crosby.binary.Osmformat;

public class WayLoader extends Parser {

    public final OSM osm;
    NodeTracker nodesInGeom, nodesInWays;

    /**
     * Load all ways that include at least one node whose ID is marked in the specified NodeTracker.
     * Supply a NodeTracker.acceptEverything() to load all ways. TODO optimization: use null to mean "accept everything"
     */
    public WayLoader(OSM osm, NodeTracker nodesInGeom) {
        this.osm = osm;
        this.nodesInGeom = nodesInGeom;
        this.nodesInWays = new NodeTracker();
    }
    
    /** Disable parsing relations. */
    @Override 
    protected void parseRelations(List<Osmformat.Relation> rels) { }

    /** Disable parsing nodes. */
    @Override 
    protected void parseNodes(List<Osmformat.Node> nodes) { }

    /** Disable parsing dense nodes. */
    @Override
    protected void parseDense(Osmformat.DenseNodes nodes) { }

    /**
     * This method will be called with each new Way object as it is created. We keep all ways that
     * have desired tags and at least one node within the geographic area, marking all nodes used in
     * these ways.
     */
    @Override
    public void handleWay(long id, Way way) {
        /* Skip ways that have no retained tags. */
        if (way.hasNoTags()) return;
        /* Check if any node in this way is marked as being within the geometry. */
        boolean found = false;
        for (long node : way.nodes) {
            if (nodesInGeom.contains(node)) {
                found = true;
                break;
            }
        }
        /* Mark all nodes in the way as being used, then save the way. */
        if (found) {
            for (long node : way.nodes) {
                nodesInWays.add(node);
            }            
            osm.ways.put(id, way);
        }
    }
       
}
