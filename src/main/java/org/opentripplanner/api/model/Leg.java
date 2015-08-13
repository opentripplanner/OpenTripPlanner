/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.model;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.i18n.T;
import org.opentripplanner.util.i18n.translations.Units;
import org.opentripplanner.util.model.EncodedPolylineBean;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

 /**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

public class Leg {

    /**
     * The date and time this leg begins.
     */
    public Calendar startTime = null;
    
    /**
     * The date and time this leg ends.
     */
    public Calendar endTime = null;
    
    /**
     * For transit leg, the offset from the scheduled departure-time of the boarding stop in this leg.
     * "scheduled time of departure at boarding stop" = startTime - departureDelay
     */
    public int departureDelay = 0;
    /**
     * For transit leg, the offset from the scheduled arrival-time of the alighting stop in this leg.
     * "scheduled time of arrival at alighting stop" = endTime - arrivalDelay
     */
    public int arrivalDelay = 0;
    /**
     * Whether there is real-time data about this Leg
     */
    public Boolean realTime = false;
    
    /**
     * Is this a frequency-based trip with non-strict departure times?
     */
    public Boolean isNonExactFrequency = null;
    
    /**
     * The best estimate of the time between two arriving vehicles. This is particularly important 
     * for non-strict frequency trips, but could become important for real-time trips, strict 
     * frequency trips, and scheduled trips with empirical headways.
     */
    public Integer headway = null;
    
    /**
     * The distance traveled while traversing the leg in meters.
     */
    public Double distance = null;
    
    /**
     * Is this leg a traversing pathways?
     */
    public Boolean pathway = false;

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    @XmlAttribute
    @JsonSerialize
    public String mode = TraverseMode.WALK.toString();

    /**
     * For transit legs, the route of the bus or train being used. For non-transit legs, the name of
     * the street being traversed.
     */
    @XmlAttribute
    @JsonSerialize
    public String route = "";

    @XmlAttribute
    @JsonSerialize
    public String agencyName;

    @XmlAttribute
    @JsonSerialize
    public String agencyUrl;

    @XmlAttribute
    @JsonSerialize
    public int agencyTimeZoneOffset;

    /**
     * For transit leg, the route's (background) color (if one exists). For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String routeColor = null;

    /**
     * For transit legs, the type of the route. Non transit -1
     * When 0-7: 0 Tram, 1 Subway, 2 Train, 3 Bus, 4 Ferry, 5 Cable Car, 6 Gondola, 7 Funicular
     * When equal or highter than 100, it is coded using the Hierarchical Vehicle Type (HVT) codes from the European TPEG standard
     * Also see http://groups.google.com/group/gtfs-changes/msg/ed917a69cf8c5bef
     */
    @XmlAttribute
    @JsonSerialize
    public Integer routeType = null;
    
    /**
     * For transit legs, the ID of the route.
     * For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String routeId = null;

    /**
     * For transit leg, the route's text color (if one exists). For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String routeTextColor = null;

    /**
     * For transit legs, if the rider should stay on the vehicle as it changes route names.
     */
    @XmlAttribute
    @JsonSerialize
    public Boolean interlineWithPreviousLeg;

    
    /**
     * For transit leg, the trip's short name (if one exists). For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String tripShortName = null;

    /**
     * For transit leg, the trip's block ID (if one exists). For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String tripBlockId = null;
    
    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String headsign = null;

    /**
     * For transit legs, the ID of the transit agency that operates the service used for this leg.
     * For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String agencyId = null;
    
    /**
     * For transit legs, the ID of the trip.
     * For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String tripId = null;
    
    /**
     * For transit legs, the service date of the trip.
     * For non-transit legs, null.
     */
    @XmlAttribute
    @JsonSerialize
    public String serviceDate = null;
    
    /**
     * The Place where the leg originates.
     */
    public Place from = null;
    
    /**
     * The Place where the leg begins.
     */
    public Place to = null;

    /**
     * For transit legs, intermediate stops between the Place where the leg originates and the Place where the leg ends.
     * For non-transit legs, null.
     * This field is optional i.e. it is always null unless "showIntermediateStops" parameter is set to "true" in the planner request.
     */
    @XmlElementWrapper(name = "intermediateStops")
    @JsonProperty(value="intermediateStops")
    public List<Place> stop;

    /**
     * The leg's geometry.
     */
    public EncodedPolylineBean legGeometry;

    /**
     * A series of turn by turn instructions used for walking, biking and driving. 
     */
    @XmlElementWrapper(name = "steps")
    @JsonProperty(value="steps")
    public List<WalkStep> walkSteps;

    @XmlElement
    @JsonSerialize
    public List<LocalizedAlert> alerts;

    @XmlAttribute
    @JsonSerialize
    public String routeShortName;

    @XmlAttribute
    @JsonSerialize
    public String routeLongName;

    @XmlAttribute
    @JsonSerialize
    public String boardRule;

    @XmlAttribute
    @JsonSerialize
    public String alightRule;

    @XmlAttribute
    @JsonSerialize
    public Boolean rentedBike;

    public String longDescription;

    public String shortDescription;

     /**
      * Creates route number route name to headsign/stop name if transit stop
      *
      * or just to destination name if not
      * @param requestedLocale
      * @param removeTagsFromLocalizations If true it removes all tags from step and leg localizations (long and short descriptions)
      */
     public void addDescriptions(Locale requestedLocale, boolean removeTagsFromLocalizations) {
         if (isTransitLeg()) {
             longDescription = agencyName + ",<b>" + routeShortName + "</b>" + " " + routeLongName;
             if (headsign != null) {
                 //should be towards
                 longDescription += ResourceBundleSingleton.INSTANCE.localizeGettext(T.trc("bus_direction", " to "), requestedLocale);
                 longDescription += headsign;
             } else {
                 longDescription += ResourceBundleSingleton.INSTANCE.localizeGettext(T.trc("direction", " to "), requestedLocale);
                 longDescription += to.name;
             }
         } else {
             T trans;
             if (mode.equals(TraverseMode.WALK.toString())) {
                  trans = T.tr("Walk");
             } else if (mode.equals(TraverseMode.BICYCLE.toString())) {
                 trans = T.tr("Cycle");
             } else if (mode.equals(TraverseMode.CAR.toString())) {
                 trans = T.tr("Car");
             } else {
                 trans = T.tr("");
             }
             longDescription = ResourceBundleSingleton.INSTANCE.localizeGettext(trans, requestedLocale) + ": " + getDistanceString(requestedLocale) +  ResourceBundleSingleton.INSTANCE.localizeGettext(T.trc("direction", " to "), requestedLocale);
             longDescription += to.name;

             shortDescription = ResourceBundleSingleton.INSTANCE.localizeGettext(T.trc("direction", " to "), requestedLocale);
             shortDescription += to.name;

             if (removeTagsFromLocalizations) {
                 longDescription = ResourceBundleSingleton.removeHTMLTags(longDescription);
                 shortDescription = ResourceBundleSingleton.removeHTMLTags(shortDescription);
             }
         }
     }

     /**
      * Returns string with humanized distance
      *
      * Units are based on Locale in {@link ResourceBundleSingleton#getUnits(Locale)}
      *
      * Humanized units look like this:
      * - if less then 528 feet: 123 feet
      * - else 0.45 miles
      * - if more then 100 km: 856 km
      * - if more then 1 km: 5.26 km
      * - else: 565 m
      * @param requestedLocale
      * @return
      */
     public String getDistanceString(Locale requestedLocale) {
         Units units = ResourceBundleSingleton.INSTANCE.getUnits(requestedLocale);
         if (units == Units.IMPERIAL) {
             double ft = distance*3.28084;
             if (ft < 528) {
                 return String.format(requestedLocale, "%.0f feet", ft);
             } else {
                 ft = ft/52.8/100;
                 return String.format(requestedLocale, "%.2f miles", ft);
             }
         } else {
             double km = distance/1000;
             if (km > 100) {
                 //100 km => 99999999 km
                 return String.format(requestedLocale, "%.0f km", km);
             } else if(km > 1) {
                 //1.11 km => 99.99 km
                 return String.format(requestedLocale, "%.2f km", km);
             } else {
                 //1m => 999 m
                 return String.format(requestedLocale, "%.0f m", distance);
             }
         }
     }

    /**
     * Whether this leg is a transit leg or not.
     * @return Boolean true if the leg is a transit leg
     */
    public Boolean isTransitLeg() {
        if (mode == null) return null;
        else if (mode.equals(TraverseMode.WALK.toString())) return false;
        else if (mode.equals(TraverseMode.CAR.toString())) return false;
        else if (mode.equals(TraverseMode.BICYCLE.toString())) return false;
        else return true;
    }
    
    /** 
     * The leg's duration in seconds
     */
    @XmlElement
    @JsonSerialize
    public double getDuration() {
        return endTime.getTimeInMillis()/1000.0 - startTime.getTimeInMillis()/1000.0;
    }

    public void addAlert(Alert alert, Locale locale) {
        if (alerts == null) {
            alerts = new ArrayList<>();
        }
        for (LocalizedAlert a : alerts) {
            if (a.alert.equals(alert)) {
                return;
            }
        }
        alerts.add(new LocalizedAlert(alert, locale));
    }

    public void setTimeZone(TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(startTime.getTime());
        startTime = calendar;
        calendar = Calendar.getInstance(timeZone);
        calendar.setTime(endTime.getTime());
        endTime = calendar;
        agencyTimeZoneOffset = timeZone.getOffset(startTime.getTimeInMillis());
    }
}
