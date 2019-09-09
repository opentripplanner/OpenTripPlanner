package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StopVertex;

/**
 * Represents two stops where the routing distance between them (using OSM data) is much longer than the euclidean
 * distance
 */
public class TransferRoutingDistanceTooLong extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    private static final String FMT = "Routing distance between stop %s and stop %s is %.0f times longer than the " +
            "euclidean distance. Street distance: %.2f, direct distance: %.2f.";

    private static final String HTMLFMT = "Routing distance between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> is %.0f times longer than " +
            "the euclidean distance. Street distance: %.2f, direct distance: %.2f.";

    private final StopVertex origin;
    private final StopVertex destination;
    private final double directDistance;
    private final double streetDistance;
    private final double ratio;

    public TransferRoutingDistanceTooLong(StopVertex origin, StopVertex destination, double directDistance, double streetDistance, double ratio) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
        this.streetDistance = streetDistance;
        this.ratio = ratio;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, ratio, streetDistance , directDistance);
    }

    @Override
    public String getHTMLMessage() {
        Stop o = origin.getStop();
        Stop d = destination.getStop();

        return String.format(HTMLFMT, o.getLat(), o.getLon(),
                o.getName(), o.getId(), d.getLat(),
                d.getLon(), d.getName(), d.getId(),
                ratio, streetDistance, directDistance);
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }
}
