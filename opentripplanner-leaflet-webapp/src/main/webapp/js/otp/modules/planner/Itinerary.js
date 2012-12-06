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
    
    firstStopIDs    : null,
    stopTimesMap     : null,
    
    initialize : function(itinData, tripPlan) {
        this.itinData = itinData;
        this.tripPlan = tripPlan;
        
        this.stopTimesMap = { };
        
        this.firstStopIDs = [ ];
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                this.firstStopIDs.push(leg.from.stopId);
                this.runStopTimesQuery(leg.from.stopId, leg.routeId, leg.startTime);
            }
        }
    },
    
    runStopTimesQuery : function(stopId, routeId, time) {
        var this_ = this;
        var hrs = 4;
        var params = {
            agency: stopId.agencyId,
            id: stopId.id,
            startTime : time-hrs*3600000,
            endTime : time+hrs*3600000
        };
        if(otp.config.routerId !== undefined) {
            params.routerId = otp.config.routerId;
        }
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/stopTimesForStop';
        $.ajax(url, {
            data:       params,
            dataType:   'jsonp',
                
            success: function(data) {
                var stopTimes = [];
                for(var i=0; i<data.stopTimes.length; i++) {
                    if(data.stopTimes[i].phase == 'departure')
                        stopTimes.push(data.stopTimes[i].time*1000);
                }
                this_.stopTimesMap[stopId.id] = stopTimes;
            }
        });
    },

    getFirstStopID : function() {
        if(this.firstStopIDs.length == 0) return null;
        console.log(this.firstStopIDs[0].agencyId+"_"+this.firstStopIDs[0].id);
        return this.firstStopIDs[0].agencyId+"_"+this.firstStopIDs[0].id;
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
