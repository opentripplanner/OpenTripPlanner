package org.opentripplanner.graph_builder.annotation;


import org.opentripplanner.model.StopTime;

public class BogusShapeDistanceTraveled extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "The shape_dist_traveled field for stoptime %s is wrong -- " +
    		"either it is the same as the value for the previous stoptime, or it is greater " +
    		"than the max shape_dist_traveled for the shape in shapes.txt";
    
    final StopTime st;
    
    public BogusShapeDistanceTraveled(StopTime st){
    	this.st = st;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, st);
    }

}
