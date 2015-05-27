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
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.xml.bind.annotation.XmlTransient;

import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.common.geometry.DirectionUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import java.util.Locale;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.NonLocalizedString;

/**
 * A vertex in the graph. Each vertex has a longitude/latitude location, as well as a set of
 * incoming and outgoing edges.
 */
public abstract class Vertex implements Serializable, Cloneable {
    private static final long serialVersionUID = MavenVersion.VERSION.getUID();

    private static final Logger LOG = LoggerFactory.getLogger(Vertex.class);

    private static int maxIndex = 0;

    private int index;
    
    /* short debugging name */
    private final String label;
    
    /* Longer human-readable name for the client */
    private I18NString name;

    private final double x;

    private final double y;
    
    private transient Edge[] incoming = new Edge[0];

    private transient Edge[] outgoing = new Edge[0];

    
    /* CONSTRUCTORS */

    protected Vertex(Graph g, String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.index = maxIndex  ++;
        // null graph means temporary vertex
        if (g != null)
            g.addVertex(this);
        this.name = new NonLocalizedString("(no name provided)");
    }

    protected Vertex(Graph g, String label, double x, double y, I18NString name) {
        this(g, label, x, y);
        this.name = name;
    }

    /* PUBLIC METHODS */

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(this.getLabel());
        if (this.getCoordinate() != null) {
            sb.append(" lat,lng=").append(this.getCoordinate().y);
            sb.append(",").append(this.getCoordinate().x);
        }
        sb.append(">");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return index;
    }

    /* EDGE UTILITY METHODS (use arrays to eliminate copy-on-write set objects) */

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
     */
    private static Edge[] addEdge(Edge[] existing, Edge e) {
        Edge[] copy = new Edge[existing.length + 1];
        int i;
        for (i = 0; i < existing.length; i++) {
            if (existing[i] == e) {
                LOG.error("repeatedly added edge {}", e);
                return existing;
            }
            copy[i] = existing[i];
        }
        copy[i] = e; // append the new edge to the copy of the existing array
        return copy;
    }

    /**
     * A static helper method to avoid repeated code for outgoing and incoming lists.
     * Synchronization must be handled by the caller, to avoid passing edge array pointers that may be invalidated.
     */
    private static Edge[] removeEdge(Edge[] existing, Edge e) {
        int nfound = 0;
        for (int i = 0, j = 0; i < existing.length; i++) {
            if (existing[i] == e) nfound++;
        }
        if (nfound == 0) {
            LOG.error("Requested removal of an edge which isn't connected to this vertex.");
            return existing;
        }
        if (nfound > 1) {
            LOG.error("There are multiple copies of the edge to be removed.)");
        }
        Edge[] copy = new Edge[existing.length - nfound];
        for (int i = 0, j = 0; i < existing.length; i++) {
            if (existing[i] != e) copy[j++] = existing[i];
        }
        return copy;
    }

    /* FIELD ACCESSOR METHODS : READ/WRITE */

    public void addOutgoing(Edge edge) {
        synchronized (this) {
            outgoing = addEdge(outgoing, edge);
        }
    }

    /** @return whether the edge was found and removed. */
    public boolean removeOutgoing(Edge edge) {
        synchronized (this) {
            int n = outgoing.length;
            outgoing = removeEdge(outgoing, edge);
            return (outgoing.length < n);
        }
    }


    public void addIncoming(Edge edge) {
        synchronized (this) {
            incoming = addEdge(incoming, edge);
        }
    }

    /** @return whether the edge was found and removed. */
    public boolean removeIncoming(Edge edge) {
        synchronized (this) {
            int n = incoming.length;
            incoming = removeEdge(incoming, edge);
            return (incoming.length < n);
        }
    }

    /**
     * Get a collection containing all the edges leading from this vertex to other vertices.
     * There is probably some overhead to creating the wrapper ArrayList objects, but this
     * allows filtering and combining edge lists using stock Collection-based methods.
     */
    public Collection<Edge> getOutgoing() {
        return Arrays.asList(outgoing);
    }

    /** Get a collection containing all the edges leading from other vertices to this vertex. */
    public Collection<Edge> getIncoming() {
        return Arrays.asList(incoming);
    }

    @XmlTransient
    public int getDegreeOut() {
        return outgoing.length;
    }

    @XmlTransient
    public int getDegreeIn() {
        return incoming.length;
    }
    
    /** Get the longitude of the vertex */
    public double getX() {
        return x;
    }

    /** Get the latitude of the vertex */
    public double getY() {
        return y;
    }

    /** Get the longitude of the vertex */
    public double getLon() {
        return x;
    }

    /** Get the latitude of the vertex */
    public double getLat() {
        return y;
    }


    /** If this vertex is located on only one street, get that street's name
     * in english localization */
    public String getName() {
        return this.name.toString();
    }

    /** If this vertex is located on only one street, get that street's name
     * in provided localization
     * @param locale wanted localization */
    public String getName(Locale locale) {
        return this.name.toString(locale);
    }

    /* FIELD ACCESSOR METHODS : READ ONLY */

    /** Every vertex has a label which is globally unique. */
    public String getLabel() {
        return label;
    }

    @XmlTransient
    public Coordinate getCoordinate() {
        return new Coordinate(getX(), getY());
    }

    /** Get the bearing, in degrees, between this vertex and another coordinate. */
    public double azimuthTo(Coordinate other) {
        return DirectionUtils.getAzimuth(getCoordinate(), other);
    }

    /** Get the bearing, in degrees, between this vertex and another. */
    public double azimuthTo(Vertex other) {
        return azimuthTo(other.getCoordinate());
    }

    /** Get this vertex's unique index, that can serve as a hashcode or an index into a table */
    @XmlTransient
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public static int getMaxIndex() {
        return maxIndex;
    }


    /* SERIALIZATION METHODS */

    private void writeObject(ObjectOutputStream out) throws IOException {
        // edge lists are transient
        out.defaultWriteObject();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.incoming = new Edge[0];
        this.outgoing = new Edge[0];
        index = maxIndex++;
    }

    /* UTILITY METHODS FOR SEARCHING, GRAPH BUILDING, AND GENERATING WALKSTEPS */

    @XmlTransient
    public List<Edge> getOutgoingStreetEdges() {
        List<Edge> result = new ArrayList<Edge>();
        for (Edge out : this.getOutgoing()) {
            if (!(out instanceof StreetEdge)) {
                continue;
            }
            result.add((StreetEdge) out);
        }
        return result;
    }
}
