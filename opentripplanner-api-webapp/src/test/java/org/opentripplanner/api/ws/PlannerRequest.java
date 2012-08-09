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

package org.opentripplanner.api.ws;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;

/**
 * Sample POJO for ReflectiveQueryScraper 
 */
public class PlannerRequest {

    /** The router ID -- internal ID to switch between router implementation (or graphs) */
    public String routerId;
    
    /** The start location -- either a Vertex name or latitude, longitude in degrees */
    public String from;
    
    /** The start location's user-visible name */
    public String fromName;

    /** The end location (see the from field for format) */
    public String to;

    /** The end location's user-visible name */
    public String toName;

    /** An unordered list of intermediate locations to be visited (see the from field for format) */
    public List<NamedPlace> intermediatePlaces;

    /** The maximum distance (in meters) the user is willing to walk. Defaults to 1/2 mile  */
    public double maxWalkDistance = Double.MAX_VALUE;

    /** The set of TraverseModes that a user is willing to use. Defaults to WALK | TRANSIT */
    public TraverseModeSet mode; // defaults in constructor

    /** The set of characteristics that the user wants to optimize for -- defaults to QUICK, or
     *  optimize for transit time. */
    public OptimizeType optimize = OptimizeType.QUICK;
    
    /** The date/time that the trip should depart (or arrive, for requests where arriveBy is true) */
    public Date dateTime = new Date();

    /** Whether the trip should depart at dateTime (false, the default), or arrive at dateTime */
    public boolean arriveBy = false;

    /** Whether the trip must be wheelchair accessible. */
    public boolean wheelchair = false;
  
    /** The maximum number of possible itineraries to return. */
    public int numItineraries = 3;

    /** The maximum slope of streets for wheelchair trips. */
    public double maxSlope = -1;
    
    /** Whether the planner should return intermediate stops lists for transit legs. */
    public boolean showIntermediateStops = false;
    
    /** List of preffered routes. */
    public String[] preferredRoutes;
    
    /** List of unpreferred routes. */
    public String[] unpreferredRoutes;
    
    /** The complete list of parameters. */
    public final HashMap<String, String> parameters = new HashMap<String, String>();

    /** TODO: documentation */
    public Integer minTransferTime;

    /** TODO: documentation */
    public String[] bannedRoutes;
    
    /** TODO: documentation */
    public Integer transferPenalty;
    
    /** TODO: documentation */
    public double walkSpeed;
    
    /** TODO: documentation */
    public double bikeSpeed;

    /** TODO: documentation */
    public double triangleSafetyFactor;
    
    /** TODO: documentation */
    public double triangleSlopeFactor;
    
    /** TODO: documentation */
    public double triangleTimeFactor;
    
    /** TODO: documentation */
    public Integer maxTransfers;
    
    /** TODO: documentation */
    public boolean intermediatePlacesOrdered;
    
    /** Constructor. All defaults should be set here or in field declarations. */
    public PlannerRequest() {
        mode = new TraverseModeSet("TRANSIT,WALK");
    }

    /** 
     * Ideally this should block setting any field named 'fromPlace' from a query param
     * of the same name, and this method's parameters should be filled in from
     * query parameters of the same name. 
     */
    public void setFromPlace(String from) {
        if (from.contains("::")) {
            String[] parts = from.split("::");
            this.fromName = parts[0];
            this.from = parts[1];
        } else {
            this.from = from;
        }
    }

    /** TODO: documentation */
    public void setToPlace(String to) {
        if (to.contains("::")) {
            String[] parts = to.split("::");
            this.toName = parts[0];
            this.to = parts[1];
        } else {
            this.to = to;
        }
    }

}