package org.opentripplanner.jags.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

import org.opentripplanner.jags.edgetype.Walkable;


public class Graph implements Serializable{
	private static final long serialVersionUID = -7583768730006630206L;
	HashMap<String,AbstractVertex> vertices;
    
    public Graph() {
        this.vertices = new HashMap<String,AbstractVertex>();
    }
    
    public Vertex addVertex( String label ) {
        AbstractVertex exists = (AbstractVertex)this.vertices.get( label );
        if( exists != null ) {
            return (Vertex)exists;
        }
        
        Vertex ret = new Vertex( label );
        this.vertices.put( label, ret );
        return ret;
    }
    
    public Vertex getVertex( String label ) {
        return (Vertex)this.vertices.get( label );
    }
    
    public Collection<AbstractVertex> getVertices() {
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
    
}