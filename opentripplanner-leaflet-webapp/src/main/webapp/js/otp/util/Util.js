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

otp.namespace("otp.util");

/**
 * Utility routines for geospatial operations
 */
 
otp.util.Geo = {

    stringToLatLng : function(str) {
        var arr = str.split(',');
        return new L.LatLng(parseFloat(arr[0]), parseFloat(arr[1]));
    }

};

otp.util.Itin = {

    getFirstStop : function(itin) {
        for(var l=0; l<itin.legs.length; l++) {
            var leg = itin.legs[l];
            if(otp.util.Itin.isTransit(leg.mode)) {
                return leg.from.stopId.agencyId+"_"+leg.from.stopId.id;
            }
        }
        return null;
    },
    
    isTransit : function(mode) {
        return mode === "TRANSIT" || mode === "SUBWAY" || mode === "BUS" || mode === "TRAM" || mode === "GONDOLA";
    },
    
    getIconSummaryHTML : function(itin, padding) {
        var html = '';
        for(var i=0; i<itin.legs.length; i++) {
            html += '<img src="images/mode/'+itin.legs[i].mode.toLowerCase()+'.png" >';
            if(i < itin.legs.length-1) html += '<img src="images/mode/arrow.png" style="margin: 0px '+(padding || '3')+'px;">';
        }
        return html;
    }   
    
}
