package org.opentripplanner.graph_builder.annotation;

import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class StopLinkedTooFar extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Stop %s is far away from nearest street. Snap distance is %s.";
    public static final String HTMLFMT = "Stop <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s&layers=T\">\"%s\" (%s)</a> is far away from nearest street. Snap distance is %s.";

    final TransitStop stop;
    final int distance;

    public StopLinkedTooFar(TransitStop stop, int distance) {
        this.stop = stop;
        this.distance = distance;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, stop, Integer.toString(distance));
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, stop.getStop().getLat(), stop.getStop().getLon(), stop.getName(), stop.getStopId(), Integer.toString(distance));
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.stop;
    }
}