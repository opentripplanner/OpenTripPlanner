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

    hasTransit  : false,
    totalWalk : 0,
        
    initialize : function(itinData, tripPlan) {
        this.itinData = itinData;
        this.tripPlan = tripPlan;
        
        this.stopTimesMap = { };
        
        this.firstStopIDs = [ ];
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                this.hasTransit = true;
                this.firstStopIDs.push(leg.from.stopId);
                this.runStopTimesQuery(leg.from.stopId, leg.routeId, leg.startTime);
            }
            if(leg.mode === "WALK") this.totalWalk += leg.distance;
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
                    var st = data.stopTimes[i].StopTime || data.stopTimes[i];
                    if(st.phase == 'departure')
                        stopTimes.push(st.time*1000);
                }
                this_.stopTimesMap[stopId.id] = stopTimes;
            }
        });
    },

    getFirstStopID : function() {
        if(this.firstStopIDs.length == 0) return null;
        //console.log(this.firstStopIDs[0].agencyId+"_"+this.firstStopIDs[0].id);
        return this.firstStopIDs[0].agencyId+"_"+this.firstStopIDs[0].id;
    },

    getIconSummaryHTML : function(padding) {
        var html = '';
        for(var i=0; i<this.itinData.legs.length; i++) {
            var exceedsWalk = (this.itinData.legs[i].mode == "WALK" && this.itinData.legs[i].distance > this.tripPlan.queryParams.maxWalkDistance);            
            html += '<img src="images/mode/'+this.itinData.legs[i].mode.toLowerCase()+'.png" '+(exceedsWalk ? 'style="background:#f88; padding: 0px 2px;"' : "")+'>';
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
    
    getFareStr : function() {
    
        if(this.itinData.fare && this.itinData.fare.fare.regular) {
            var decimalPlaces = this.itinData.fare.fare.regular.currency.defaultFractionDigits;
            return this.itinData.fare.fare.regular.currency.symbol +
                (this.itinData.fare.fare.regular.cents/Math.pow(10,decimalPlaces)).toFixed(decimalPlaces);
        }
        return "N/A";
    },
    
    getLink : function(itinIndex) {
        var appendChar = "?", paramStr = "";
        for(param in this.tripPlan.queryParams) {
            paramStr += appendChar+param+"="+ encodeURIComponent(this.tripPlan.queryParams[param]);
            appendChar = "&";
        }
        if(itinIndex) paramStr += appendChar+"itinIndex="+itinIndex;
        return paramStr;
    },
    
    differentServiceDayFrom : function(itin, offsetHrs) {
        offsetHrs = offsetHrs || 4; // default to 4 hrs; i.e. use 4am as breakpoint between days
        var time1 = moment(this.itinData.startTime).add("hours", otp.config.timeOffset-offsetHrs).format('D');
        var time2 = moment(itin.itinData.startTime).add("hours", otp.config.timeOffset-offsetHrs).format('D');
        return time1 !== time2;
    },
    
    getTransitSegments : function() {
        var segments = [];
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                var stopIndices = [];
                if(leg.from.stopIndex !== null) {
                    stopIndices.push(leg.from.stopIndex);
                }
                if(leg.intermediateStops) {
                    for(var s = 0; s < leg.intermediateStops.length; s++) {
                        if(s == 0 && leg.from.stopIndex == null) { // temp workaround for apparent backend bug
                            stopIndices.push(leg.intermediateStops[s].stopIndex-1);
                        }
                        stopIndices.push(leg.intermediateStops[s].stopIndex);
                    }
                }
                stopIndices.push(leg.to.stopIndex);
                
                var segment = {
                    leg : leg,
                    stopIndices : stopIndices,
                    tripString : leg.agencyId + "_" + leg.tripId + ":" + stopIndices.join(':')
                }
                segments.push(segment);
            } 
        }
        return segments;
    },
    
    /*getTripSegments : function() {
        var segments = [];
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                var tripString = leg.agencyId + "_"+leg.tripId + ":";
                if(leg.from.stopIndex !== null) tripString += leg.from.stopIndex + ":";
                if(leg.intermediateStops) {
                    for(var s = 0; s < leg.intermediateStops.length; s++) {
                        if(s == 0 && leg.from.stopIndex == null) { // temp workaround for apparent backend bug
                            tripString += (leg.intermediateStops[s].stopIndex-1) + ":";
                        }
                        tripString += leg.intermediateStops[s].stopIndex+':';
                    }
                }
                tripString += leg.to.stopIndex;
                //console.log("leg "+l+": "+tripString);
                segments.push(tripString);
            } 
        }
        return segments;
    },*/
        
    getGroupTripCapacity : function() {
        var capacity = 100000;
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                capacity = Math.min(capacity, this.getModeCapacity(leg.mode));
            }
        }
        return capacity;
    },
    
    getModeCapacity : function(mode) {
        if(mode === "SUBWAY" || mode === "TRAM") return 80;
        if(mode === "BUS") return 40;
        return 0;
    },
    
    
    /* returns [[south, west], [north, east]] */    
    
    getBoundsArray : function() {
        var start = this.itinData.legs[0].from;
        var end = this.itinData.legs[this.itinData.legs.length-1].to;
        return [[Math.min(start.lat, end.lat), Math.min(start.lon, end.lon)],
                [Math.max(start.lat, end.lat), Math.max(start.lon, end.lon)]];
    }
    
});
