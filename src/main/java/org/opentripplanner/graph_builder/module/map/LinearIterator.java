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

package org.opentripplanner.graph_builder.module.map;

import java.util.Iterator;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.linearref.LinearLocation;

/**
 * I copied this class from JTS but made a few changes.
 * 
 * The JTS version of this class has several design decisions that don't work for me. In particular,
 * hasNext() in the original should be "isValid", and if we start mid-segment, we should continue at
 * the end of this segment rather than the end of the next segment.
 */
public class LinearIterator implements Iterable<LinearLocation> {

    private Geometry linearGeom;

    private final int numLines;

    /**
     * Invariant: currentLine <> null if the iterator is pointing at a valid coordinate
     * 
     * @throws IllegalArgumentException if linearGeom is not lineal
     */
    private LineString currentLine;

    private int componentIndex = 0;

    private int vertexIndex = 0;

    private double segmentFraction;

    /**
     * Creates an iterator initialized to the start of a linear {@link Geometry}
     * 
     * @param linear the linear geometry to iterate over
     * @throws IllegalArgumentException if linearGeom is not lineal
     */
    public LinearIterator(Geometry linear) {
        this(linear, 0, 0);
    }

    /**
     * Creates an iterator starting at a {@link LinearLocation} on a linear {@link Geometry}
     * 
     * @param linear the linear geometry to iterate over
     * @param start the location to start at
     * @throws IllegalArgumentException if linearGeom is not lineal
     */
    public LinearIterator(Geometry linear, LinearLocation start) {
        this(linear, start.getComponentIndex(), start.getSegmentIndex());
        this.segmentFraction = start.getSegmentFraction();
    }

    /**
     * Creates an iterator starting at a specified component and vertex in a linear {@link Geometry}
     * 
     * @param linearGeom the linear geometry to iterate over
     * @param componentIndex the component to start at
     * @param vertexIndex the vertex to start at
     * @throws IllegalArgumentException if linearGeom is not lineal
     */
    public LinearIterator(Geometry linearGeom, int componentIndex, int vertexIndex) {
        if (!(linearGeom instanceof Lineal))
            throw new IllegalArgumentException("Lineal geometry is required");
        this.linearGeom = linearGeom;
        numLines = linearGeom.getNumGeometries();
        this.componentIndex = componentIndex;
        this.vertexIndex = vertexIndex;
        loadCurrentLine();
    }

    private void loadCurrentLine() {
        if (componentIndex >= numLines) {
            currentLine = null;
            return;
        }
        currentLine = (LineString) linearGeom.getGeometryN(componentIndex);
    }

    /**
     * Tests whether there are any vertices left to iterator over.
     * 
     * @return <code>true</code> if there are more vertices to scan
     */
    public boolean hasNext() {
        if (componentIndex >= numLines)
            return false;
        if (componentIndex == numLines - 1 && vertexIndex >= currentLine.getNumPoints() - 1)
            return false;
        return true;
    }
    
    public boolean isValidIndex() {
        if (componentIndex >= numLines)
            return false;
        if (componentIndex == numLines - 1 && vertexIndex >= currentLine.getNumPoints())
            return false;
        return true;
    }

    /**
     * Moves the iterator ahead to the next vertex and (possibly) linear component.
     */
    public void next() {
        if (!hasNext())
            return;
        segmentFraction = 0.0;
        vertexIndex++;
        if (vertexIndex >= currentLine.getNumPoints()) {
            componentIndex++;
            if (componentIndex < linearGeom.getNumGeometries() - 1) {
                loadCurrentLine();
                vertexIndex = 0;
            }
        }
    }

    /**
     * Checks whether the iterator cursor is pointing to the endpoint of a linestring.
     * 
     * @return <code>true</true> if the iterator is at an endpoint
     */
    public boolean isEndOfLine() {
        if (componentIndex >= numLines)
            return false;
        // LineString currentLine = (LineString) linear.getGeometryN(componentIndex);
        if (vertexIndex < currentLine.getNumPoints() - 1)
            return false;
        return true;
    }

    /**
     * The component index of the vertex the iterator is currently at.
     * 
     * @return the current component index
     */
    public int getComponentIndex() {
        return componentIndex;
    }

    /**
     * The vertex index of the vertex the iterator is currently at.
     * 
     * @return the current vertex index
     */
    public int getVertexIndex() {
        return vertexIndex;
    }

    /**
     * Gets the {@link LineString} component the iterator is current at.
     * 
     * @return a linestring
     */
    public LineString getLine() {
        return currentLine;
    }

    /**
     * Gets the first {@link Coordinate} of the current segment. (the coordinate of the current
     * vertex).
     * 
     * @return a {@link Coordinate}
     */
    public Coordinate getSegmentStart() {
        return currentLine.getCoordinateN(vertexIndex);
    }

    /**
     * Gets the second {@link Coordinate} of the current segment. (the coordinate of the next
     * vertex). If the iterator is at the end of a line, <code>null</code> is returned.
     * 
     * @return a {@link Coordinate} or <code>null</code>
     */
    public Coordinate getSegmentEnd() {
        if (vertexIndex < getLine().getNumPoints() - 1)
            return currentLine.getCoordinateN(vertexIndex + 1);
        return null;
    }

    public LinearLocation getLocation() {
        return new LinearLocation(componentIndex, vertexIndex, segmentFraction);
    }

    class LinearIteratorIterator implements Iterator<LinearLocation> {

        @Override
        public boolean hasNext() {
            return LinearIterator.this.hasNext();
        }

        @Override
        public LinearLocation next() {
            LinearLocation result = getLocation();
            LinearIterator.this.next();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    @Override
    public Iterator<LinearLocation> iterator() {
        return new LinearIteratorIterator();
    }

    public static LinearLocation getEndLocation(Geometry linear) {
        //the version in LinearLocation is broken
        
        int lastComponentIndex = linear.getNumGeometries() - 1;
        LineString lastLine = (LineString) linear.getGeometryN(lastComponentIndex);
        int lastSegmentIndex = lastLine.getNumPoints() - 1;
        return new LinearLocation(lastComponentIndex, lastSegmentIndex, 0.0);
    }
}