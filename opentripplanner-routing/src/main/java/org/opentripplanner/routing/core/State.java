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

import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.FareAttribute;
import org.opentripplanner.routing.core.Fare.FareType;

public class State {

    private long _time;
    private int pattern = -1;
    public AgencyAndId tripId = null;
    public double walkDistance = 0;

    public boolean justTransferred = false;

    private List<String> zonesVisited;
    
    private List<AgencyAndId> routesVisited;
    private FareContext fareContext;

    public State() {
        this(System.currentTimeMillis());
    }

    public State(long time) {
        _time = time;
        zonesVisited = new ArrayList<String>();
        routesVisited = new ArrayList<AgencyAndId>();
    }    

    public State(long time, int pattern, AgencyAndId tripId, double walkDistance) {
        this(time,pattern,tripId,walkDistance, new ArrayList<String>(), new ArrayList<AgencyAndId>(), null);
    }

    public State(long time, int pattern, AgencyAndId tripId, double walkDistance,
            List<String> zonesVisited, List<AgencyAndId> routesVisited, 
            FareContext fareContext) {
        _time = time;
        this.pattern = pattern;
        this.tripId = tripId;
        this.walkDistance = walkDistance;
        this.zonesVisited = zonesVisited;
        this.routesVisited = routesVisited;
        this.fareContext = fareContext;
    }

    public void addZone(String zone, FareContext context) {
        if (zone == null) {
            return;
        }
        if (zonesVisited.size() > 0 && zonesVisited.get(zonesVisited.size() - 1).equals(zone)) {
            return;
        }
        //copy on write
        zonesVisited = new ArrayList<String>(zonesVisited);
        zonesVisited.add(zone);
        fareContext = context;
    }
    
    public void addRoute(AgencyAndId route, FareContext fareContext) {
        if (routesVisited.size() > 0 && routesVisited.get(routesVisited.size() - 1).equals(route)) {
            return;
        }
        //copy on write
        routesVisited = new ArrayList<AgencyAndId>(routesVisited);
        routesVisited.add(route);
        this.fareContext = fareContext;
    }
    
    public List<String> getZonesVisited() {
        return zonesVisited;
    }

    public Fare getCost() {
        if (fareContext == null) {
            //we have never actually visited any zones, so there's no fare data.
            //perhaps we're planning a biking-only trip.
            return null;
        }
        float bestFare = Float.MAX_VALUE; 
        Currency currency = null;
        HashMap<AgencyAndId, FareRuleSet> fareRules = fareContext.getFareRules();
        HashMap<AgencyAndId, FareAttribute> fareAttributes = fareContext.getFareAttributes();
        for (AgencyAndId fareId : fareRules.keySet()) {
            FareRuleSet ruleSet = fareRules.get(fareId);
            if (ruleSet.matches(zonesVisited, routesVisited)) {
                FareAttribute attribute = fareAttributes.get(fareId);
                float newFare = attribute.getPrice();
                if (newFare < bestFare) {
                    bestFare = newFare;
                    currency = Currency.getInstance(attribute.getCurrencyType());
                }
            }
        }
        if (bestFare == Float.MAX_VALUE) {
            return null;
        }
        Fare fare = new Fare();
        fare.addFare(FareType.regular, new WrappedCurrency(currency), (int) (bestFare * Math.pow(10, currency.getDefaultFractionDigits())));
        return fare;
    }

    public long getTime() {
        return _time;
    }

    public void incrementTimeInSeconds(int numOfSeconds) {
        _time += numOfSeconds * 1000;
    }

    public State clone() {
        State ret = new State(_time, pattern, tripId, walkDistance, zonesVisited, routesVisited, fareContext);
        return ret;
    }

    public String toString() {
        return "<State " + new Date(_time) + "," + pattern + ">";
    }

    public void setPattern(int curPattern) {
        this.pattern = curPattern;
    }

    public int getPattern() {
        return pattern;
    }


}