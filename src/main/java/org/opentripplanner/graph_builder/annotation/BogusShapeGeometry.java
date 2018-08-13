package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.FeedId;

public class BogusShapeGeometry extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Shape geometry for shape_id %s does not have two distinct points.";
    
    final FeedId shapeId;
    
    public BogusShapeGeometry(FeedId shapeId){
    	this.shapeId = shapeId;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, shapeId);
    }

}
