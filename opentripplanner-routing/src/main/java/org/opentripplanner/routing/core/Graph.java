package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.routing.edgetype.DrawHandler;
import org.opentripplanner.routing.edgetype.Drawable;

public class Graph implements Serializable {
    private static final long serialVersionUID = -7583768730006630206L;

    HashMap<String, Vertex> vertices;

    public Graph() {
        this.vertices = new HashMap<String, Vertex>();
    }

    public Vertex addVertex(Vertex vv) {
        Vertex exists = this.vertices.get(vv.getLabel());
        if (exists != null) {
            return exists;
        }

        this.vertices.put(vv.getLabel(), vv);
        return vv;
    }

    public Vertex addVertex(String label, double x, double y) {
        Vertex exists = this.vertices.get(label);
        if (exists != null) {
            assert exists.getX() == x && exists.getY() == y;
            return exists;
        }

        Vertex ret = new GenericVertex(label, x, y);
        this.vertices.put(label, ret);
        return ret;
    }

    public Vertex getVertex(String label) {
        return this.vertices.get(label);
    }

    public Collection<Vertex> getVertices() {
        return this.vertices.values();
    }

    public void addEdge(Vertex a, Vertex b, Edge ee) {
        a.addOutgoing(ee);
        b.addIncoming(ee);
    }

    public void addEdge(Edge ee) {
        Vertex fromv = ee.getFromVertex();
        Vertex tov = ee.getToVertex();
        fromv.addOutgoing(ee);
        tov.addIncoming(ee);
    }
    
    public void addEdge(String from_label, String to_label, Edge ee) {
        Vertex v1 = this.getVertex(from_label);
        Vertex v2 = this.getVertex(to_label);
        
        addEdge(v1, v2, ee);
    }

    public Vertex nearestVertex(float lat, float lon) {
        double minDist = Float.MAX_VALUE;
        Vertex ret = null;
        for (Vertex vv : this.vertices.values()) {
            if (vv instanceof Locatable) {
                double dist = ((Locatable) vv).distance(lon, lat);
                if (dist < minDist) {
                    ret = vv;
                    minDist = dist;
                }
            }
        }
        return ret;
    }

    public void draw(DrawHandler drawer) throws Exception {
        for (Vertex vv : this.getVertices()) {
            for (Edge ee : vv.getOutgoing()) {
                if (ee instanceof Drawable) {
                    drawer.handle((Drawable) ee);
                }
            }
        }
    }

}