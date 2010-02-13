package org.opentripplanner.routing.core;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import org.opentripplanner.routing.edgetype.Turn;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * This class holds common data for an intersection, including a list of the actual vertices
 * representing that intersection. It assumes no turn restrictions, but could be subclassed to add
 * them.
 */
public class Intersection implements Serializable {
    private static final long serialVersionUID = -6202870304501372173L;

    public ArrayList<IntersectionVertex> vertices;

    public double y;

    public double x;

    public String label;

    public Intersection(String label, double x, double y) {
        this.label = label;
        this.x = x;
        this.y = y;
        vertices = new ArrayList<IntersectionVertex>();
    }

    public int getDegree() {
        return vertices.size();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        vertices.trimToSize();
        out.defaultWriteObject();
    }

    /**
     * This always returns true, but subclasses may override it to implement turn restrictions.
     * 
     * @param fromIndex
     *            the index of the starting IntersectionVertex in the list of vertices
     * @param toIndex
     *            the index of the ending IntersectionVertex in the list of vertices
     * @return whether this turn can be made
     */
    protected boolean canTurn(int fromIndex, int toIndex) {
        return true;
    }

    public Vector<Edge> getOutgoing(IntersectionVertex fromVertex) {
        int fromIndex = vertices.indexOf(fromVertex);
        Vector<Edge> outgoing = new Vector<Edge>();
        for (int i = 0; i < vertices.size(); ++i) {
            if (i != fromIndex && canTurn(fromIndex, i)) {
                outgoing.add(new Turn(fromVertex, vertices.get(i)));
            }
        }
        return outgoing;
    }

    public Vector<Edge> getIncoming(IntersectionVertex toVertex) {
        int toIndex = vertices.indexOf(toVertex);
        Vector<Edge> incoming = new Vector<Edge>();
        for (int i = 0; i < vertices.size(); ++i) {
            if (i != toIndex && canTurn(i, toIndex)) {
                incoming.add(new Turn(vertices.get(i), toVertex));
            }
        }
        return incoming;
    }

    public Coordinate getCoordinate() {
        return new Coordinate(x, y);
    }
}
