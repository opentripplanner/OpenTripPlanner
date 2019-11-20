package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.StopTime;

public class NegativeDwellTime implements DataImportIssue {

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
