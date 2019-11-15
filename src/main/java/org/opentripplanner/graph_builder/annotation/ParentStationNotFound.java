package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Stop;

public class ParentStationNotFound implements DataImportIssue {

    public static final String FMT = "Parent station %s not found. Stop %s will not be linked to a "
            + "parent station.";

    final String parentStop;

    final Stop stop;

    public ParentStationNotFound(Stop stop, String parentStop){
    	this.stop = stop;
    	this.parentStop = parentStop;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, parentStop, stop);
    }
}
