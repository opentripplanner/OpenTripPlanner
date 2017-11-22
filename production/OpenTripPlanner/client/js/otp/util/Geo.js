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
    },
    
    truncatedLatLng : function(latLngStr) {
        var ll = otp.util.Geo.stringToLatLng(latLngStr);
        return Math.round(ll.lat*100000)/100000+","+Math.round(ll.lng*100000)/100000;
    },

	decodePolyline : function(polyline) {
		
		  var currentPosition = 0;

		  var currentLat = 0;
		  var currentLng = 0;
	
		  var dataLength  = polyline.length;
		  
		  var polylineLatLngs = new Array();
		  
		  while (currentPosition < dataLength) {
			  
			  var shift = 0;
			  var result = 0;
			  
			  var byte;
			  
			  do {
				  byte = polyline.charCodeAt(currentPosition++) - 63;
				  result |= (byte & 0x1f) << shift;
				  shift += 5;
			  } while (byte >= 0x20);
			  
			  var deltaLat = ((result & 1) ? ~(result >> 1) : (result >> 1));
			  currentLat += deltaLat;
	
			  shift = 0;
			  result = 0;
			
			  do {
				  byte = polyline.charCodeAt(currentPosition++) - 63;
				  result |= (byte & 0x1f) << shift;
				  shift += 5;
			  } while (byte >= 0x20);
			  
			  var deltLng = ((result & 1) ? ~(result >> 1) : (result >> 1));
			  
			  currentLng += deltLng;
	
			  polylineLatLngs.push(new L.LatLng(currentLat * 0.00001, currentLng * 0.00001));
		  }	
		  
		  return polylineLatLngs;
	},

	distanceString : function (m) {
		if (otp.config.metric) {
			return this.distanceStringMetric(m);
		} else {
			return this.distanceStringImperial(m);
		}
	},

	distanceStringImperial : function (m) {
		var ft = m*3.28084;
        if(ft < 528) return Math.round(ft) + ' feet';
        return Math.round(ft/52.8)/100+" miles";
	},

	distanceStringMetric : function (m) {
		km = m/1000;
        if ( km > 100 ) {
            //100 km => 999999999 km
            km = km.toFixed(0);
            return km+" km";
        } else if ( km > 1 ) {
            //1.1 km => 99.9 km
            km = km.toFixed(1);
            return km+" km";
        } else {
            //1m => 999m
            m = m.toFixed(0);
            return m+" m";
        }
	}
};
