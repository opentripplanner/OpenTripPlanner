package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StopVertex;

/**
 * Represents two stops that are close to each other where no route is found between them using OSM data
 */
public class TransferCouldNotBeRouted extends GraphBuilderAnnotation {

    private static final long serialVersionUID = 1L;

    private static final String FMT = "Connection between stop %s and stop %s could not be routed. " +
            "Euclidean distance is %.0f.";

    private static final String HTMLFMT = "Connection between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> could not be routed. " +
            "Euclidean distance is %.0f.";

    private final StopVertex origin;
    private final StopVertex destination;
    private final double directDistance;

    public TransferCouldNotBeRouted(StopVertex origin, StopVertex destination, double directDistance) {
        this.origin = origin;
        this.destination = destination;
        this.directDistance = directDistance;
    }

    @Override
    public String getMessage() {
        return String.format(FMT, origin, destination, directDistance);
    }

    @Override
    public String getHTMLMessage() {
        Stop o = origin.getStop();
        Stop d = destination.getStop();

        return String.format(HTMLFMT, origin.getStop().getLat(), origin.getStop().getLon(),
                o.getName(), o.getId(), d.getLat(),
                d.getLon(), d.getName(), d.getId(),
                directDistance);
    }

    @Override
    public Vertex getReferencedVertex() {
        return this.origin;
    }
}
