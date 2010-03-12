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

package org.opentripplanner.routing.core;

import java.io.Serializable;
import java.util.HashMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;

public class FareContext implements Serializable {

    private static final long serialVersionUID = 2573564440938824416L;

    private HashMap<AgencyAndId, FareAttribute> fareAttributes;
    private HashMap<AgencyAndId, FareRuleSet> fareRules;

    public FareContext(HashMap<AgencyAndId, FareRuleSet> fareRules,
            HashMap<AgencyAndId, FareAttribute> fareAttributes) {
       this.setFareRules(fareRules);
       this.setFareAttributes(fareAttributes);
    }

    public void setFareAttributes(HashMap<AgencyAndId, FareAttribute> fareAttributes) {
        this.fareAttributes = fareAttributes;
    }

    public HashMap<AgencyAndId, FareAttribute> getFareAttributes() {
        return fareAttributes;
    }

    public void setFareRules(HashMap<AgencyAndId, FareRuleSet> fareRules) {
        this.fareRules = fareRules;
    }

    public HashMap<AgencyAndId, FareRuleSet> getFareRules() {
        return fareRules;
    }
    
}

