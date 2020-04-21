package org.opentripplanner.ext.transferanalyzer.annotations;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.model.Stop;

/**
 * Represents two stops where the routing distance between them (using OSM data) is much longer than the euclidean
 * distance
 */
public class TransferRoutingDistanceTooLong implements DataImportIssue {

    private static final String FMT = "Routing distance between stop %s and stop %s is %.0f times longer than the " +
            "euclidean distance. Street distance: %.2f, direct distance: %.2f.";

    private static final String HTMLFMT = "Routing distance between stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> and stop " +
            "<a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\" (%s)</a> is %.0f times longer than " +
            "the euclidean distance. Street distance: %.2f, direct distance: %.2f.";

    private final Stop origin;
    private final Stop destination;
    private final double directDistance;
    private final double streetDistance;
    private final double ratio;

    public TransferRoutingDistanceTooLong(Stop origin, Stop destination, double directDistance, double streetDistance, double ratio) {
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

        return String.format(HTMLFMT, origin.getLat(), origin.getLon(),
                origin.getName(), origin.getId(), destination.getLat(),
                destination.getLon(), destination.getName(), destination.getId(),
                ratio, streetDistance, directDistance);
    }
}
