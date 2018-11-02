package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopTime;

public class BogusShapeGeometryCaught extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Shape geometry for shape_id %s cannot be used with stop " +
    		"times %s and %s; using straight-line path instead";
    
    final FeedScopedId shapeId;
    final StopTime stA;
    final StopTime stB;
    
    public BogusShapeGeometryCaught(FeedScopedId shapeId, StopTime stA, StopTime stB){
    	this.shapeId = shapeId;
    	this.stA = stA;
    	this.stB = stB;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, shapeId, stA, stB);
    }

}
