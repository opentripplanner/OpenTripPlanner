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

import java.util.HashMap;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.error.PlannerError;

/**
 *
 */
@XmlRootElement
public class Response {

    /**
     * A dictionary of the parameters provided in the request that triggered this response.
     */
    public HashMap<String, String> requestParameters;
    /**
     * The actual trip plan.
     */
    public TripPlan plan;
    /**
     * The error (if any) that this response raised.
     */
    @XmlElement(required=false)
    public PlannerError error = null;
    
    public Response() {
    }

    public Response(Request req, TripPlan plan) {
        requestParameters = req.getParameters();
        this.plan = plan;
    }
}