package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.FeedScopedId;

public class BogusShapeGeometry extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Shape geometry for shape_id %s does not have two distinct points.";
    
    final FeedScopedId shapeId;
    
    public BogusShapeGeometry(FeedScopedId shapeId){
    	this.shapeId = shapeId;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, shapeId);
    }

}
