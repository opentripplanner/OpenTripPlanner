/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package org.opentripplanner.api.model;

import java.util.*;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.google.common.collect.ImmutableMap;
import org.opentripplanner.api.model.alertpatch.LocalizedAlert;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.profile.BikeRentalStationInfo;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.graph.Edge;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Lists;
import org.opentripplanner.util.ResourceBundleSingleton;
import org.opentripplanner.util.i18n.T;

/**
 * Represents one instruction in walking directions. Three examples from New York City:
 * <p>
 * Turn onto Broadway from W 57th St (coming from 7th Ave): <br/>
 * distance = 100 (say) <br/>
 * walkDirection = RIGHT <br/>
 * streetName = Broadway <br/>
 * everything else null/false <br/>
 * </p>
 * <p>
 * Now, turn from Broadway onto Central Park S via Columbus Circle <br/>
 * distance = 200 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Central Park S <br/>
 * exit = 1 (first exit) <br/>
 * immediately everything else false <br/>
 * </p>
 * <p>
 * Instead, go through the circle to continue on Broadway <br/>
 * distance = 100 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Broadway <br/>
 * exit = 3 <br/>
 * stayOn = true <br/>
 * everything else false <br/>
 * </p>
 * */
public class WalkStep {

    private static final ImmutableMap<String, T> exitNumbers;
    private static final ImmutableMap<RelativeDirection, T> relativeDirections;
    private static final ImmutableMap<AbsoluteDirection, T> absoluteDirections;

    static {
        exitNumbers = ImmutableMap.<String, T>builder()
            .put("1", T.tr("first"))
            .put("2", T.tr("second"))
            .put("3", T.tr("third"))
            .put("4", T.tr("fourth"))
            .put("5", T.tr("fifth"))
            .put("6", T.tr("sixth"))
            .put("7", T.tr("seventh"))
            .put("8", T.tr("eight"))
            .put("9", T.tr("ninth"))
            .put("10", T.tr("tenth"))
            .build();

        relativeDirections = ImmutableMap.<RelativeDirection, T>builder()
            .put(RelativeDirection.DEPART, T.trc("itinerary", "depart"))
            .put(RelativeDirection.CIRCLE_CLOCKWISE, T.tr("clockwise"))
            .put(RelativeDirection.CIRCLE_COUNTERCLOCKWISE, T.tr("counter clockwise"))
            .put(RelativeDirection.HARD_LEFT, T.tr("hard left"))
            .put(RelativeDirection.LEFT, T.tr("left"))
            .put(RelativeDirection.SLIGHTLY_LEFT, T.tr("slight left"))
            .put(RelativeDirection.CONTINUE, T.tr("continue"))
            .put(RelativeDirection.SLIGHTLY_RIGHT, T.tr("slight right"))
            .put(RelativeDirection.RIGHT, T.tr("right"))
            .put(RelativeDirection.HARD_RIGHT, T.tr("hard right"))
            .put(RelativeDirection.ELEVATOR, T.tr("elevator"))
            .put(RelativeDirection.UTURN_LEFT, T.tr("U-turn left"))
            .put(RelativeDirection.UTURN_RIGHT, T.tr("U-turn right"))
            .build();

        absoluteDirections = ImmutableMap.<AbsoluteDirection, T>builder()
            .put(AbsoluteDirection.NORTH, T.tr("north"))
            .put(AbsoluteDirection.NORTHEAST, T.tr("northeast"))
            .put(AbsoluteDirection.EAST, T.tr("east"))
            .put(AbsoluteDirection.SOUTHEAST, T.tr("southeast"))
            .put(AbsoluteDirection.SOUTH, T.tr("south"))
            .put(AbsoluteDirection.SOUTHWEST, T.tr("southwest"))
            .put(AbsoluteDirection.WEST, T.tr("west"))
            .put(AbsoluteDirection.NORTHWEST, T.tr("northwest"))
            .build();
    }

    /**
     * The distance in meters that this step takes.
     */
    public double distance = 0;

    /**
     * The relative direction of this step.
     */
    public RelativeDirection relativeDirection;

    /**
     * The name of the street.
     */
    public String streetName;

    /**
     * The absolute direction of this step.
     */
    public AbsoluteDirection absoluteDirection;

    /**
     * When exiting a highway or traffic circle, the exit name/number.
     */

    public String exit;

    /**
     * Indicates whether or not a street changes direction at an intersection.
     */
    public Boolean stayOn = false;

    /**
     * This step is on an open area, such as a plaza or train platform, and thus the directions should say something like "cross"
     */
    public Boolean area = false;

    /**
     * The name of this street was generated by the system, so we should only display it once, and generally just display right/left directions
     */
    public Boolean bogusName = false;

    /**
     * The longitude of start of the step
     */
    public double lon;

    /**
     * The latitude of start of the step
     */
    public double lat;

    /**
     * The elevation profile as a comma-separated list of x,y values. x is the distance from the start of the step, y is the elevation at this
     * distance.
     */
    @XmlTransient
    public List<P2<Double>> elevation;

    @XmlElement
    @JsonSerialize
    public List<LocalizedAlert> alerts;

    public transient double angle;

    /**
     * The walkStep's mode; only populated if this is the first step of that mode in the leg.
     * Used only in generating the streetEdges array in StreetSegment; not serialized. 
     */
    public transient String newMode;

    /**
     * The street edges that make up this walkStep.
     * Used only in generating the streetEdges array in StreetSegment; not serialized. 
     */
    public transient List<Edge> edges = Lists.newArrayList();

    public String shortDescription;

    public String longDescription;
    /**
     * The bike rental on/off station info.
     * Used only in generating the streetEdges array in StreetSegment; not serialized. 
     */
    public transient BikeRentalStationInfo bikeRentalOnStation, bikeRentalOffStation;

    public void setDirections(double lastAngle, double thisAngle, boolean roundabout) {
        relativeDirection = getRelativeDirection(lastAngle, thisAngle, roundabout);
        setAbsoluteDirection(thisAngle);
    }

    public String toString() {
        String direction = absoluteDirection.toString();
        if (relativeDirection != null) {
            direction = relativeDirection.toString();
        }
        return "WalkStep(" + direction + " on " + streetName + " for " + distance + ")";
    }

    public static RelativeDirection getRelativeDirection(double lastAngle, double thisAngle,
            boolean roundabout) {

        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;

        if (roundabout) {
            // roundabout: the direction we turn onto it implies the circling direction
            if (angleDiff > ccwAngleDiff) {
                return RelativeDirection.CIRCLE_CLOCKWISE;
            } else {
                return RelativeDirection.CIRCLE_COUNTERCLOCKWISE;
            }
        }

        // less than 0.3 rad counts as straight, to simplify walking instructions
        if (angleDiff < 0.3 || ccwAngleDiff < 0.3) {
            return RelativeDirection.CONTINUE;
        } else if (angleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_RIGHT;
        } else if (ccwAngleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_LEFT;
        } else if (angleDiff < 2) {
            return RelativeDirection.RIGHT;
        } else if (ccwAngleDiff < 2) {
            return RelativeDirection.LEFT;
        } else if (angleDiff < Math.PI) {
            return RelativeDirection.HARD_RIGHT;
        } else {
            return RelativeDirection.HARD_LEFT;
        }
    }

    public void setAbsoluteDirection(double thisAngle) {
        int octant = (int) (8 + Math.round(thisAngle * 8 / (Math.PI * 2))) % 8;
        absoluteDirection = AbsoluteDirection.values()[octant];
    }

    public void addAlerts(Collection<Alert> newAlerts, Locale locale) {
        if (newAlerts == null) {
            return;
        }
        if (alerts == null) {
            alerts = new ArrayList<>();
        }
        ALERT: for (Alert newAlert : newAlerts) {
            for (LocalizedAlert alert : alerts) {
                if (alert.alert.equals(newAlert)) {
                    break ALERT;
                }
            }
            alerts.add(new LocalizedAlert(newAlert, locale));
        }
    }

    public String streetNameNoParens() {
        int idx = streetName.indexOf('(');
        if (idx <= 0) {
            return streetName;
        }
        return streetName.substring(0, idx - 1);
    }

    @XmlJavaTypeAdapter(ElevationAdapter.class)
    @JsonSerialize
    public List<P2<Double>> getElevation() {
        return elevation;
    }

    public void addDescriptions(Locale requestedLocale) {
        Map<String, String> translations = new HashMap<>(4);
        translations.put("relativeDirection", getLocalizedRelativeDirection(requestedLocale));
        translations.put("streetName", streetName);
        translations.put("absoluteDirection", getLocalizedAbsoluteDirection(requestedLocale));
        if (relativeDirection == RelativeDirection.CIRCLE_COUNTERCLOCKWISE || relativeDirection == RelativeDirection.CIRCLE_CLOCKWISE) {
            translations.put("ordinal_exit_number", getOrdinalExitTranslation(requestedLocale));
            longDescription = ResourceBundleSingleton.INSTANCE.localizeGettextSprintfFormat(T.tr(
                "Take roundabout %(relativeDirection)s to %(ordinal_exit_number)s exit on %(streetName)s"),
                requestedLocale, translations);
        } else {
            if (relativeDirection == RelativeDirection.DEPART) {
                longDescription = ResourceBundleSingleton.INSTANCE.localizeGettextSprintfFormat(
                    T.tr("Start on <b>%(streetName)s</b> heading %(absoluteDirection)s"),
                    requestedLocale, translations);
            } else {
                if (stayOn) {
                    longDescription = ResourceBundleSingleton.INSTANCE
                        .localizeGettextSprintfFormat(T.tr(
                            "<b>%(relativeDirection)s</b> to continue on <b>%(streetName)s</b>"),
                            requestedLocale, translations);
                } else {
                    longDescription = ResourceBundleSingleton.INSTANCE
                        .localizeGettextSprintfFormat(
                            T.tr("<b>%(relativeDirection)s</b> on to <b>%(streetName)s</b>"),
                            requestedLocale, translations);
                }

            }
        }
        longDescription = longDescription.substring(0, 1).toUpperCase(requestedLocale) + longDescription.substring(1);
    }

    /**
     * This returns localized ordinal number 1-10. or number + . for other numbers.
     *
     * It is used in localization of roundabout exits
     * @param requestedLocale
     * @return
     */
    private String getOrdinalExitTranslation(Locale requestedLocale) {
        if (Integer.valueOf(exit) > 10) {
            return exit + ".";
        } else if(exitNumbers.containsKey(exit)) {
            return ResourceBundleSingleton.INSTANCE.localizeGettext(exitNumbers.get(exit), requestedLocale);
        } else {
            return exit;
        }
    }

    private String getLocalizedRelativeDirection(Locale requestedLocale) {
        return ResourceBundleSingleton.INSTANCE.localizeGettext(relativeDirections.get(relativeDirection), requestedLocale);
    }

    private String getLocalizedAbsoluteDirection(Locale requestedLocale) {
        return ResourceBundleSingleton.INSTANCE.localizeGettext(absoluteDirections.get(absoluteDirection), requestedLocale);
    }

}
