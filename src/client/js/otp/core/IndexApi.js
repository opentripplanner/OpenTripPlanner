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

otp.core.IndexApi = otp.Class({

    webapp          : null,

    agencies        : null,
    routes          : null,

    initialize : function(webapp) {
        this.webapp = webapp;
    },

    loadAgencies : function(callbackTarget, callback) {
        var this_ = this;
        if(this.agencies) {
            if(callback) callback.call(callbackTarget);
            return;
        }

        var url = otp.config.hostname + '/' + otp.config.restService + '/index/agencies';
        $.ajax(url, {
            success: function(data) {
                this_.agencies = {};

                for(var i=0; i<data.length; i++) {
                    var agencyData = data[i];
                    this_.agencies[agencyData.id] = {
                        index : i,
                        agencyData : agencyData,
                    };
                }

                if(callback) callback.call(callbackTarget);
            }
        });
    },

    loadRoutes : function(callbackTarget, callback) {
        var this_ = this;
        if(this.routes) {
            if(callback) callback.call(callbackTarget);
            return;
        }

        var url = otp.config.hostname + '/' + otp.config.restService + '/index/routes';
        $.ajax(url, {
            success: function(data) {
                if(_.isEmpty(data)) {
                    console.log("Error: routes call returned no route data. OTP Message: "+data.message);
                    return;
                }
                var sortedRoutes = data;
                sortedRoutes.sort(function(a,b) {
                    a = a.shortName || a.longName;
                    b = b.shortName || b.longName;
                    if(otp.util.Text.isNumber(a) && otp.util.Text.isNumber(b)) {
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
                    var agencyAndId = routeData.id;
                    routes[agencyAndId] = {
                        index : i,
                        routeData : routeData,
                        variants : null
                    };
                }
                this_.routes = routes;
                if(callback) callback.call(callbackTarget);
            }
        });
    },

    loadVariants : function(agencyAndId, callbackTarget, callback) {
        var this_ = this;
        //console.log("loadVariants: "+agencyAndId);
        var route = this_.routes[agencyAndId];
        if(route.variants) {
            if(callback) callback.call(callbackTarget, route.variants);
            return;
        }

        // load more details about route
        var url = otp.config.hostname + '/' + otp.config.restService + '/index/routes/' + agencyAndId ;
        $.ajax(url, {
            success: function(data){
                // index api does not return the mode yet...
                routeMode = route.routeData.mode;
                route.routeData = data;
                route.routeData.mode = routeMode;
            }

        });

        url += '/patterns';
        $.ajax(url, {
            success: function(data) {
                route.variants = {};
                _.each(data,function(pattern, i) {
                    route.variants[pattern.id] = pattern;
                    this_.loadPattern(agencyAndId, pattern.id);
                    route.variants[pattern.id].index = i;
                    route.variants[pattern.id].route = route.routeData;
                });
                if(callback && callbackTarget) {
                    callback.call(callbackTarget, route.variants);
                }
            }
        });
    },

    loadPattern : function(routeId, patternId) {
        var this_ = this;
        var url = otp.config.hostname + '/' + otp.config.restService + '/index/patterns/' + patternId ;
        $.ajax(url, {
            async: false,
            success: function(data) {
                this_.routes[routeId].variants[patternId]=data;
            }
        });
    },

    readVariantForTrip : function(tripAgency, routeId, tripId, callbackTarget, callback) {
        var this_ = this;
        var agency_routeId = tripAgency + ':' + routeId;
        var agency_Tripid = tripAgency + ':' + tripId;
        var route = this_.routes[agency_routeId];
        var variantData = {};
        // since the new index api does not provide variant/pattern for trip (yet)
        // we have to iterate on route's patterns searching for the current trip.
        _.each(route.variants, function(pattern) {
            var tripIds = _.pluck(pattern.trips, 'id');
            if (_.contains(tripIds, agency_Tripid)) {
                variantData = pattern;
            }
        });
        //console.log("vFT result:");
        //console.log(variantData);
        callback.call(callbackTarget, variantData);
    },

    //runStopTimesQuery : function(agencyId, stopId, startTime, endTime, callbackTarget, callback) {
    runStopTimesQuery : function( stopId, date, callbackTarget, callback) {
        date = moment(date).format('YYYYMMDD');
        var url = otp.config.hostname + '/' + otp.config.restService + '/index/stops/' + stopId + '/stoptimes/' + date;
        $.ajax(url, {
            //data:       params,

            success: function(data) {
                callback.call(callbackTarget, data);
            }
        });
    },

    loadStopsInRectangle : function(agencyId, bounds, callbackTarget, callback) {
        var params = {
            maxLat : bounds.getNorthWest().lat,
            minLon : bounds.getNorthWest().lng,
            minLat : bounds.getSouthEast().lat,
            maxLon : bounds.getSouthEast().lng,
        };

        var url = otp.config.hostname + '/' + otp.config.restService + '/index/stops';
        $.ajax(url, {
            data:       params,

            success: function(data) {
                callback.call(callbackTarget, data);
            }
        });
    },

    loadRoutesForStop : function(agencyId, callbackTarget, callback) {

        var url = otp.config.hostname + '/' + otp.config.restService + '/index/stops/' + agencyId + '/routes';
        $.ajax(url, {
            success: function(data) {
                callback.call(callbackTarget, data);
            }
        });
    },
/* to be implemented
    loadStopsById : function(agencyId, id, callbackTarget, callback) {
        var params = {
            id : id,
            extended : true
        };
        if(agencyId !== null) {
            params.agency = agencyId;
        }
        if(typeof otp.config.routerId !== 'undefined') {
            params.routerId = otp.config.routerId;
        }

        var url = otp.config.hostname + '/' + otp.config.restService + '/transit/stopData';
        $.ajax(url, {
            data:       params,
            dataType:   'jsonp',

            success: function(data) {
                callback.call(callbackTarget, data);
            }
        });
    },

    loadStopsByName : function(agencyId, name, callbackTarget, callback) {
        var params = {
            name: name,
            extended : true
        };
        if(agencyId !== null) {
            params.agency = agencyId;
        }
        if(typeof otp.config.routerId !== 'undefined') {
            params.routerId = otp.config.routerId;
        }

        var url = otp.config.hostname + '/' + otp.config.restService + '/transit/stopsByName';
        $.ajax(url, {
            data:       params,
            dataType:   'jsonp',

            success: function(data) {
                callback.call(callbackTarget, data);
            }
        });
    },
    */
});
