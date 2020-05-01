package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.RentalStationVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.Locale;

/**
 * A class to model an edge that links from the street network to a rental station or a rental station to the street
 * network.
 */
public abstract class StreetRentalLink extends Edge {
    private static final long serialVersionUID = 1L;

    private RentalStationVertex rentalStationVertex;

    public StreetRentalLink(StreetVertex fromv, RentalStationVertex tov) {
        super(fromv, tov);
        rentalStationVertex = tov;
    }

    public StreetRentalLink(RentalStationVertex fromv, StreetVertex tov) {
        super(fromv, tov);
        rentalStationVertex = fromv;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        return null;
    }

    public String getName() {
        return rentalStationVertex.getName();
    }

    @Override
    public String getName(Locale locale) {
        return rentalStationVertex.getName(locale);
    }
}
