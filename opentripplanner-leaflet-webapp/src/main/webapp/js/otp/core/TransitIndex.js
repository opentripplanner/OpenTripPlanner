/* This program is free software: you can redistribute it and/or
   modify it under the teMap.jsrms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.core");

otp.core.TransitIndex = otp.Class({

    webapp          : null,

    routes  : { },
    
    initialize : function(webapp) {
        var this_ = this;
        this.webapp = webapp;
        
        
        // load route data from server
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routes';
        $.ajax(url, {
            dataType:   'jsonp',
                
            success: function(data) {
                for(var i=0; i<data.routes.length; i++) {
                    var routeData = data.routes[i];
                    var agency_id = routeData.id.agencyId+"_"+routeData.id.id;
                    this_.routes[agency_id] = {
                        index : i,
                        routeData : routeData,
                        variants : null
                    };
                }
            }
        });        
    },
    
    loadVariants : function(agency_id, callbackTarget, callback) {
        var this_ = this;
        //console.log("loadVariants: "+agency_id);
        var route = this.routes[agency_id];

        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routeData';
        $.ajax(url, {
            data: {
                agency : route.routeData.id.agencyId,
                id : route.routeData.id.id
                
            },
            dataType:   'jsonp',
                
            success: function(data) {
                //console.log(data);
                route.variants = {};
                for(var i=0; i<data.routeData[0].variants.length; i++) {
                    route.variants[data.routeData[0].variants[i].name] = data.routeData[0].variants[i];
                    data.routeData[0].variants[i].index = i;
                }
                if(callback && callbackTarget) {
                    callback.call(callbackTarget);
                }
            }
        });
        
    },
    
    readVariantForTrip : function(tripAgency, tripId, callbackTarget, callback) {
    
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/variantForTrip';
        $.ajax(url, {
            data: {
                tripAgency : tripAgency,
                tripId : tripId
            },
            dataType:   'jsonp',
                
            success: function(data) {
                //console.log("vFT result:");
                //console.log(data);
                callback.call(callbackTarget, data);
            }
        });        

        /*var route = this.routes[agency_id];
        console.log("looking for trip "+tripId+" in "+agency_id);
        
        if(!route.variants) {
            console.log("ERROR: transitIndex.routes.["+agency_id+"].variants null in TransitIndex.getVariantForTrip()");
            return;
        }
        
        for(var vi=0; vi<route.variants.length; vi++) {
            var variant = route.variants[vi];
            console.log("searching variant "+vi);
            //console.log(variant);
            for(var ti=0; ti<variant.trips.length; ti++) {
                var trip = variant.trips[ti];
                console.log(" - "+trip.id)
                if(trip.id == tripId) return variant;
            }
        }
        
        console.log("cound not find trip "+tripId);
        return null;*/
    },

    runStopTimesQuery : function(stopId, routeId, time, callbackTarget, callback) {
        //var this_ = this;
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
                /*var stopTimes = [];
                for(var i=0; i<data.stopTimes.length; i++) {
                    var st = data.stopTimes[i].StopTime || data.stopTimes[i];
                    if(st.phase == 'departure')
                        stopTimes.push(st.time*1000);
                }
                this_.stopTimesMap[stopId.id] = stopTimes;*/
                callback.call(callbackTarget, data);                
            }
        });
    },

    runStopTimesQuery2 : function(agencyId, stopId, startTime, endTime, callbackTarget, callback) {
        //var this_ = this;
        var params = {
            agency: agencyId,
            id: stopId,
            startTime : startTime,
            endTime : endTime
        };
        if(otp.config.routerId !== undefined) {
            params.routerId = otp.config.routerId;
        }
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/stopTimesForStop';
        $.ajax(url, {
            data:       params,
            dataType:   'jsonp',
                
            success: function(data) {
                callback.call(callbackTarget, data);                
            }
        });
    },        
});
