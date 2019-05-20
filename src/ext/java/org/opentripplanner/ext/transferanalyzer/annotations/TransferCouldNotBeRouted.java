package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TransferCouldNotBeRouted extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Connection between stop %s and stop %s could not be routed. " +
            "Euclidean distance is %s.";

    public static final String HTMLFMT = "Connection between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> could not be routed. " +
            "Euclidean distance is %s.";

    private final TransitStop origin;
    private final TransitStop destination;
    private final double directDistance;

    public TransferCouldNotBeRouted(TransitStop origin, TransitStop destination, double directDistance) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, round2(directDistance));
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, origin.getStop().getLat(), origin.getStop().getLon(),
                origin.getStop().getName(), origin.getStop().getId(), destination.getStop().getLat(),
                destination.getStop().getLon(), destination.getStop().getName(), destination.getStop().getId(),
                round2(directDistance));
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }

    private String round2(Double number) {
        return String.format("%.0f", number);
    }
}
