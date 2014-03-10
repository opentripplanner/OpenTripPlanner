package org.opentripplanner.osm;

import java.util.List;

import crosby.binary.Osmformat;

public class NodeGeomFilter extends Parser {

    public double minLat =  -90.0;
    public double minLon = -180.0;
    public double maxLat =   90.0;
    public double maxLon =  180.0;
    public final NodeTracker nodes = new NodeTracker();

    public NodeGeomFilter(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }

    /** Disable parsing relations in this first pass. */
    @Override 
    protected void parseRelations(List<Osmformat.Relation> rels) { }

    /** Disable parsing relations in this first pass. */
    @Override 
    protected void parseWays(List<Osmformat.Way> ways) { }

    /** 
     * In the first pass, mark the nodes that are within the specified bounding geometry.
     * This method will be called from parseNodes on each OTP Node object. 
     */
    @Override
    public void handleNode(long id, Node node) {
        // Not using JTS and Geotools Geometry, it's way too complicated for what we're doing.
        if (node.lon > minLon && node.lon < maxLon && 
            node.lat > minLat && node.lon < maxLat) {
            nodes.add(id);     
        }
    }
    
}
