package org.opentripplanner.osm;

import com.vividsolutions.jts.geom.Envelope;
import crosby.binary.Osmformat;

import java.util.List;

/**
 * Scans through the whole PBF file to find which nodes are inside a bounding geometry.
 * 
 * If we rewrite the parse functions to only decode coordinates, the run takes 22 sec. Using the
 * stock parse methods which create Node objects but overriding handleNode, run time only increases
 * to 25 sec. Must be some crafty JIT optimization.
 */
public class NodeGeomFilter extends Parser {

    public double minLat =  -90.0;
    public double minLon = -180.0;
    public double maxLat =   90.0;
    public double maxLon =  180.0;
    public final NodeTracker nodesInGeom = new NodeTracker();

    public NodeGeomFilter(Envelope env) {
        minLon = env.getMinX();
        maxLon = env.getMaxX();
        minLat = env.getMinY();
        maxLat = env.getMaxY();
    }

    public NodeGeomFilter(double minLat, double minLon, double maxLat, double maxLon) {
        this.minLat = minLat;
        this.minLon = minLon;
        this.maxLat = maxLat;
        this.maxLon = maxLon;
    }

    /** Disable parsing relations in this pass. */
    @Override 
    protected void parseRelations(List<Osmformat.Relation> rels) { }

    /** Disable parsing relations in this pass. */
    @Override 
    protected void parseWays(List<Osmformat.Way> ways) { }

    /** For each node or dense node, just check whether it is in the bounding geom. */
    @Override 
    public void handleNode(long id, Node node) {
        if (inGeom(node.getLat(), node.getLon())) {
            nodesInGeom.add(id);
        }
    };
    
    /* We are not using JTS and Geotools Geometry, it's too complicated for this simple task. */
    private boolean inGeom(double lat, double lon) {
        return lon > minLon && lon < maxLon && lat > minLat && lon < maxLat;
    }
    
}
