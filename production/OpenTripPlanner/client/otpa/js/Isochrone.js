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

otp.namespace("otp.analyst");

/**
 * Isochrone class.
 */
otp.analyst.Isochrone = otp.Class({
    /**
     * Isochrone constructor.
     * 
     * @param parameters
     *            Request parameters
     * @param cutoffSec
     *            Cutoff time in seconds (array of values or single value).
     * @param options
     *            Optional parameters
     */
    initialize : function(parameters, cutoffSec, options) {
        if (!(cutoffSec instanceof Array)) {
            cutoffSec = [ cutoffSec ];
        }
        this.options = $.extend({
            load : true,
            async : true
        }, options);
        this.isochrones = null;
        this.onLoadCallbacks = $.Callbacks();
        var routerId = parameters.routerId;
        if (!routerId || 0 === routerId.length)
            routerId = 'default';
        this.url = '/otp/routers/' + routerId + '/isochrone?' + $.param(parameters, false);
        for (var i = 0; i < cutoffSec.length; i++)
            this.url += "&cutoffSec=" + cutoffSec[i];
        this.isoMap = [];
        var thisIso = this;
        if (this.options.load) {
            var ajaxParams = {
                dataType : 'json',
                accepts : {
                    json : 'application/json'
                },
                success : function(result) {
                    // Index features on cutoff time
                    for (var i = 0; i < result.features.length; i++) {
                        // Depending on JSON lib it's either time or Time
                        var time = result.features[i].properties.Time;
                        if (!time)
                            time = result.features[i].properties.time;
                        thisIso.isoMap[time] = result.features[i];
                    }
                    thisIso.onLoadCallbacks.fire(thisIso);
                },
                async : this.options.async
            };
            jQuery.ajax(this.url, ajaxParams);
        }
    },

    /**
     * Get the GeoJson feature.
     * 
     * @param cutoffSec
     *            The iso time to request the GeoJSON feature from.
     */
    getFeature : function(cutoffSec) {
        return this.isoMap[cutoffSec];
    },

    /**
     * Get the request URL.
     */
    getUrl : function() {
        return this.url;
    },

    /**
     * Add a callback when loaded.
     */
    onLoad : function(callback) {
        this.onLoadCallbacks.add(callback);
        return this;
    },

});