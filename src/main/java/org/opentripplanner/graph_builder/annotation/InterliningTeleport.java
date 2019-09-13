package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.model.Trip;

public class InterliningTeleport extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Interlining trip '%s' on block '%s' implies teleporting %d meters.";

    final Trip prevTrip;
    final String blockId;
    final int distance;

    public InterliningTeleport(Trip prevTrip, String blockId, int distance){
    	this.prevTrip = prevTrip;
    	this.blockId = blockId;
    	this.distance = distance;
    }
    
    @Override
    public String getMessage() {
        return String.format(FMT, prevTrip, blockId, distance);
    }

}
