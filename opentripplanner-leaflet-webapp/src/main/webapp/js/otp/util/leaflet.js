// doesn't appear to work in Leaftet 0.4. Replaces w/ otp.util.Polyline

L.EncodedPolyline = L.Polyline.extend ({
	
	initialize: function (polyline, options) {
		L.Polyline.prototype.initialize.call(this, options);
	
		this._latlngs = this._decode(polyline);
	},
	
	setPolyline: function (polyline) {
	
		this.setLatLngs(this._decode(polyline));
	},
	
	_decode: function (polyline) {
		
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
	}
	
});
