package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Stop;

/**
 * Represents two stops that are close to each other where no route is found between them using OSM data
 */
public class TransferCouldNotBeRouted implements DataImportIssue {

    private static final String FMT = "Connection between stop %s and stop %s could not be routed. " +
            "Euclidean distance is %.0f.";

    private static final String HTMLFMT = "Connection between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> could not be routed. " +
            "Euclidean distance is %.0f.";

    private final Stop origin;
    private final Stop destination;
    private final double directDistance;

    public TransferCouldNotBeRouted(Stop origin, Stop destination, double directDistance) {
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
        return String.format(HTMLFMT, origin.getLat(), origin.getLon(),
                origin.getName(), origin.getId(), destination.getLat(),
                destination.getLon(), destination.getName(), destination.getId(),
                directDistance);
    }
}
