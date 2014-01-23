/**
 * OTPA TimeGrid lib demo.
 */

/* --- LEAFLET STUFF --- */

// Initialize a map
var map = L.map('map', {
    minZoom : 10,
    maxZoom : 18,
}).setView(L.latLng(43.297, 5.370), 12);

// Add OSM/OpenTransport layers
var osmAttrib = 'Map data &copy; OpenStreetMap contributors';
var osmLayer = new L.TileLayer(
        "http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png", {
            subdomains : [ "otile1", "otile2", "otile3", "otile4" ],
            maxZoom : 18,
            attribution : osmAttrib
        });
var otAttrib = 'Map data &copy; OpenStreetMap contributors & <a href="http://www.thunderforest.com/">Thunderforest</a>';
var otLayer = new L.TileLayer(
        "http://{s}.tile.thunderforest.com/transport/{z}/{x}/{y}.png", {
            subdomains : [ "a", "b", "c" ],
            maxZoom : 18,
            attribution : osmAttrib
        });
map.addLayer(otLayer);

// Isochrone layer group
var isochrones = new L.LayerGroup([]);
map.addLayer(isochrones);

// Add control
map.addControl(new L.Control.Layers({
    "Transport" : otLayer,
    "OSM" : osmLayer
}));

// Origin marker
var origMarker = L.marker(map.getCenter(), {
    draggable : true,
}).on('dragend', updateOrigin).addTo(map);
map.on('click', function mapClick(e) {
    origMarker.setLatLng(e.latlng);
    updateOrigin();
});

/* --- OTPA STUFF --- */

// Load POIs
var hospitals = OTPA.poiList();
hospitals.loadFromJson('hospitals.json');

// Default request
var reqParams = {
    routerId : '',
    fromPlace : null,
    date : '2014/01/15',
    time : '12:00:00',
    mode : 'WALK,TRANSIT',
    maxWalkDistance : 2000,
    precisionMeters : 100,
    maxTimeSec : 5400
};
// Isotimes to display
var isotimes = [ 900, 1800, 2700, 3600 ];
var timeGridLayer;
var colorMap = null;

function updateOrigin() {
    // Update the origin location
    reqParams.fromPlace = origMarker.getLatLng().lat + ','
            + origMarker.getLatLng().lng;
    // Get a TimeGrid
    var timeGrid = OTPA.timeGrid(reqParams, function(timeGrid) {

        // Add a gradient layer
        isochrones.clearLayers();
        colorMap = OTPA.colorMap(0, reqParams.maxTimeSec);
        timeGridLayer = timeGrid.getLeafletLayer(colorMap);
        timeGridLayer.setOpacity(0.7);
        isochrones.addLayer(timeGridLayer);
        timeGridLayer.bringToFront();

        // Add isochrone layers
        // var iso = OTPA.isochrone(reqParams, isotimes);
        // for (var i = 0; i < isotimes.length; i++) {
        // isochrones.addLayer(L.geoJson(iso.getFeature(isotimes[i]), {
        // style : {
        // color : "#0000FF",
        // weight : 1,
        // dashArray : (i % 2) == 0 ? "5,5" : "",
        // fillOpacity : 0.0,
        // fillColor : "#000000"
        // }
        // }));
        // }

        // Compute and update hospital scoring
        var scorer = OTPA.scoring();
        var wHealth = scorer.score(timeGrid, hospitals,
                OTPA.sigmoid(1800, 900), 10);
        $('#hospitalScore').text(wHealth.toFixed(2));
    });
};


// Create a slider and connect it to the color map cutoff.
$(function() {
    $("#cutoffSec").slider({
        min : 0,
        max : reqParams.maxTimeSec,
        range : true,
        step : 300,
        values : [ 0, reqParams.maxTimeSec ]
    }).on("slidechange", function(ev, ui) {
        colorMap.setMinCutoff(ui.values[0]);
        colorMap.setMaxCutoff(ui.values[1]);
        timeGridLayer.refresh();
    });
    updateOrigin();
});
