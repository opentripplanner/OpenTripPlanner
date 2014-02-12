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

package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;

/** An instance of CoordinateSequence that can be efficiently extended */
public class CoordinateArrayListSequence implements CoordinateSequence, Cloneable {
    ArrayList<Coordinate> coordinates;
    
    public CoordinateArrayListSequence() {
        coordinates = new ArrayList<Coordinate>();
    }
    
    @SuppressWarnings("unchecked")
    public CoordinateArrayListSequence(ArrayList<Coordinate> coordinates) {
        this.coordinates = (ArrayList<Coordinate>) coordinates.clone(); 
    }

    @Override
    public Envelope expandEnvelope(Envelope env) {
        for (Coordinate c : coordinates) {
            env.expandToInclude(c);
        }
        return env;
    }

    @SuppressWarnings("unchecked")
    @Override
    public CoordinateArrayListSequence clone() {
        CoordinateArrayListSequence clone;
        try {
            clone = (CoordinateArrayListSequence) super.clone();
        } catch (CloneNotSupportedException e) {
            /* never happens since super is Object */
            throw new RuntimeException(e);
        }
        clone.coordinates = (ArrayList<Coordinate>) coordinates.clone();
        return clone;
    }

    
    @Override
    public Coordinate getCoordinate(int i) {
        return coordinates.get(i);
    }

    @Override
    public void getCoordinate(int index, Coordinate coord) {
        Coordinate internalCoord = coordinates.get(index);
        coord.x = internalCoord.x;
        coord.y = internalCoord.y;
    }

    @Override
    public Coordinate getCoordinateCopy(int i) {
        return new Coordinate(coordinates.get(i));
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public double getOrdinate(int index, int ordinateIndex) {
        // TODO Auto-generated method stub
        return ordinateIndex == 0 ? coordinates.get(index).x : coordinates.get(index).y; 
    }

    @Override
    public double getX(int index) {
        return coordinates.get(index).x;
    }

    @Override
    public double getY(int index) {
        return coordinates.get(index).y;
    }

    @Override
    public void setOrdinate(int index, int ordinateIndex, double value) {
        switch(ordinateIndex) {
        case 0:
            coordinates.get(index).x = value;
            break;
        case 1:
            coordinates.get(index).y = value;
            break;
        default:
            throw new UnsupportedOperationException(); 
        }
    }

    @Override
    public int size() {
        return coordinates.size();
    }

    @Override
    public Coordinate[] toCoordinateArray() {
        return coordinates.toArray(new Coordinate[0]);
    }

    public void extend(Coordinate[] newCoordinates) {
        coordinates.addAll(Arrays.asList(newCoordinates));
    }

    public void extend(Coordinate[] newCoordinates, int start) {
        extend(newCoordinates, start, newCoordinates.length);
    }
    
    public void extend(Coordinate[] newCoordinates, int start, int end) {
        coordinates.addAll(Arrays.asList(newCoordinates).subList(start, end));
    }

    public void add(Coordinate newCoordinate) {
        coordinates.add(newCoordinate);
    }

    public void clear() {
    	coordinates = new ArrayList<Coordinate>();
    }
}
