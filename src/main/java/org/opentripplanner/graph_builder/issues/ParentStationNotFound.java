package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TransitEntity;

public class ParentStationNotFound implements DataImportIssue {

    public static final String FMT = "Parent station %s not found. Stop %s will not be linked to a "
            + "parent station.";

    final String parentStop;

    final TransitEntity<FeedScopedId> stop;

    public ParentStationNotFound(TransitEntity<FeedScopedId> stop, String parentStop){
    	this.stop = stop;
    	this.parentStop = parentStop;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, parentStop, stop);
    }
}
