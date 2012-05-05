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

var portland      = new L.LatLng(45.5191, -122.6745);
var ottawa        = new L.LatLng(45.4131, -75.63806);
var sanfrancisco  = new L.LatLng(37.7805, -122.419);
var dc            = new L.LatLng(38.8951, -77.03666);
var new_carrolton = new L.LatLng(38.9538, -76.8851);

var initLocation = new_carrolton;

var map = new L.Map('map', {
	minZoom : 10,
	maxZoom : 16
});

var mapboxURL = "http://{s}.tiles.mapbox.com/v3/mapbox.mapbox-streets/{z}/{x}/{y}.png";
var OSMURL    = "http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png";
var aerialURL = "http://{s}.mqcdn.com/naip/{z}/{x}/{y}.png";

var mapboxAttrib = "Data <a href='http://creativecommons.org/licenses/by-sa/2.0/' target='_blank'>CC-BY-SA</a>" +
    " by <a href='http://openstreetmap.org/' target='_blank'>OpenStreetMap</a>, " +
    "Tiles from <a href='http://mapbox.com/about/maps' target='_blank'>MapBox Streets</a>";
var mapboxLayer = new L.TileLayer(mapboxURL, 
		{subdomains: ["a","b","c","d"], maxZoom: 16, attribution: mapboxAttrib});

var osmAttrib = 'Map data &copy; 2011 OpenStreetMap contributors';
var osmLayer = new L.TileLayer(OSMURL, 
		{subdomains: ["otile1","otile2","otile3","otile4"], maxZoom: 16, attribution: osmAttrib});

var aerialLayer = new L.TileLayer(aerialURL, 
		{subdomains: ["oatile1","oatile2","oatile3","oatile4"], maxZoom: 16, attribution: osmAttrib});

var flags = {
	twoEndpoint: false,
	// note times are in UTC
    startTime: '2012-06-06T14:00:00Z',
    endTime: '2012-06-06T16:00:00Z'
};

var params = {
    layers: 'traveltime',
    styles: 'mask',
    time: flags.startTime,
    batch: true,
    maxWalkDistance: '2500',
	mode: 'WALK,TRANSIT' // SUBWAY for metrorail, TRAM for purple line
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
			param = [encodeURIComponent(key), encodeURIComponent(val)].join('=');
			ret.push(param);
		}
	}
	return "?" + ret.join('&');
};

var analystUrl = "/opentripplanner-api-webapp/ws/tile/{z}/{x}/{y}.png"; 
var analystLayer = new L.TileLayer(analystUrl + buildQuery(params), {attribution: osmAttrib});

var refresh = function () {
	var o = origMarker.getLatLng();
	var d = destMarker.getLatLng();
    if (flags.twoEndpoint) {
    	params.fromPlace = [o.lat + "," + o.lng, d.lat + "," + d.lng];
    	// toPlace is not currently used, but must be provided to avoid missing vertex error
    	// missing to place should be tolerated in batch mode
    	params.toPlace = [d.lat + "," + d.lng, o.lat + "," + o.lng];
    	map.addLayer(destMarker);
    } else {
    	params.fromPlace = o.lat + "," + o.lng;
    	// toPlace is not currently used, but must be provided to avoid missing vertex error
    	params.toPlace = d.lat + "," + d.lng;
    	map.removeLayer(destMarker);
    }
    console.log(flags);
    console.log(params);
    console.log(analystUrl + buildQuery(params));
	if (analystLayer != null)
		map.removeLayer(analystLayer);
	analystLayer._url = analystUrl + buildQuery(params);
    map.addLayer(analystLayer);
};

var baseMaps = {
	"MapBox": mapboxLayer,
    "OSM": osmLayer,
    "Aerial Photo": aerialLayer
};
	        
var overlayMaps = {
    "Analyst WMS": analystLayer,
    //"Analyst Tiles": analystTile
};

var origMarker = new L.Marker(initLocation, {draggable: true});
var destMarker = new L.Marker(initLocation, {draggable: true});
//marker.bindPopup("I am marker.");
origMarker.on('dragend', refresh);
destMarker.on('dragend', refresh);

map.addLayer(mapboxLayer);
map.addLayer(analystLayer);
map.addLayer(origMarker);
map.setView(initLocation, 12);

var layersControl = new L.Control.Layers(baseMaps, overlayMaps);
map.addControl(layersControl);

refresh();

// tools

var purpleOn = function () {
    params.bannedRoutes = "";
    refresh();
};

var purpleOff = function () {
    params.bannedRoutes = "Test_Purple";
    refresh();
};

var color30 = function () {
	params.layers = 'traveltime',
	params.styles = 'color30',
    params.time = flags.startTime;
	flags.twoEndpoint = false;
    refresh();
};

var gray = function () {
	params.layers = 'traveltime',
	params.styles = 'mask',
    params.time = flags.startTime;
	flags.twoEndpoint = false;
    refresh();
};

var difference = function () {
	params.layers = 'difference',
	params.styles = 'difference',
    params.bannedRoutes = ["", "Test_Purple"];
    params.time = flags.startTime;
	flags.twoEndpoint = false;
    refresh();
};

var hagerstrand = function () {
	params.layers = 'hagerstrand',
	params.styles = 'transparent',
    params.bannedRoutes = "";
	params.time = [flags.startTime, flags.endTime],
	flags.twoEndpoint = true;
    refresh();
};

var mapSetupTool = function () {
    var o = document.getElementById('setupOrigTime').value;
    if (o != '')
        flags.startTime = o;

    var d = document.getElementById('setupDestTime').value;
    if (d != '')
        flags.endTime = d;

    var l = document.getElementById('setupLayer').value;
    // if we switch from Hägerstrand to Travel Time, remove dest marker
    if (l == 'hagerstrand')
        hagerstrand();

    else if (l == 'traveltime') {
        gray();
    }

    flags.layer = l;

    refresh();
    return false;
};     

var downloadTool = function () { 
    var params = {
        format: document.getElementById('downloadFormat').value,
        srs: document.getElementById('downloadProj').value,
        layers: document.getElementById('downloadLayer').value,
        resolution: document.getElementById('downloadResolution').value
    };

    // TODO: this bounding box needs to be reprojected!
    var bounds = map.getBounds();
    var bbox;

    // reproject
    var src = new Proj4js.Proj('EPSG:4326');
    // TODO: undefined srs?
    var dest = new Proj4js.Proj(params.srs);

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

        var url = '/opentripplanner-api-webapp/ws/wms?layers=' + params.layers +
            '&format=' + params.format + 
            '&srs=' + params.srs +
            '&resolution=' + params.resolution +
            '&bbox=' + bbox +
            '&DIM_ORIGINLAT=' + analyst.wmsParams.DIM_ORIGINLAT +
            '&DIM_ORIGINLON=' + analyst.wmsParams.DIM_ORIGINLON +
            '&time=' + analyst.wmsParams.time +
            '&DIM_ORIGINLATB=' + analyst.wmsParams.DIM_ORIGINLATB + 
            '&DIM_ORIGINLONB=' + analyst.wmsParams.DIM_ORIGINLONB +
            '&DIM_TIMEB=' + analyst.wmsParams.DIM_TIMEB;

        window.open(url);
    }, 1000); // this is the end of setInterval, run every 1s
};
