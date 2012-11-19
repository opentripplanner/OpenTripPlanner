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

var INIT_LOCATION = new L.LatLng(38.9538, -76.8851); // new carrolton
var AUTO_CENTER_MAP = true;
var ROUTER_ID = "";
var MSEC_PER_HOUR = 60 * 60 * 1000;
var MSEC_PER_DAY = 86400000;
// var BASE_DATE_MSEC = Date.parse('2012-11-15');
// Note: time zone does not matter since we are turning this back into text before sending it
var BASE_DATE_MSEC = new Date().getTime() - new Date().getTime() % MSEC_PER_DAY; 


var map = new L.Map('map', {
	minZoom : 10,
	maxZoom : 17,
	// what we really need is a fade transition between old and new tiles without removing the old ones
});

var mapboxURL = "http://{s}.tiles.mapbox.com/v3/mapbox.mapbox-light/{z}/{x}/{y}.png";
var OSMURL    = "http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png";
var aerialURL = "http://{s}.mqcdn.com/naip/{z}/{x}/{y}.png";

var mapboxAttrib = "Tiles from <a href='http://mapbox.com/about/maps' target='_blank'>MapBox Streets</a>";
var mapboxLayer = new L.TileLayer(mapboxURL, 
		{subdomains: ["a","b","c","d"], maxZoom: 17, attribution: mapboxAttrib});

var osmAttrib = 'Map data &copy; 2011 OpenStreetMap contributors';
var osmLayer = new L.TileLayer(OSMURL, 
		{subdomains: ["otile1","otile2","otile3","otile4"], maxZoom: 18, attribution: osmAttrib});

var aerialLayer = new L.TileLayer(aerialURL, 
		{subdomains: ["oatile1","oatile2","oatile3","oatile4"], maxZoom: 18, attribution: osmAttrib});

var flags = {
	twoEndpoint: false
};

// convert a map of query parameters into a query string, 
// expanding Array values into multiple query parameters
var buildQuery = function(params) {
	ret = [];
	for (key in params) {
		vals = params[key];
		// wrap scalars in array
		if ( ! (vals instanceof Array)) vals = new Array(vals);
		for (i in vals) { 
			val = vals[i]; // js iterates over indices not values!
			// skip params that are empty or stated to be the same as previous
			if (val == '' || val == 'same')
				continue;
			param = [encodeURIComponent(key), encodeURIComponent(val)].join('=');
			ret.push(param);
		}
	}
	return "?" + ret.join('&');
};

var analystUrl = "/opentripplanner-api-webapp/ws/tile/{z}/{x}/{y}.png"; 
var analystLayer = new L.TileLayer(analystUrl, {attribution: osmAttrib});

// create geoJSON layers for DC Purple Line

var purpleLineCoords = [
 [-77.094111, 38.984299],
 [-77.077117, 38.994373],
 [-77.053857, 39.000076],
 [-77.039909, 38.999709],
 [-77.031970, 38.994172],
 [-77.024503, 38.995340],
 [-77.017379, 38.999209],
 [-77.009525, 38.999776],
 [-77.003560, 38.998141],
 [-76.995492, 38.999509],
 [-76.987596, 38.989036],
 [-76.978927, 38.983665],
 [-76.955194, 38.985466], 
 [-76.944122, 38.987535],
 [-76.936870, 38.985567],
 [-76.927128, 38.978094],
 [-76.925197, 38.968785],
 [-76.920004, 38.960543],
 [-76.900392, 38.963246],
 [-76.885371, 38.951733],
 [-76.871681, 38.947928]];

var purpleLineStopsFeature = { 
	"type": "Feature",
	"geometry": {
	    "type": "MultiPoint",
	    "coordinates": purpleLineCoords,
	    "properties": {
	        "name": "Purple Line stops"
	    }	
	}
};

var geojsonMarkerOptions = {
		radius: 4,
		fillColor: "#000",
		color: "#000",
		weight: 0,
		opacity: 0,
		fillOpacity: 0.8
};

var purpleLineStopsLayer = new L.GeoJSON(purpleLineStopsFeature, {
	pointToLayer: function (latlng) { 
		return new L.CircleMarker(latlng, geojsonMarkerOptions);
	}});
map.addLayer(purpleLineStopsLayer);

var purpleLineAlignmentFeature = { 
	"type": "Feature",
	"geometry": {
	    "type": "LineString",
	    "coordinates": purpleLineCoords,
	    "properties": {
	        "name": "Purple Line alignment",
	        "style": {
	            "color": "#004070",
	            "weight": 4,
	            "opacity": 0.8
	        }
	    }	
	}
};
var purpleLineAlignmentLayer = new L.GeoJSON(purpleLineAlignmentFeature);
map.addLayer(purpleLineAlignmentLayer);

var baseMaps = {
	"MapBox": mapboxLayer,
    "OSM": osmLayer,
    "Aerial Photo": aerialLayer
};
	        
var overlayMaps = {
    "Analyst Tiles": analystLayer,
    "Stops": purpleLineStopsLayer,
	"Alignment": purpleLineAlignmentLayer
};

var initLocation = INIT_LOCATION;
if (AUTO_CENTER_MAP) {
	// attempt to get map metadata (bounds) from server
	var request = new XMLHttpRequest();
	request.open("GET", "/opentripplanner-api-webapp/ws/metadata", false); // synchronous request
	request.setRequestHeader("Accept", "application/xml");
	request.send(null);
	if (request.status == 200 && request.responseXML != null) {
		var x = request.responseXML;
		var minLat = parseFloat(x.getElementsByTagName('minLatitude')[0].textContent);
		var maxLat = parseFloat(x.getElementsByTagName('maxLatitude')[0].textContent);
		var minLon = parseFloat(x.getElementsByTagName('minLongitude')[0].textContent);
		var maxLon = parseFloat(x.getElementsByTagName('maxLongitude')[0].textContent);
		var lon = (minLon + maxLon) / 2;
		var lat = (minLat + maxLat) / 2;
		initLocation = new L.LatLng(lat, lon);
	}
}
map.setView(initLocation, 12);
var initLocation2 = new L.LatLng(initLocation.lat + 0.05, initLocation.lng + 0.05);
console.log(initLocation, initLocation2);

//Marker icons

var greenMarkerIcon = new L.Icon({ iconUrl: 'js/lib/leaflet/images/marker-green.png' });
var redMarkerIcon = new L.Icon({ iconUrl: 'js/lib/leaflet/images/marker-red.png' });
var origMarker = new L.Marker(initLocation,  {draggable: true, icon: greenMarkerIcon });
var destMarker = new L.Marker(initLocation2, {draggable: true, icon: redMarkerIcon });
origMarker.on('dragend', mapSetupTool);
origMarker.bindPopup("I am the origin.");
destMarker.on('dragend', mapSetupTool);
destMarker.bindPopup("I am the destination.");

// add layers to map 
// do not add analyst layer yet -- it will be added in refresh() once params are pulled in

map.addLayer(mapboxLayer);
map.addLayer(origMarker);
map.addControl(new L.Control.Layers(baseMaps, overlayMaps));

// tools

var purpleOn = function () {
    params.bannedRoutes = "";
    refresh();
};

var purpleOff = function () {
    params.bannedRoutes = "Test_Purple";
    refresh();
};

// use function statement rather than expression to allow hoisting -- is there a better way?
function mapSetupTool() {

	var params = { 
		batch: true
		//bannedRoutes = ["", "Test_Purple"];
	};

	// pull search parameters from form
	switch($('#searchTypeSelect').val()) {
	case 'single':
		params.layers = 'traveltime';
		params.styles = 'color30';
		break;
	case 'ppa':
		params.layers = 'hagerstrand';
		params.styles = 'transparent';
		break;
	case 'diff2':
		params.layers = 'difference';
		params.styles = 'difference';
		break;
	case 'diff1':
		params.layers = 'difference';
		params.styles = 'difference';
		break;
	}
	params.time = [$('#setupTime').val()];
    if (flags.twoEndpoint)
        params.time.push( $('#setupTime2').val() );
    params.mode = $('#setupMode').val();
    params.maxWalkDistance = $('#setupMaxDistance').val();

    // get origin and destination coordinate from map markers
	var o = origMarker.getLatLng();
	params.fromPlace = [o.lat + ',' + o.lng];
	params.arriveBy = [$('#arriveByA').val()];
    if (flags.twoEndpoint) {
    	var d = destMarker.getLatLng();
    	params.fromPlace.push(d.lat + ',' + d.lng);
    	params.arriveBy.push($('#arriveByB').val());
    }
	
	// set from and to places to the same string(s) so they work for both arriveBy and departAfter
	params.toPlace = params.fromPlace;
    	
    var URL = analystUrl + buildQuery(params);
    console.log(params);
    console.log(URL);
    
    // is there a better way to trigger a refresh than removing and re-adding?
	if (analystLayer != null)
		map.removeLayer(analystLayer);
	analystLayer._url = URL;
    map.addLayer(analystLayer);
	legend.src = "/opentripplanner-api-webapp/ws/legend.png?width=300&height=40&styles=" 
		+ params.styles;

	return false;
};     

var downloadTool = function () { 
    var dlParams = {
        format: document.getElementById('downloadFormat').value,
        srs: document.getElementById('downloadProj').value,
        resolution: document.getElementById('downloadResolution').value
    };

    // TODO: this bounding box needs to be reprojected!
    var bounds = map.getBounds();
    var bbox;

    // reproject
    var src = new Proj4js.Proj('EPSG:4326');
    // TODO: undefined srs?
    var dest = new Proj4js.Proj(dlParams.srs);

    // wait until ready then execute
    var interval;
    interval = setInterval(function () {
        // if not ready, wait for next iteration
        if (!(src.readyToUse && dest.readyToUse))
            return;

        // clear the interval so this function is not called back.
        clearInterval(interval);

        var swll = bounds.getSouthWest();
        var nell = bounds.getNorthEast();
        
        var sw = new Proj4js.Point(swll.lng, swll.lat);
        var ne = new Proj4js.Point(nell.lng, nell.lat);

        Proj4js.transform(src, dest, sw);
        Proj4js.transform(src, dest, ne);

        // left, bot, right, top
        bbox = [sw.x, sw.y, ne.x, ne.y].join(',');

        var url = '/opentripplanner-api-webapp/ws/wms' +
            buildQuery(params) +
            '&format=' + dlParams.format + 
            '&srs=' + dlParams.srs +
            '&resolution=' + dlParams.resolution +
            '&bbox=' + bbox;
            // all of the from, to, time, &c. is taken care of by buildQuery.
        
        window.open(url);
    }, 1000); // this is the end of setInterval, run every 1s

    // prevent form submission
    return false;
};

var displayTimes = function(fractionalHours, fractionalHoursOffset) {
	console.log("fhour", fractionalHours);
	// console.log("offset", fractionalHoursOffset);
	var msec = BASE_DATE_MSEC + fractionalHours * MSEC_PER_HOUR; 
	document.getElementById('setupTime').value = new Date(msec).toISOString().substring(0,19);
	msec += fractionalHoursOffset * MSEC_PER_HOUR; 
	document.getElementById('setupTime2').value = new Date(msec).toISOString().substring(0,19);
};

function setFormDisabled(formName, disabled) {
	var form = document.forms[formName];
    var limit = form.elements.length;
    var i;
    for (i=0;i<limit;i++) {
    	console.log('   ', form.elements[i], disabled);
        form.elements[i].disabled = disabled;
    }
}

function setTwoEndpoint(two) {
	if (two) {
		if (!(flags.twoEndpoint)) {
			var llo = origMarker.getLatLng();
			var lld = destMarker.getLatLng();
			lld.lat = llo.lat;
			lld.lng = llo.lng + 0.02;
		}
		//$('#endpointBControls').show( 200 );
		$('#endpointBControls').fadeIn( 500 );
		map.addLayer(destMarker);
	} else {
		//$('#endpointBControls').hide( 800 );
		$('#endpointBControls').fadeOut( 500 );
		map.removeLayer(destMarker);
	}
	flags.twoEndpoint = two;
}

// bind js functions to HTML element events (handle almost everything at the form level)

// anytime a form element changes, refresh the map
$('#searchTypeForm').change( mapSetupTool );

// intercept slider change event bubbling to avoid frequent map rendering
(function(slider, offset) {
    slider.bind('change', function() {
    	displayTimes(slider.val(), offset.val()); 
        return false; // block event propagation
    }).change();
    slider.bind('mouseup', function() {
    	slider.parent().trigger('change');
    });
    offset.bind('change', function() {
    	displayTimes(slider.val(), offset.val()); 
    });
}) ($("#timeSlider"), $('#setupRelativeTime2'));

//hide some UI elements when they are irrelevant
$('#searchTypeSelect').change( function() { 
	var type = this.value;
	console.log('search type changed to', type);
	if (type == 'single')
		setTwoEndpoint(false);
	else
		setTwoEndpoint(true); // but in diff1 we should hide the second marker
}).change(); // trigger this event (and implicitly a form change event) immediately upon binding

