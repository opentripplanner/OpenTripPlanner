package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.vertextype.VehicleParkingEntranceVertex;

public class VehicleParkingUnlinked implements DataImportIssue {

    private static final String FMT = "Vehicle parking %s not near any streets; it will not be usable.";
    private static final String HTMLFMT = "Vehicle parking <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\"</a> not near any streets; it will not be usable.";

    final VehicleParkingEntranceVertex vehicleParkingEntranceVertex;

    public VehicleParkingUnlinked(VehicleParkingEntranceVertex vehicleParkingEntranceVertex) {
        this.vehicleParkingEntranceVertex = vehicleParkingEntranceVertex;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, vehicleParkingEntranceVertex.getLat(), vehicleParkingEntranceVertex
                .getLon(),
            vehicleParkingEntranceVertex
        );
    }

    @Override
    public String getMessage() {
        return String.format(FMT, vehicleParkingEntranceVertex);
    }

}
