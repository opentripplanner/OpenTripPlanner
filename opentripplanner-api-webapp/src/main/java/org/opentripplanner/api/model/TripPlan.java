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
import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlElementWrapper;

/**
 *
 */
public class TripPlan {

    public Date date = null;
    public Place from = null;
    public Place to = null;
    
    public TripPlan() {}
    
    public TripPlan(Place from, Place to, Date date) {
        this.from = from;
        this.to = to;
        this.date = date;
    }
    
    @XmlElementWrapper(name="itineraries")
    public List<Itinerary> itinerary = new ArrayList<Itinerary>();

    public void addItinerary(Itinerary itinerary) {
        this.itinerary.add(itinerary);
    }
}
