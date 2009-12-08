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
public class Itinerary {

    public long duration = 0;
    public Date startTime = null;
    public Date endTime = null;

    public Double walkTime = 0.0;
    public Double transitTime = 0.0;
    public Double waitingTime = 0.0;
    
    public Double walkDistance = 0.0;

    public Double elevationLost = 0.0;
    public Double elevationGained = 0.0;

    public Integer transfers = 0;

    public Fare fare = new Fare();

    @XmlElementWrapper(name = "legs")
    public List<Leg> leg = new ArrayList<Leg>();

    public void addLeg(Leg leg) {
        this.leg.add(leg);
    }
}
