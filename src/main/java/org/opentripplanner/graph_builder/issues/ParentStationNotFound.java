package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.StationElement;

public class ParentStationNotFound implements DataImportIssue {

    public static final String FMT = "Parent station %s not found. Station element %s will not be "
        + "linked to a parent station.";

    final String parentStop;

    final StationElement stationElement;

    public ParentStationNotFound(StationElement stationElement, String parentStop){
    	this.stationElement = stationElement;
    	this.parentStop = parentStop;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, parentStop, stationElement);
    }
}
