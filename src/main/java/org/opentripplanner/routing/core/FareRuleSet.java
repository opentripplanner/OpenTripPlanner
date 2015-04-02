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
import java.util.HashSet;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.common.model.P2;

public class FareRuleSet implements Serializable {

    private static final long serialVersionUID = 7218355718876553028L;

    private String agency = null;
    private Set<AgencyAndId> routes;
    private Set<P2<String>> originDestinations;
    private Set<String> contains;
    private FareAttribute fareAttribute;
    
    public FareRuleSet(FareAttribute fareAttribute) {
        this.fareAttribute = fareAttribute;
        routes = new HashSet<AgencyAndId>();
        originDestinations= new HashSet<P2<String>>();
        contains = new HashSet<String>();
    }

    public void setAgency(String agency) {
        // TODO With new GTFS lib, read value from fareAttribute directly?
        this.agency = agency;
    }

    public void addOriginDestination(String origin, String destination) {
        originDestinations.add(new P2<String>(origin, destination));
    }

    public Set<P2<String>> getOriginDestinations() {
        return originDestinations;
    }

    public void addContains(String containsId) {
        contains.add(containsId);
    }
    
    public void addRoute(AgencyAndId route) {
        routes.add(route);
    }

    public FareAttribute getFareAttribute() {
        return fareAttribute;
    }

    public boolean hasAgencyDefined() {
        return agency != null;
    }

    public boolean matches(Set<String> agencies, String startZone, String endZone, Set<String> zonesVisited,
            Set<AgencyAndId> routesVisited) {
        //check for matching agency
        if (agency != null) {
            if (agencies.size() != 1 || !agencies.contains(agency))
                return false;
        }

        //check for matching origin/destination, if this ruleset has any origin/destination restrictions
        if (originDestinations.size() > 0) {
            P2<String> od = new P2<String>(startZone, endZone);
            if (!originDestinations.contains(od)) {
                P2<String> od2 = new P2<String>(od.first, null);
                if (!originDestinations.contains(od2)) {
                    od2 = new P2<String>(null, od.first);
                    if (!originDestinations.contains(od2)) {
                        return false;
                    }
                }
            }
        }

        //check for matching contains, if this ruleset has any containment restrictions
        if (contains.size() > 0) {
            if (!zonesVisited.equals(contains)) {
                return false;
            }
        }

        //check for matching routes
        if (routes.size() != 0) {
            if (!routes.containsAll(routesVisited)) {
                return false;
            }
        }

        return true;
    }
}

