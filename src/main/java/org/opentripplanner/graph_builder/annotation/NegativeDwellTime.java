package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.StopTime;

public class NegativeDwellTime extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Negative time dwell at %s; we will assume it is zero.";
    
    final StopTime stop;
    
    public NegativeDwellTime(StopTime stop){
    	this.stop = stop;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, stop);
    }

}
