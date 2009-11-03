package org.opentripplanner.jags.core;
import java.io.Serializable;
import java.util.Vector;


public class Vertex extends AbstractVertex implements Serializable{

	private static final long serialVersionUID = 364261663335739528L;
	public Vector<Edge> outgoing;
    public Vector<Edge> incoming;
    public String label;
    public Class type;
    
    Vertex( String label ) {
        this.label = label;
        this.outgoing = new Vector<Edge>();
        this.incoming = new Vector<Edge>();
    }
    
    public int getDegreeOut() {
    	return this.outgoing.size();
    }
    
    public int getDegreeIn() {
    	return this.incoming.size();
    }
    
    public void addIncoming(Edge ee) {
        this.incoming.add( ee );
    }
    
    public void addOutgoing(Edge ee) {
        this.outgoing.add( ee );
    }
    
    public String toString() {
        return "<"+this.label+" "+this.outgoing.size()+" "+this.incoming.size()+">";
    }
}