package org.opentripplanner.graph_builder.issues;

import org.opentripplanner.graph_builder.DataImportIssue;
import org.opentripplanner.routing.vertextype.VehicleParkingVertex;

public class VehicleParkingUnlinked implements DataImportIssue {

    private static final String FMT = "Vehicle parking %s not near any streets; it will not be usable.";
    private static final String HTMLFMT = "Vehicle parking <a href=\"http://www.openstreetmap.org/?mlat=%s&mlon=%s\">\"%s\"</a> not near any streets; it will not be usable.";

    final VehicleParkingVertex vehicleParkingVertex;

    public VehicleParkingUnlinked(VehicleParkingVertex vehicleParkingVertex) {
        this.vehicleParkingVertex = vehicleParkingVertex;
    }

    @Override
    public String getHTMLMessage() {
        return String.format(HTMLFMT, vehicleParkingVertex.getLat(), vehicleParkingVertex.getLon(),
            vehicleParkingVertex
        );
    }

    @Override
    public String getMessage() {
        return String.format(FMT, vehicleParkingVertex);
    }

}
