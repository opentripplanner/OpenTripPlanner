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

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.opentripplanner.util.model.EncodedPolylineBean;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place on a
 * particular vehicle (or on foot).
 */

public class Leg {

    /**
     * The amount of time in milliseconds it takes to traverse this leg.
     */
    public long duration = 0;
    
    /**
     * The date and time this leg begins.
     */
    public Date startTime = null;
    
    /**
     * The date and time this leg ends.
     */
    public Date endTime = null;
    
    /**
     * The distance traveled while traversing the leg in meters.
     */
    public Double distance = null;

    /**
     * The mode (e.g., <code>Walk</code>) used when traversing this leg.
     */
    @XmlAttribute
    public String mode = "Walk";

    /**
     * For transit legs, the route of the bus or train being used. For non-transit legs, the name of
     * the street being traversed.
     */
    @XmlAttribute
    public String route = "";

    /**
     * For transit legs, the headsign of the bus or train being used. For non-transit legs, null.
     */
    @XmlAttribute
    public String headsign = null;

    /**
     * For transit legs, the ID of the transit agency that operates the service used for this leg.
     * For non-transit legs, null.
     */
    @XmlAttribute
    public String agencyId = null;
    
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
    public List<Place> stop;
    
    /**
     * The leg's geometry.
     */
    public EncodedPolylineBean legGeometry;

    /**
     * A series of turn by turn instructions used for walking, biking and driving. 
     */
    @XmlElementWrapper(name = "steps")
    public List<WalkStep> walkSteps;
}
