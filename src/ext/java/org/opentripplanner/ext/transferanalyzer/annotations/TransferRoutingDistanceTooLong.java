package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TransferRoutingDistanceTooLong extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    public static final String FMT = "Routing distance between stop %s and stop %s is %s times longer than the " +
            "euclidean distance. Street distance: %s, direct distance: %s.";

    public static final String HTMLFMT = "Routing distance between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> is %s times longer than " +
            "the euclidean distance. Street distance: %s, direct distance: %s.";

    private final TransitStop origin;
    private final TransitStop destination;
    private final double directDistance;
    private final double streetDistance;
    private final double ratio;

    public TransferRoutingDistanceTooLong(TransitStop origin, TransitStop destination, double directDistance, double streetDistance, double ratio) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
        this.streetDistance = streetDistance;
        this.ratio = ratio;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, round(ratio), round2(streetDistance) , round2(directDistance));
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, origin.getStop().getLat(), origin.getStop().getLon(),
                origin.getStop().getName(), origin.getStop().getId(), destination.getStop().getLat(),
                destination.getStop().getLon(), destination.getStop().getName(), destination.getStop().getId(),
                round(ratio), round2(streetDistance), round2(directDistance));
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }

    private String round(Double number) {
        return String.format("%.2f", number);
    }

    private String round2(Double number) {
        return String.format("%.0f", number);
    }
}
