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

    routes          : null,
    routesLoaded    : false,
    
    initialize : function(webapp) {
        this.webapp = webapp;
    },
    
    loadRoutes : function(callbackTarget, callback) {
        var this_ = this;
        if(this.routesLoaded) {
            if(callback) callback.call(callbackTarget);
            return;
        }
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routes';
        $.ajax(url, {
            dataType:   'jsonp',
                
            success: function(data) {
                if(!_.has(data, 'routes')) {
                    console.log("Error: routes call returned no route data. OTP Message: "+data.message);
                    return;
                }
                var sortedRoutes = data.routes;
                sortedRoutes.sort(function(a,b) {
                    a = a.routeShortName || a.routeLongName;
                    b = b.routeShortName || b.routeLongName;
                    if(otp.util.Text.isNumber(a) && otp.util.Text.isNumber(a)) {
                        if(parseFloat(a) < parseFloat(b)) return -1;
                        if(parseFloat(a) > parseFloat(b)) return 1;
                        return 0;
                    }
                    if(a < b) return -1;
                    if(a > b) return 1;
                    return 0;
                });
                
                var routes = { };
                for(var i=0; i<sortedRoutes.length; i++) {
                    var routeData = sortedRoutes[i];
                    var agencyAndId = routeData.id.agencyId+"_"+routeData.id.id;
                    routes[agencyAndId] = {
                        index : i,
                        routeData : routeData,
                        variants : null
                    };
                }
                this_.routes = routes;
                this_.routesLoaded = true;
                if(callback) callback.call(callbackTarget);
            }            
        });        
    },
    
    loadVariants : function(agencyAndId, callbackTarget, callback) {
        var this_ = this;
        //console.log("loadVariants: "+agencyAndId);
        var route = this.routes[agencyAndId];

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

        /*var route = this.routes[agencyAndId];
        console.log("looking for trip "+tripId+" in "+agencyAndId);
        
        if(!route.variants) {
            console.log("ERROR: transitIndex.routes.["+agencyAndId+"].variants null in TransitIndex.getVariantForTrip()");
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
    
    loadStopsInRectangle : function(agencyId, bounds, callbackTarget, callback) {
        var params = {
            leftUpLat : bounds.getNorthWest().lat,
            leftUpLon : bounds.getNorthWest().lng,
            rightDownLat : bounds.getSouthEast().lat,
            rightDownLon : bounds.getSouthEast().lng,
        };
        if(agencyId !== null) {
            params.agency = agencyId;
        }
        if(typeof otp.config.routerId !== 'undefined') {
            params.routerId = otp.config.routerId;
        }
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/stopsInRectangle';
        $.ajax(url, {
            data:       params,
            dataType:   'jsonp',
                
            success: function(data) {
                callback.call(callbackTarget, data);                
            }
        });
    },
});
