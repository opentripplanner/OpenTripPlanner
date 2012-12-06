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

otp.modules.planner.Itinerary = otp.Class({

    itinData      : null,
    tripPlan      : null,
    
    initialize : function(itinData, tripPlan) {
        this.itinData = itinData;
        this.tripPlan = tripPlan;
    },

    getFirstStop : function() {
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                return leg.from.stopId.agencyId+"_"+leg.from.stopId.id;
            }
        }
        return null;
    },
    
    isTransit : function(mode) {
        return otp.util.itin.isTransit(this.itinData.mode);
    },
    
    getIconSummaryHTML : function(padding) {
        var html = '';
        for(var i=0; i<this.itinData.legs.length; i++) {
            html += '<img src="images/mode/'+this.itinData.legs[i].mode.toLowerCase()+'.png" >';
            if(i < this.itinData.legs.length-1)
                html += '<img src="images/mode/arrow.png" style="margin: 0px '+(padding || '3')+'px;">';
        }
        return html;
    },
    
    getStartTimeStr : function() {
        return otp.util.Time.formatItinTime(this.itinData.startTime);
    },
    
    getEndTimeStr : function() {
        return otp.util.Time.formatItinTime(this.itinData.endTime);
    },
    
    getDurationStr : function() {
        return otp.util.Time.msToHrMin(this.itinData.duration);
    },
    
});
