package org.opentripplanner.common.geometry;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;

public class Serializable2DPackedCoordinateSequenceFactory implements Serializable, CoordinateSequenceFactory {

    private static final long serialVersionUID = 1L;
    
    @Override
    public CoordinateSequence create(Coordinate[] coordinates) {
        return new PackedCoordinateSequence.Double(coordinates, 2);
        //return new IntPackedCoordinateSequence(coordinates);
    }

    @Override
    public CoordinateSequence create(CoordinateSequence coordSeq) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CoordinateSequence create(int size, int dimension) {
        throw new UnsupportedOperationException();
    }
    

}
