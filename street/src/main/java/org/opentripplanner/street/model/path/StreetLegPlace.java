package org.opentripplanner.street.model.path;

import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * The start or end of a {@link StreetLeg}.
 *
 * @param vertex                 The vertex where the leg starts or ends
 * @param name                   A display name for the place. For intersections this is derived
 *                               from the names of the streets meeting there.
 * @param realTimeVehicleParking If the vertex is a vehicle parking entrance: whether real-time
 *                               availability data exists for the mode used. Always false otherwise.
 */
public record StreetLegPlace(Vertex vertex, I18NString name, boolean realTimeVehicleParking) {}
