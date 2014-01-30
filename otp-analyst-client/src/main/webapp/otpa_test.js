/**
 * OTPA TimeGrid lib demo.
 */

/* --- LEAFLET STUFF --- */

var GRID_ORIGIN = "43.3,5.4"; // Grid reference point
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
L.control.layers({
    "Transport" : otLayer,
    "OSM" : osmLayer
}, {
    "Gradient" : isochrones,
}).addTo(map);

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

// Default 1st request
var reqParams = {
    routerId : '',
    fromPlace : null,
    date : '2014/01/15',
    time : '12:00:00',
    // walkSpeed : 1.5,
    // maxTransfers : 3,
    // transferPenalty : 0,
    // walkReluctance : 1.0,
    mode : 'WALK,TRANSIT',
    maxWalkDistance : 1000,
    precisionMeters : 100,
    maxTimeSec : 5400,
    coordinateOrigin : GRID_ORIGIN
};
// Default 2dn request
var reqParams2 = {
    routerId : '',
    fromPlace : null,
    date : '2014/01/15',
    time : '20:00:00',
    // walkSpeed : 1.5,
    // maxTransfers : 3,
    // transferPenalty : 0,
    // walkReluctance : 3.0,
    // bannedRoutes : "TRRTM\\_1__T1\\_A,TRRTM\\_1__T2\\_A",
    mode : 'WALK,TRANSIT',
    maxWalkDistance : 1000,
    precisionMeters : 100,
    maxTimeSec : 5400,
    coordinateOrigin : GRID_ORIGIN
};
// Isotimes to display
var isotimes = [ 900, 1800, 2700, 3600 ];
var timeGridLayer1;
var timeGridLayerDiff;
var colorMap = null;
var colorMapDiff = null;
var timeGrid1 = null;
var timeGridDiff = null;

function updateOrigin() {
    // Update the origin location
    reqParams.fromPlace = origMarker.getLatLng().lat + ','
            + origMarker.getLatLng().lng;
    reqParams2.fromPlace = reqParams.fromPlace;
    // Get a TimeGrid
    var timeGrid2 = null;
    timeGrid1 = OTPA.timeGrid(reqParams, function() {
        timeGrid2 = OTPA.timeGrid(reqParams2, function() {

            timeGridDiff = OTPA.timeGridComposite(timeGrid1, timeGrid2);
            // Add a gradient layer
            isochrones.clearLayers();
            colorMap = OTPA.colorMap({
                max : reqParams.maxTimeSec
            });
            colorMap.setLegendCanvas($("#legend").get(0));
            colorMapDiff = OTPA.colorMap({
                min : -600,
                max : +600,
                delta : true
            });
            colorMapDiff.setLegendCanvas($("#legendDiff").get(0));
            timeGridLayerDiff = OTPA
                    .getLeafletLayer(timeGridDiff, colorMapDiff);
            timeGridLayerDiff.setOpacity(0.5);
            timeGridLayer1 = OTPA.getLeafletLayer(timeGrid1, colorMap);
            timeGridLayer1.setOpacity(0.5);

            if ($('#diffLayer').is(':checked')) {
                isochrones.addLayer(timeGridLayerDiff);
                timeGridLayerDiff.bringToFront();
            } else {
                isochrones.addLayer(timeGridLayer1);
                timeGridLayer1.bringToFront();
            }

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
            var wHealth = scorer.score(timeGrid1, hospitals, OTPA.sigmoid(1800,
                    900), 10);
            $('#hospitalScore').text(wHealth.toFixed(2));
        });
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
        timeGridLayer1.refresh();
    });
    updateOrigin();
    $('#diffLayer').click(function() {
        if ($('#diffLayer').is(':checked')) {
            isochrones.removeLayer(timeGridLayer1);
            isochrones.addLayer(timeGridLayerDiff);
            timeGridLayerDiff.bringToFront();
            $('#legend').hide();
            $('#cutoffSec').hide();
            $('#legendDiff').show();
        } else {
            isochrones.removeLayer(timeGridLayerDiff);
            isochrones.addLayer(timeGridLayer1);
            timeGridLayer1.bringToFront();
            $('#legendDiff').hide();
            $('#legend').show();
            $('#cutoffSec').show();
        }
    });
    $('#legendDiff').hide();
    $('#downloadIsoimage').click(
            function() {
                var timeGrid = $('#diffLayer').is(':checked') ? timeGridDiff
                        : timeGrid1;
                var colorMp = $('#diffLayer').is(':checked') ? colorMapDiff
                        : colorMap;
                var image = OTPA.getImage(timeGrid, colorMp, {
                    width : 2000,
                    // If not provided, default to grid extent
                    southwest : map.getBounds().getSouthWest(),
                    northeast : map.getBounds().getNorthEast()
                });
                window.open(image.src);
            });
});
