/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;

import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;


/**
 * Allows adding supplemental edges to existing vertices in another graph. Useful for CH, and
 * potentially for "extraEdges".
 * 
 * @author andrewbyrd
 */
public class OverlayGraph implements Serializable {

    private static final long serialVersionUID = 20111106L; // YYYYMMDD

    private static final int INITIAL_EDGELIST_CAPACITY = 5;

    private IdentityHashMap<Vertex, List<Edge>> outgoing;

    private IdentityHashMap<Vertex, List<Edge>> incoming;

    /**
     * Create an empty OverlayGraph.
     */
    public OverlayGraph() {
        outgoing = new IdentityHashMap<Vertex, List<Edge>>();
        incoming = new IdentityHashMap<Vertex, List<Edge>>();
    }

    /**
     * Copy contents of a Graph into this OverlayGraph
     */
    public OverlayGraph(Graph g) {
        this();
        for (Vertex v : g.getVertices()) {
            for (Edge e : v.getOutgoing())
                this.addOutgoing(v, e);
            for (Edge e : v.getIncoming())
                this.addIncoming(v, e);
        }
    }

    public void addOutgoing(Vertex fromv, Edge e) {
        List<Edge> fromOutgoing = outgoing.get(fromv);
        if (fromOutgoing == null) {
            fromOutgoing = new ArrayList<Edge>(INITIAL_EDGELIST_CAPACITY);
            outgoing.put(fromv, fromOutgoing);
        }
        if (!fromOutgoing.contains(e))
            fromOutgoing.add(e);
    }

    public void addIncoming(Vertex tov, Edge e) {
        List<Edge> toIncoming = incoming.get(tov);
        if (toIncoming == null) {
            toIncoming = new ArrayList<Edge>(INITIAL_EDGELIST_CAPACITY);
            incoming.put(tov, toIncoming);
        }
        if (!toIncoming.contains(e))
            toIncoming.add(e);
    }

    public void removeOutgoing(Vertex fromv, Edge e) {
        List<Edge> fromOutgoing = outgoing.get(fromv);
        if (fromOutgoing != null) {
            fromOutgoing.remove(e);
        }
    }

    public void removeIncoming(Vertex tov, Edge e) {
        List<Edge> toIncoming = incoming.get(tov);
        if (toIncoming != null) {
            toIncoming.remove(e);
        }
    }

    public void addEdge(Edge e) {
        Vertex fromv = e.getFromVertex();
        Vertex tov = e.getToVertex();
        addOutgoing(fromv, e);
        addIncoming(tov, e);
    }

    public void removeEdge(Edge e) {
        Vertex fromv = e.getFromVertex();
        Vertex tov = e.getToVertex();
        removeOutgoing(fromv, e);
        removeIncoming(tov, e);
    }

    public List<Edge> getOutgoing(Vertex v) {
        List<Edge> ret = outgoing.get(v);
        if (ret == null)
            ret = Collections.emptyList();
        return ret;
    }

    public List<Edge> getIncoming(Vertex v) {
        List<Edge> ret = incoming.get(v);
        if (ret == null)
            ret = Collections.emptyList();
        return ret;
    }

    /**
     * A single edge can appear once or twice. (CH graphs might have only outgoing or only incoming
     * edges.) Avoid double-counting.
     */
    public int countEdges() {
        HashSet<Edge> eset = new HashSet<Edge>(1000);
        for (List<Edge> l : outgoing.values())
            for (Edge e : l)
                eset.add(e);

        for (List<Edge> l : incoming.values())
            for (Edge e : l)
                eset.add(e);

        return eset.size();
    }

    /**
     * A single Vertex can appear once or twice. (CH graphs might have only outgoing or only
     * incoming edges.) Avoid double-counting.
     */
    public Collection<Vertex> getVertices() {
        HashSet<Vertex> sv = new HashSet<Vertex>();
        sv.addAll(outgoing.keySet());
        sv.addAll(incoming.keySet());
        return sv;
    }

    /**
     * A single Vertex can appear once or twice. (CH graphs might have only outgoing or only
     * incoming edges.) Avoid double-counting. This is very inefficient.
     */
    public int countVertices() {
        return getVertices().size();
    }

    // need to make sure lists are never null - Vertex
    // beware concurrentModification of lists.
    // public void removeVertex(Vertex vertex) {
    // List<Edge> toRemove = outgoing.remove(vertex);
    // toRemove.addAll(incoming.remove(vertex));
    // for (Edge e : toRemove)
    // if (e instanceof Edge)
    // removeEdge((Edge)e);
    // }

    public void removeVertex(Vertex vertex) {
        outgoing.remove(vertex);
        incoming.remove(vertex);
    }

    public int getDegreeIn(Vertex v) {
        List<Edge> l = incoming.get(v);
        if (l == null)
            return 0;
        else
            return l.size();
    }

    public int getDegreeOut(Vertex v) {
        List<Edge> l = outgoing.get(v);
        if (l == null)
            return 0;
        else
            return l.size();
    }

    public boolean containsVertex(Vertex vertex) {
        return outgoing.containsKey(vertex) || incoming.containsKey(vertex);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        for (List<Edge> le : outgoing.values())
            ((ArrayList<Edge>)le).trimToSize();
        for (List<Edge> le : incoming.values())
            ((ArrayList<Edge>)le).trimToSize();
        out.defaultWriteObject();
    }

}
