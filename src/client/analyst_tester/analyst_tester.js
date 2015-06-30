'use strict';

// Basic test of one-to-many profile routing served up at http://localhost:8080/profile_analyst_tester.html
// Or access using a file:// URL to allow modifying this client while using a running OTP server.
// When you move the marker, a request is sent to OTP profile router in one-to-many mode.

var map = L.map('map');

L.tileLayer('http://a.tiles.mapbox.com/v3/conveyal.hml987j0/{z}/{x}/{y}.png', {
    attribution: '&copy; <a href="www.openstreetmap.org/copyright">OpenStreetMap</a>'
}).addTo(map);

// Groups together all visible isochrone layers and allows removing them all at once
var isochroneLayerGroup = L.layerGroup();
isochroneLayerGroup.addTo(map);

var travelTimeTileLayer = L.tileLayer('');
travelTimeTileLayer.addTo(map);


var fromMarker = L.marker(center, {draggable: true});

// Find center of default region
var center;
var req = new XMLHttpRequest();
req.open('GET', 'http://localhost:8080/otp/routers/default', false); // async=false, define center before setting up map
req.setRequestHeader("Accept", "application/json");
req.onload = function() {
    var resp = JSON.parse(req.responseText);
    // console.log(resp);
    var lat = resp['centerLatitude'];
    var lng = resp['centerLongitude'];
    var center = new L.LatLng(lat, lng);
    map.setView(center, 13);
    fromMarker.setLatLng(center);
};
req.send(null);

// Fetch an isochrone for the given surface ID and add the isochrone to the map with the given name.
function getIsochroneForSurfaceId (surfaceId, name) {
    var req = new XMLHttpRequest();
    // High spacing limits the number of isochrones coming back
    req.open('GET', 'http://localhost:8080/otp/surfaces/' + surfaceId + '/isochrone?spacing=45&nMax=1', true); // async=true, background call
    req.setRequestHeader("Accept", "application/json");
    req.onload = function() {
        var response = JSON.parse(req.responseText);
        console.log(response);
        // Each response is a FeatureCollection with 1 feature (a multipolygon with holes)
        // Actually maybe more than one feature if the above call returns multiple isochrones
        var newLayer = L.geoJson(response, {
            style: {
                "color": "#505080",
                "weight": 1,
                "opacity": 0.3
            }
        });
        newLayer.addTo(isochroneLayerGroup);
    }
    req.send(null);
}

function profileRouteThreeIsochrones() {
    var req = new XMLHttpRequest();
    var fromPos = fromMarker.getLatLng();
    var qstring = '?from=' + fromPos.lat + ',' + fromPos.lng;
    qstring += '&analyst=true';
    qstring += '&modes=WALK,BICYCLE,TRANSIT'; // note that TRANSIT or specific transit modes must be included
    // Fetch http://localhost:8080/otp/routers/default/index/routes
    req.open('GET', 'http://localhost:8080/otp/routers/default/profile' + qstring, true); // async=true, background call
    req.setRequestHeader("Accept", "application/json");
    req.onload = function() {
        var resp = JSON.parse(req.responseText);
        console.log(resp);
        getIsochroneForSurfaceId(resp.min, "min");
        getIsochroneForSurfaceId(resp.avg, "avg");
        getIsochroneForSurfaceId(resp.max, "max");
    };
    isochroneLayerGroup.clearLayers();
    req.send(null);
}

function profileRouteIsochrones(banAgency) {

    var url = 'http://localhost:8080/otp/routers/default/profile';
    var fromPos = fromMarker.getLatLng();
    var qstring = '?from=' + fromPos.lat + ',' + fromPos.lng;
    qstring += '&analyst=true';
    qstring += '&modes=WALK,BICYCLE,TRANSIT'; // note that TRANSIT or specific transit modes must be included
    // Fetch bannable routes from http://localhost:8080/otp/routers/default/index/routes

    var req = new XMLHttpRequest();
    req.open('GET', url + qstring, true); // async=true, background call
    req.setRequestHeader("Accept", "application/json");
    req.onload = function() {
        var resp = JSON.parse(req.responseText);
        getIsochroneForSurfaceId(resp.avg, "max");
    };

    var reqBan = new XMLHttpRequest();
    reqBan.open('GET', url + qstring + "&banAgency=" + banAgency, true); // async=true, background call
    reqBan.setRequestHeader("Accept", "application/json");
    reqBan.onload = function() {
        var resp = JSON.parse(reqBan.responseText);
        getIsochroneForSurfaceId(resp.avg, "maxBan");
    };

    isochroneLayerGroup.clearLayers();
    req.send();
    reqBan.send();

}

function profileRouteTiles(bannedAgency) {

    var url = 'http://localhost:8080/otp/routers/default/profile';
    var fromPos = fromMarker.getLatLng();
    var qstring = '?from=' + fromPos.lat + ',' + fromPos.lng;
    qstring += '&analyst=true';
    qstring += '&modes=WALK,BICYCLE,TRANSIT'; // note that TRANSIT or specific transit modes must be included
    // Fetch bannable routes from http://localhost:8080/otp/routers/default/index/routes

    var req = new XMLHttpRequest();
    req.open('GET', url + qstring, false);
    req.setRequestHeader("Accept", "application/json");
    req.send();
    var resp = JSON.parse(req.responseText);
    var surf = resp.avg;

    var reqBan = new XMLHttpRequest();
    reqBan.open('GET', url + qstring + "&banAgency=" + bannedAgency, false);
    reqBan.setRequestHeader("Accept", "application/json");
    reqBan.send();
    var respBan = JSON.parse(reqBan.responseText);
    var surfBan = respBan.avg;

    var url = 'http://localhost:8080/otp/surfaces/' + surfBan + '/differencetiles/' + surf + '/{z}/{x}/{y}.png';
    travelTimeTileLayer.setUrl(url);

}

fromMarker.on("dragend",function(ev){
    var pos = ev.target.getLatLng();
    this.bindPopup(pos.toString());
    profileRouteThreeIsochrones("0");
});

fromMarker.addTo(map);
