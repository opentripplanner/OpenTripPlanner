package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.FeedScopedId;

public class BogusShapeGeometry implements DataImportIssue {

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
