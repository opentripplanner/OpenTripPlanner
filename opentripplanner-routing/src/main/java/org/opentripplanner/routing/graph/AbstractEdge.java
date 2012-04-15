/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.graph;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlTransient;

import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.patch.Alert;
import org.opentripplanner.routing.patch.Patch;

/**
 * This is the standard implementation of an edge with fixed from and to Vertex instances;
 * all standard OTP edges are subclasses of this.  
 */
public abstract class AbstractEdge implements Edge {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    protected Vertex fromv;

    protected Vertex tov;

    private List<Patch> patches;

    protected AbstractEdge(Vertex v1, Vertex v2) {
    	if (v1 == null || v2 == null)
    		throw new IllegalStateException(this.getClass() + " constructed with null vertex : " + 
    				  v1 + " " + v2);
        this.fromv = v1;
        this.tov = v2;
//        if (! vertexTypesValid()) {
//            throw new IllegalStateException(this.getClass() + " constructed with bad vertex types");
//        }

        fromv.addOutgoing(this);
        tov.addIncoming(this);
    }

    @Override
    public Vertex getFromVertex() {
        return fromv;
    }

    @Override
    public Vertex getToVertex() {
        return tov;
    }

    public void attachFrom(Vertex fromv) {
        detachFrom();
        if (fromv == null)
            throw new IllegalStateException("attaching to fromv null");
        this.fromv = fromv;
        fromv.addOutgoing(this);
    }

    public void attachTo(Vertex tov) {
        detachTo();
        if (tov == null)
            throw new IllegalStateException("attaching to tov null");
        this.tov = tov;
        tov.addIncoming(this);
    }

    /** Attach this edge to new endpoint vertices, keeping edgelists coherent */
    @Override
    public void attach(Vertex fromv, Vertex tov) {
    	attachFrom(fromv);
    	attachTo(tov);
    }
    
    private boolean detachFrom() {
        boolean detached = false;
        if (fromv != null) {
            detached = fromv.removeOutgoing(this);
            fromv = null;
        }
        return detached;
    }

    private boolean detachTo() {
        boolean detached = false;
        if (tov != null) {
            detached = tov.removeIncoming(this);
            tov = null;
        }
        return detached;
    }

    /** Disconnect this edge from its endpoint vertices, keeping edgelists coherent */
    @Override
    public int detach() {
        int nDetached = 0;
    	if (detachFrom())
    	    nDetached += 1;
    	if (detachTo())
    	    nDetached += 1;
    	return nDetached;
    }
    
    public Trip getTrip() {
        return null;
    }

    public Set<Alert> getNotes() {
    	return null;
    }
    
    @Override
    public int hashCode() {
        return fromv.hashCode() * 31 + tov.hashCode();
    }

    @Override
    public boolean isRoundabout() {
        return false;
    }
    
    @Override
    public State optimisticTraverse (State s0) {
    	return this.traverse(s0);
    }
    
    public double weightLowerBound(TraverseOptions options) {
        // Edge weights are non-negative. Zero is an admissible default lower bound.
    	return 0;
    }
        
    public double timeLowerBound(TraverseOptions options) {
        // No edge should take less than zero time to traverse.
        return 0;
    }

    @Override
    public void addPatch(Patch patch) {
        if (patches == null) {
            patches = new ArrayList<Patch>();
        }
        if (!patches.contains(patch)) {
            patches.add(patch);
        }
    }

    public List<Patch> getPatches() {
        if (patches == null) {
            return Collections.emptyList();
        }
        return patches;
    }

    @Override
    public void removePatch(Patch patch) {
        if (patches == null || patches.size() == 1) {
            patches = null;
        } else {
            patches.remove(patch);
        }
    }

    public boolean hasBogusName() {
        return false;
    }

    public String toString() {
        return getClass().getName() + "(" + fromv + " -> " + tov + ")";
    }

    
    /* SERIALIZATION */
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // edge lists are transient, reconstruct them
        fromv.addOutgoing(this);
        tov.addIncoming(this);
    }
    
    private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException {
        if (fromv == null)
            System.out.printf("fromv null %s \n", this);
        if (tov == null)
            System.out.printf("tov null %s \n", this);
        out.defaultWriteObject();
    }

    
    /* GRAPH COHERENCY AND TYPE CHECKING */
    
    @SuppressWarnings("unchecked")
    private static final ValidVertexTypes VALID_VERTEX_TYPES =
            new ValidVertexTypes(Vertex.class, Vertex.class);
    
    @XmlTransient
    @Override
    public ValidVertexTypes getValidVertexTypes() {
        return VALID_VERTEX_TYPES;
    }

    /*
     * This may not be necessary if edge constructor types are strictly specified
     */
    @Override
    public final boolean vertexTypesValid() {
        return getValidVertexTypes().isValid(fromv, tov);
    }
    
    public static final class ValidVertexTypes {
        private final Class<? extends Vertex>[] classes;
        // varargs constructor: 
        // a loophole in the law against arrays/collections of parameterized generics
        public ValidVertexTypes (Class<? extends Vertex>... classes) {
            if (classes.length % 2 != 0) {
                throw new IllegalStateException("from/to/from/to...");
            } else {
                this.classes = classes;
            }
        }
        public boolean isValid (Vertex from, Vertex to) {
            for (int i=0; i<classes.length; i+=2) {
                if (classes[i].isInstance(from) && classes[i+1].isInstance(to))
                    return true;
            }
            return false;
        }
    }

}
