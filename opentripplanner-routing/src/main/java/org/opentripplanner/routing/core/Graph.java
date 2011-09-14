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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.Date;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * This holds the edge list for every vertex.
 */
public class Graph implements Serializable {
    // update serialVersionId to the current date in format YYYYMMDDL
    // whenever changes are made that could make existing graphs incompatible
    private static final long serialVersionUID = 20110614L;

    // transit feed validity information in seconds since epoch
    private long transitServiceStarts = Long.MAX_VALUE;

    private long transitServiceEnds = 0;

    private Map<Class<?>, Object> _services = new HashMap<Class<?>, Object>();

    HashMap<String, GraphVertex> vertices;

    private TransferTable transferTable = new TransferTable();

    public Graph() {
        this.vertices = new HashMap<String, GraphVertex>();
    }

    public Vertex addVertex(Vertex vv) {
        String label = vv.getLabel();
        GraphVertex gv = vertices.get(label);
        if (gv == null) {
            gv = new GraphVertex(vv);
            vertices.put(label, gv);
        }
        return gv.vertex;
    }

    public Vertex addVertex(String label, double x, double y) {
        GraphVertex gv = vertices.get(label);
        if (gv == null) {
            Vertex vv = new GenericVertex(label, x, y);
            gv = new GraphVertex(vv);
            vertices.put(label, gv);
        }
        return gv.vertex;
    }

    public Vertex addVertex(String label, String name, AgencyAndId stopId, double x, double y) {
        GraphVertex gv = vertices.get(label);
        if (gv == null) {
            Vertex vv = new GenericVertex(label, x, y, name, stopId);
            gv = new GraphVertex(vv);
            vertices.put(label, gv);
        }
        return gv.vertex;
    }

    public Vertex getVertex(String label) {
        GraphVertex gv = vertices.get(label);
        if (gv == null) {
            return null;
        }
        return gv.vertex;
    }

    public GraphVertex getGraphVertex(String label) {
        return vertices.get(label);
    }

    public Collection<GraphVertex> getVertices() {
        return vertices.values();
    }

    public void addEdge(Vertex a, Vertex b, Edge ee) {
        a = addVertex(a);
        b = addVertex(b);
        vertices.get(a.getLabel()).addOutgoing(ee);
        vertices.get(b.getLabel()).addIncoming(ee);
    }

    public void addEdge(DirectEdge ee) {
        Vertex fromv = ee.getFromVertex();
        Vertex tov = ee.getToVertex();
        fromv = addVertex(fromv);
        tov = addVertex(tov);
        vertices.get(fromv.getLabel()).addOutgoing(ee);
        vertices.get(tov.getLabel()).addIncoming(ee);
    }

    public void addEdge(String from_label, String to_label, Edge ee) {
        Vertex v1 = this.getVertex(from_label);
        Vertex v2 = this.getVertex(to_label);

        addEdge(v1, v2, ee);
    }

    public Vertex nearestVertex(float lat, float lon) {
        double minDist = Float.MAX_VALUE;
        Vertex ret = null;
        Coordinate c = new Coordinate(lon, lat);
        for (GraphVertex vv : vertices.values()) {
            double dist = vv.vertex.distance(c);
            if (dist < minDist) {
                ret = vv.vertex;
                minDist = dist;
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    public <T> T putService(Class<T> serviceType, T service) {
        return (T) _services.put(serviceType, service);
    }

    public boolean hasService(Class<?> serviceType) {
        return _services.containsKey(serviceType);
    }

    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceType) {
        return (T) _services.get(serviceType);
    }

    public void removeVertex(Vertex vertex) {
        vertices.remove(vertex.getLabel());
    }

    public void removeVertexAndEdges(Vertex vertex) {
        GraphVertex gv = getGraphVertex(vertex);
        if (gv == null) {
            return;
        }
        vertices.remove(vertex.getLabel());
        for (Edge e : gv.getOutgoing()) {
            if (e instanceof DirectEdge) {
                DirectEdge edge = (DirectEdge) e;
                GraphVertex target = vertices.get(edge.getToVertex().getLabel());
                if (target != null) {
                    target.removeIncoming(e);
                }
            }
        }
        for (Edge e : gv.getIncoming()) {
            if (e instanceof DirectEdge) {
                DirectEdge edge = (DirectEdge) e;
                GraphVertex source = vertices.get(edge.getFromVertex().getLabel());
                if (source != null) {
                    source.removeIncoming(e);
                }
            }
        }
    }

    public Envelope getExtent() {
        Envelope env = new Envelope();
        for (GraphVertex v : this.getVertices()) {
            env.expandToInclude(v.vertex.getCoordinate());
        }
        return env;
    }

    public Collection<Edge> getOutgoing(Vertex v) {
        return vertices.get(v.getLabel()).outgoing;
    }

    public Collection<Edge> getIncoming(Vertex v) {
        return vertices.get(v.getLabel()).incoming;
    }

    public int getDegreeOut(Vertex v) {
        return vertices.get(v.getLabel()).outgoing.size();
    }

    public int getDegreeIn(Vertex v) {
        return vertices.get(v.getLabel()).incoming.size();
    }

    public Collection<Edge> getIncoming(String label) {
        return vertices.get(label).incoming;
    }

    public Collection<Edge> getOutgoing(String label) {
        return vertices.get(label).outgoing;
    }

    public GraphVertex getGraphVertex(Vertex vertex) {
        return getGraphVertex(vertex.getLabel());
    }

    public void addGraphVertex(GraphVertex graphVertex) {
        vertices.put(graphVertex.vertex.getLabel(), graphVertex);
    }

    public TransferTable getTransferTable() {
        return transferTable;
    }

    // Infer the time period covered by the trasit feed
    public void updateTransitFeedValidity(CalendarServiceData data) {
        final long SEC_IN_DAY = 24 * 60 * 60;
        for (AgencyAndId sid : data.getServiceIds()) {
            for (ServiceDate sd : data.getServiceDatesForServiceId(sid)) {
                long t = sd.getAsDate().getTime();
                // assume feed is unreliable after midnight on last service day
                long u = t + SEC_IN_DAY;
                if (t < this.transitServiceStarts)
                    this.transitServiceStarts = t;
                if (u > this.transitServiceEnds)
                    this.transitServiceEnds = u;
            }
        }
    }

    // Check to see if we have transit information for a given date
    public boolean transitFeedCovers(Date d) {
        long t = d.getTime();
        return t >= this.transitServiceStarts && t < this.transitServiceEnds;
    }

    public void removeEdge(DirectEdge e) {
        GraphVertex gv = vertices.get(e.getFromVertex().getLabel());
        if (gv != null) {
            gv.removeOutgoing(e);
        }
        gv = vertices.get(e.getToVertex().getLabel());
        if (gv != null) {
            gv.removeIncoming(e);
        }
    }
    
    public int countVertices () {
        return vertices.size();
    }

    public int countEdges () {
        int nEdges = 0;
        for (GraphVertex gv : vertices.values()) {
            nEdges += gv.getDegreeOut();
        }
        return nEdges;
    }

}