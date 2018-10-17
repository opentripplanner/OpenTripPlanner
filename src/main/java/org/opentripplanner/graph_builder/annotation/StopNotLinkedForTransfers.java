package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class StopNotLinkedForTransfers extends GraphBuilderAnnotation {
    private static final long serialVersionUID = 1L;

    public static final String FMT = "Stop %s not near any other stops; no transfers are possible.";

    public static final String HTMLFMT = "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s (%s)\"</a> not near any other stops; no transfers are possible.";

    final TransitStop stop;
    
    public StopNotLinkedForTransfers(TransitStop stop){
    	this.stop = stop;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, stop.getLat(), stop.getLon(), stop.getName(), stop.getStopId());
    }

    @Override
    public String getMessage() {
        return String.format(FMT, stop);
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.stop;
    }
}
