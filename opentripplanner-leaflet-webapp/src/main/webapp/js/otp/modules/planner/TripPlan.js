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
    
    initialize : function(planData, queryParams) {
        this.planData = planData;
        this.queryParams = queryParams;
        this.itineraries = [ ];
        
        for(var i=0; i<this.planData.itineraries.length; i++) {
            var itinData = this.planData.itineraries[i];
            this.itineraries.push(new otp.modules.planner.Itinerary(itinData, this));
        }
    }
         
});
