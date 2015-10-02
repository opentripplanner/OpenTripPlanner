

package org.opentripplanner.util;

import java.util.HashSet;
import java.util.Objects;

/**
 * This class is used to send to client which Travel Options are possible on this server
 *
 * This options are used in client "Travel by" drop down.
 *
 * Each travel option consist of two variables:
 * - value is a value which is sent to the server if this is chosen ("TRANSIT, WALK", "CAR", etc.)
 * - name is a name with which client can nicely name this option even if specific value changes ("TRANSIT", "PARKRIDE", "TRANSIT_BICYCLE", etc.)
 *
 * Travel options are created from {@link org.opentripplanner.routing.graph.Graph} transitModes variable and based if park & ride, bike & ride, bike sharing is supported.
 * List itself is created in {@link TravelOptionsMaker#makeOptions(HashSet, boolean, boolean, boolean)}
 *
 * @see TravelOptionsMaker#makeOptions(HashSet, boolean, boolean, boolean)
 * * Created by mabu on 28.7.2015.
 */
public class TravelOption {
    public String value;
    public String name;

    public TravelOption(String value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Creates TravelOption where value and name are same
     *
     * @param value
     */
    public TravelOption(String value) {
        this.value = value;
        this.name = value;
    }

    @Override public String toString() {
        return "TravelOption{" +
            "value='" + value + '\'' +
            ", name='" + name + '\'' +
            '}';
    }

    @Override public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TravelOption that = (TravelOption) o;
        return Objects.equals(value, that.value) && Objects.equals(name, that.name);
    }

    @Override public int hashCode() {
        return Objects.hash(value, name);
    }
}
