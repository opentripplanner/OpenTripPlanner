package org.opentripplanner.routing.vehicle_rental;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.opentripplanner.util.I18NString;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import java.util.Set;

public abstract class RentalStation {
    @XmlAttribute
    @JsonSerialize
    public String id;

    @XmlTransient
    @JsonIgnore
    public I18NString name;

    @XmlAttribute
    @JsonSerialize
    public double x, y; //longitude, latitude

    @XmlAttribute
    @JsonSerialize
    public boolean allowDropoff = true;

    @XmlAttribute
    @JsonSerialize
    public boolean allowPickup = true;

    /**
     * List of compatible network names. Null (default) to be compatible with all.
     */
    @XmlAttribute
    @JsonSerialize
    public Set<String> networks = null;

    /**
     * The last time (in epoch seconds) that this rental station had updated information. This source of this
     * information varies according to the following fall-back methods:
     *  - First try to use the value for a specific station or floating vehicle was updated according to the provider if
     *  available in the feed. In the context of the GBFS this will be either the
     *  station_status#data#stations#station#last_reported or free_bike_status#data#bikes#bike#last_reported. Note that
     *  free_bike_status#data#bikes#bike#last_reported is available in feeds compliant with GBFS-2.1-RC+.
     *  - If per-station or per-vehicle information is not available, try to use the feed-wide last update time. In the
     *  context of the GBFS, this will be either the station_status#last_udated or free_bike_status#last_updated field.
     *  - If feed-wide data on when the last report was, then use the current timestamp when the data was received by
     *  OTP.
     */
    @XmlAttribute
    @JsonSerialize
    public Long lastReportedEpochSeconds;

    /**
     * Obtain a desired reported time using fallbacks. First try to use a station-specific value, then a feed-specific
     * value, then the current system time. Assume that values of 0 indicate a non-set time.
     */
    public static Long getLastReportedTimeUsingFallbacks(Long stationSpecificValue, Integer feedSpecificValue) {
        if (stationSpecificValue != null && stationSpecificValue > 0) return stationSpecificValue;
        if (feedSpecificValue != null && feedSpecificValue > 0) return Long.valueOf(feedSpecificValue);
        return System.currentTimeMillis() / 1000;
    }
}

