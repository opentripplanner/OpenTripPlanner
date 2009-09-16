package org.opentripplanner.jags.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Drawable;
import org.opentripplanner.jags.edgetype.Walkable;


public class Graph implements Serializable{
	private static final long serialVersionUID = -7583768730006630206L;
	HashMap<String,Vertex> vertices;
    
    public Graph() {
        this.vertices = new HashMap<String,Vertex>();
    }
    
    public Vertex addVertex( Vertex vv ) {
        Vertex exists = this.vertices.get( vv.label );
        if( exists != null ) {
            return exists;
        }
        
        this.vertices.put( vv.label, vv );
        return vv;
    }
    
    public Vertex addVertex( String label ) {
        Vertex exists = this.vertices.get( label );
        if( exists != null ) {
            return exists;
        }
        
        Vertex ret = new Vertex( label );
        this.vertices.put( label, ret );
        return ret;
    }
    
    public Vertex getVertex( String label ) {
        return (Vertex)this.vertices.get( label );
    }
    
    public Collection<Vertex> getVertices() {
    	return this.vertices.values();
    }
    
    public Edge addEdge( Vertex a, Vertex b, Walkable ep ) {
    	Edge ee = new Edge( a, b, ep );
    	a.addOutgoing( ee );
    	b.addIncoming( ee );
    	return ee;
    }
    
    public Edge addEdge( String from_label, String to_label, Walkable ep ) {
        Vertex v1 = this.getVertex( from_label );
        Vertex v2 = this.getVertex( to_label );
        
        return addEdge( v1, v2, ep );
    }

	public Vertex nearestVertex(float lat, float lon) {
		double minDist = Float.MAX_VALUE;
		Vertex ret = null;
		for(Vertex vv : this.vertices.values()) {
			if(vv instanceof Locatable) {
				double dist = ((Locatable)vv).distance(lon, lat);
				if(dist < minDist) {
					ret = vv;
					minDist = dist;
				}
			}
		}
		return ret;
	}

	public void draw(DrawHandler drawer) throws Exception {
		for(Vertex vv : this.getVertices() ) {
			for(Edge ee : vv.outgoing ) {
				if(ee.payload instanceof Drawable) {
					drawer.handle((Drawable)ee.payload);
				}
			}
		}
	}
    
}