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
    
    
    // TODO : use version in TransitIndex.js instead
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
    
    getStartTime : function() {
        return this.itinData.legs[0].startTime;
    },
    
    getEndTime : function() {
        return this.itinData.legs[this.itinData.legs.length-1].endTime;
    },

    getStartTimeStr : function() {
        return otp.util.Time.formatItinTime(this.getStartTime());
    },
    
    getEndTimeStr : function() {
        return otp.util.Time.formatItinTime(this.getEndTime());
    },

    getStartLocationStr : function() {
        var from = this.itinData.legs[0].from;
        return from.name || "(" + from.lat.toFixed(5) + ", " + from.lon.toFixed(5) +  ")";
    },
    
    getEndLocationStr : function() {
        var to = this.itinData.legs[this.itinData.legs.length-1].to;
        return to.name || "(" + to.lat.toFixed(5) + ", " + to.lon.toFixed(5)+  ")";
    },
    
    getDurationStr : function() {
        return otp.util.Time.msToHrMin(this.getEndTime() - this.getStartTime());
    },
    
    getFareStr : function() {
    
        if(this.itinData.fare && this.itinData.fare.fare.regular) {
            var decimalPlaces = this.itinData.fare.fare.regular.currency.defaultFractionDigits;
            return this.itinData.fare.fare.regular.currency.symbol +
                (this.itinData.fare.fare.regular.cents/Math.pow(10,decimalPlaces)).toFixed(decimalPlaces);
        }
        return "N/A";
    },
    
    differentServiceDayFrom : function(itin, offsetHrs) {
        offsetHrs = offsetHrs || 4; // default to 4 hrs; i.e. use 4am as breakpoint between days
        var time1 = moment(this.itinData.startTime).add("hours", otp.config.timeOffset-offsetHrs).format('D');
        var time2 = moment(itin.itinData.startTime).add("hours", otp.config.timeOffset-offsetHrs).format('D');
        return time1 !== time2;
    },
    
    /*getTransitSegments : function() {
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
    },*/

    getTransitLegs : function() {
        var legs = [];
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                legs.push(leg);
            } 
        }
        return legs;
    },

    getModeDistance : function(mode) {
        var distance = 0;
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];
            if(leg.mode === mode) {
                distance += leg.distance;
            } 
        }
        return distance;
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
    },
    
    getHtmlNarrative : function() {
        var html = "";
        html += '<link rel="stylesheet" href="js/otp/modules/planner/planner-style.css" />';
        html += '<div class="otp-itin-printWindow">';
        
        html += '<h3>Start: '+this.getStartLocationStr()+' at '+this.getStartTimeStr()+'</h3>';
        
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];

            // header
            html += '<h4>'+(l+1)+'. '+otp.util.Itin.modeString(leg.mode).toUpperCase();//
            if(otp.util.Itin.isTransit(leg.mode)) {
                html += ': ';
                if(leg.route !== leg.routeLongName) html += "("+leg.route+") ";
                html += leg.routeLongName;
                if(leg.headsign) html +=  " toward " + leg.headsign;
            }
            else { // walk / bike / car
                html += " "+otp.util.Itin.distanceString(leg.distance)+ " to "+leg.to.name;
            }
            html += '</h4>'
            
            // main content
            if(otp.util.Itin.isTransit(leg.mode)) { // transit
                html += '<ul>';
                html += '<li><b>Board</b>: ' + leg.from.name + ' (' + leg.from.stopId.agencyId + ' stop ' + 
                        leg.from.stopId.id + '), ' + otp.util.Time.formatItinTime(leg.startTime, "h:mma") + '</li>';
                html += '<li><i>Time in transit: '+otp.util.Time.msToHrMin(leg.duration)+'</i></li>';
                html += '<li><b>Alight</b>: ' + leg.to.name + ' (' + leg.to.stopId.agencyId + ' stop ' + 
                        leg.to.stopId.id + '), ' + otp.util.Time.formatItinTime(leg.endTime, "h:mma") + '</li>';
                
                html += '</ul>';
            }
            else if (leg.steps) { // walk / bike / car
            
                for(var i=0; i<leg.steps.length; i++) {
                    var step = leg.steps[i];
                    var text = otp.util.Itin.getLegStepText(step);
                    
                    html += '<div class="otp-itin-print-step" style="margin-top: .5em;">';
                    html += '<div class="otp-itin-step-icon">';
                    if(step.relativeDirection)
                        html += '<img src="images/directions/' +
                            step.relativeDirection.toLowerCase()+'.png">';
                    html += '</div>';                
                    var dist = otp.util.Itin.distanceString(step.distance);
                    //html += '<div class="otp-itin-step-dist">' +
                    //    '<span style="font-weight:bold; font-size: 1.2em;">' + 
                    //    distArr[0]+'</span><br>'+distArr[1]+'</div>';
                    html += '<div class="otp-itin-step-text">'+text+'<br>'+dist+'</div>';
                    html += '<div style="clear:both;"></div></div>';
                }            
            }
            

        }
        
        html += '<h3>End: '+this.getEndLocationStr()+' at '+this.getEndTimeStr()+'</h3>';

        // trip summary
        html += '<div class="otp-itinTripSummary" style="font-size: .9em">';
        html += '<div class="otp-itinTripSummaryHeader">Trip Summary</div>';
        html += '<div class="otp-itinTripSummaryLabel">Travel</div><div class="otp-itinTripSummaryText">'+this.getStartTimeStr()+'</div>';
        html += '<div class="otp-itinTripSummaryLabel">Time</div><div class="otp-itinTripSummaryText">'+this.getDurationStr()+'</div>';
        if(this.hasTransit) {
            html += '<div class="otp-itinTripSummaryLabel">Transfers</div><div class="otp-itinTripSummaryText">'+this.itinData.transfers+'</div>';
            if(this.itinData.walkDistance > 0) {
                html += '<div class="otp-itinTripSummaryLabel">Total Walk</div><div class="otp-itinTripSummaryText">' + 
                    otp.util.Itin.distanceString(this.itinData.walkDistance) + '</div>';
            }
            html += '<div class="otp-itinTripSummaryLabel">Fare</div><div class="otp-itinTripSummaryText">'+this.getFareStr()+'</div>';
        }
        html += '</div>';

        html += '</div>';
        
        return html;
    },
    
    getTextNarrative : function(itinLink) {
        var text = ''
        text += 'Start: '+this.getStartLocationStr()+' at '+this.getStartTimeStr()+'\n\n';
        
        for(var l=0; l<this.itinData.legs.length; l++) {
            var leg = this.itinData.legs[l];

            // header
            text += (l+1)+'. '+otp.util.Itin.modeString(leg.mode).toUpperCase();
            if(otp.util.Itin.isTransit(leg.mode)) {
                text += ': ';
                if(leg.route !== leg.routeLongName) text += "("+leg.route+") ";
                text += leg.routeLongName;
                if(leg.headsign) text +=  " toward " + leg.headsign;
            }
            else { // walk / bike / car
                text += ' '+ otp.util.Itin.distanceString(leg.distance)+ " to "+leg.to.name;
            }
            text += '\n';
            
            // content
            if(otp.util.Itin.isTransit(leg.mode)) {
                text += ' - Board: ' + leg.from.name + ' (' + leg.from.stopId.agencyId + ' stop ' + 
                        leg.from.stopId.id + '), ' + otp.util.Time.formatItinTime(leg.startTime, "h:mma") + '\n';
                text += ' - Time in transit: '+otp.util.Time.msToHrMin(leg.duration) + '\n';
                text += ' - Alight: ' + leg.to.name + ' (' + leg.to.stopId.agencyId + ' stop ' + 
                        leg.to.stopId.id + '), ' + otp.util.Time.formatItinTime(leg.endTime, "h:mma") + '\n';
            }
            else if (leg.steps) { // walk / bike / car
            
                for(var i=0; i<leg.steps.length; i++) {
                    var step = leg.steps[i];
                    var desc = otp.util.Itin.getLegStepText(step, false);                    
                    var dist = otp.util.Itin.distanceString(step.distance);
                    text += ' - ' + desc + ' ('+ dist + ')\n';

                }            
            }
            text += '\n';
            
        }
        
        text += 'End: '+this.getEndLocationStr()+' at '+this.getEndTimeStr()+'\n';
        
        if(itinLink) {
            text += '\nView itinerary online:\n' + itinLink + '\n';
        }
        return text;
    }
    
});
