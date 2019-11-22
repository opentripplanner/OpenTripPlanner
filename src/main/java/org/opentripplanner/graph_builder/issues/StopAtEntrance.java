package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.StopTime;

public class StopAtEntrance implements DataImportIssue {

    public static final String FMT = "The stoptime %s stops at an entrance. ";
    
    final StopTime st;
    final boolean repaired;
    
    public StopAtEntrance(StopTime st, boolean repaired){
    	this.st = st;
    	this.repaired = repaired;
    }
    
    @Override
    public String getMessage() {
        String ret;
        if (repaired) {
            ret = "We have corrected for this by using the parent station.";
        } else {
            ret = "We could not correct for this.";
        }
        return String.format(FMT.concat(ret), st);
    }

}
