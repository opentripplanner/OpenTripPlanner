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
otp.Class({
    /**
     * Isochrone constructor.
     * 
     * @param parameters
     *            Request parameters
     * @param cutoffSec
     *            Cutoff time in seconds (array of values or single value).
     */
    initialize : function(parameters, cutoffSec) {
        if (!(cutoffSec instanceof Array)) {
            cutoffSec = [ cutoffSec ];
        }
        this.isochrones = null;
        var url = '/otp-rest-servlet/ws/isochrone?' + $.param(parameters, false);
        for (var i = 0; i < cutoffSec.length; i++)
            url += "&cutoffSec=" + cutoffSec[i];
        this.isoMap = [];
        var thisIso = this;
        var ajaxParams = {
            url : url,
            success : function(result) {
                // Index features on cutoff time
                for (var i = 0; i < result.features.length; i++) {
                    var time = result.features[i].properties.Time;
                    thisIso.isoMap[time] = result.features[i];
                }
            },
            async : false
        };
        jQuery.ajax(ajaxParams);
    },

    /**
     * Get the GeoJson feature.
     * 
     * @param cutoffSec
     *            The iso time to request the GeoJSON feature from.
     */
    getFeature : function(cutoffSec) {
        return this.isoMap[cutoffSec];
    }
});