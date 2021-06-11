package org.opentripplanner.routing.vehicle_parking;

import java.io.Serializable;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;

@Getter
@Builder
@EqualsAndHashCode
public class VehicleParkingEntrance implements Serializable {

    @EqualsAndHashCode.Exclude
    private final VehicleParking vehicleParking;

    private final FeedScopedId entranceId;

    private final double x, y;

    private final I18NString name;

    // Used to explicitly specify the intersection to link to instead of using (x, y)
    @EqualsAndHashCode.Exclude
    @Setter
    private transient StreetVertex vertex;

    // If this entrance should be linked to car accessible streets
    private final boolean carAccessible;

    // If this entrance should be linked to walk/bike accessible streets
    private final boolean walkAccessible;

    void clearVertex() {
        vertex = null;
    }
}
