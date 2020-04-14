package org.opentripplanner.api.model;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

public class ApiTrip implements Serializable {

        private static final long serialVersionUID = 1L;

        public String id;
        public String routeId;
        public String serviceId;
        public String tripShortName;
        public String tripHeadsign;
        public String routeShortName;
        public String directionId;
        public String blockId;
        public String shapeId;

        /**
         * 0: No accessibility information for the trip.
         * 1: Vehicle being used on this particular trip can accommodate at least one rider in a wheelchair.
         * 2: No riders in wheelchairs can be accommodated on this trip.
         */
        public int wheelchairAccessible = 0;
        /**
         * 0 = unknown / unspecified, 1 = bikes allowed, 2 = bikes NOT allowed
         */
        public int bikesAllowed = 0;

        /** Custom extension for KCM to specify a fare per-trip */
        public String fareId;

        @Override
        public String toString() {
            return "<Trip " + id + ">";
        }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        ApiTrip apiTrip = (ApiTrip) o;
        return id.equals(apiTrip.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
