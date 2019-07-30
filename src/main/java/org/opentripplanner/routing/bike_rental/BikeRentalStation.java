package org.opentripplanner.routing.bike_rental;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.ResourceBundleSingleton;

public class BikeRentalStation implements Serializable, Cloneable {
    private static final long serialVersionUID = 8311460609708089384L;

    @JsonSerialize
    public String id;
    //Serialized in TranslatedBikeRentalStation
    @JsonIgnore
    public I18NString name;
    @JsonSerialize
    public double x, y; //longitude, latitude
    @JsonSerialize
    public int bikesAvailable = Integer.MAX_VALUE;
    @JsonSerialize
    public int spacesAvailable = Integer.MAX_VALUE;
    @JsonSerialize
    public boolean allowDropoff = true;
    @JsonSerialize
    public boolean isFloatingBike = false;
    @JsonSerialize
    public boolean isCarStation = false;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    @JsonSerialize
    public Set<String> networks = null;
    
    /**
     * Whether this station is static (usually coming from OSM data) or a real-time source. If no real-time data, users should take
     * bikesAvailable/spacesAvailable with a pinch of salt, as they are always the total capacity divided by two. Only the total is meaningful.
     */
    @JsonSerialize
    public boolean realTimeData = true;

    /**
     * This is used for localization. Currently "bike rental station" isn't part of the name.
     * It can be added on the client. But since it is used as Station: name, and Recommended Pick Up: name.
     * It isn't used.
     *
     * Names can be different in different languages if name tags in OSM have language tags.
     *
     * It is set in {@link org.opentripplanner.api.resource.BikeRental} from URL parameter.
     *
     * Sets default locale on start
     *
     */
    @JsonIgnore
    public Locale locale = ResourceBundleSingleton.INSTANCE.getLocale(null);

    /**
     * FIXME nonstandard definition of equals, relying on only the station field.
     * We should probably be keying collections on station ID rather than the station object with nonstandard equals.
     */
    public boolean equals(Object o) {
        if (!(o instanceof BikeRentalStation)) {
            return false;
        }
        BikeRentalStation other = (BikeRentalStation) o;
        return other.id.equals(id);
    }
    
    public int hashCode() {
        return id.hashCode() + 1;
    }
    
    public String toString () {
        return String.format(Locale.US, "Bike rental station %s at %.6f, %.6f", name, y, x); 
    }

    @Override
    public BikeRentalStation clone() {
        try {
            return (BikeRentalStation) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(); //can't happen
        }
    }

    /**
     * Gets translated name of bike rental station based on locale
     */
    @JsonSerialize
    public String getName() {
        return name.toString(locale);
    }
}
