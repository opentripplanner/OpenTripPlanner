package org.opentripplanner.osm;

import java.util.List;

import crosby.binary.Osmformat;

/** 
 * Load all nodes whose IDs are marked in the specified NodeTracker. 
 * Supply a NodeTracker.acceptEverything() to load all nodes.
 */
public class NodeLoader extends Parser {

    public final OSM osm;
    NodeTracker nodes;
    
    public NodeLoader(OSM osm, NodeTracker nodes) {
        this.osm = osm;
        this.nodes = nodes;
    }
    
    /** Disable parsing ways in this pass. */
    @Override 
    protected void parseWays(List<Osmformat.Way> ways) { }
    
    /** Disable parsing relations in this pass. */
    @Override 
    protected void parseRelations(List<Osmformat.Relation> rels) { }

    /** Only save nodes whose ids are marked in the supplied NodeTracker. */
    public void handleNode(long id, Node node) {
        if (nodes.contains(id)) {
            osm.nodes.put(id,  node);
        }
    };

}
