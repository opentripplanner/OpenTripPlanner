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

otp.namespace("otp.modules.planner");

otp.modules.planner.TripPlan = otp.Class({

    planData      : null,
    queryParams   : null,
    
    itineraries   : null,
    
    earliestStartTime : null,
    latestEndTime : null,
    
    initialize : function(planData, queryParams) {
        this.planData = planData;
        this.queryParams = queryParams;
        this.itineraries = [ ];
        
        if(!planData) return;
        for(var i=0; i<this.planData.itineraries.length; i++) {
            var itinData = this.planData.itineraries[i];
            this.itineraries.push(new otp.modules.planner.Itinerary(itinData, this));
        }
        this.calculateTimeBounds();  
    },
    
    addItinerary : function(itin) {
        this.itineraries.push(itin);
        itin.tripPlan = this;
        this.calculateTimeBounds();
        //console.log("added itin, n="+this.itineraries.length);
    },
    
    replaceItinerary : function(index, itin) {
        this.itineraries[index] = itin;
        itin.tripPlan = this;
        this.calculateTimeBounds();
    },
    
    calculateTimeBounds : function() {
        this.earliestStartTime = this.latestEndTime = null;
        for(var i=0; i<this.itineraries.length; i++) {
            var itin = this.itineraries[i];
            this.earliestStartTime = (this.earliestStartTime == null || 
                                      itin.getStartTime() < this.earliestStartTime) ?
                                         itin.getStartTime() : this.earliestStartTime; 
            this.latestEndTime = (this.latestEndTime == null || 
                                  itin.getEndTime() > this.latestEndTime) ?
                                    itin.getEndTime() : this.latestEndTime; 
        }
        //console.log("earliest start: "+otp.util.Time.formatItinTime(this.earliestStartTime));
        //console.log("latest end: "+otp.util.Time.formatItinTime(this.latestEndTime));
    },
    
});
