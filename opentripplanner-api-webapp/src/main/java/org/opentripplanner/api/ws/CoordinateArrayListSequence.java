package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Arrays;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Envelope;

/** An instance of CoordinateSequence that can be efficiently extended */
public class CoordinateArrayListSequence implements CoordinateSequence {
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

    @Override
    public CoordinateArrayListSequence clone() {
        return new CoordinateArrayListSequence(coordinates);
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

}
