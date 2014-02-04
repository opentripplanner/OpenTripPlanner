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

/*
 * OpenTripPlanner Analyst JavaScript library demo (Marseille Metropole).
 */
$(function() {

    /* Global context object. */
    var gui = {};
    /* Our reference point for diff mode */
    gui.GRID_ORIGIN = L.latLng(43.3, 5.4);

    /* Initialize a leaflet map */
    gui.map = L.map('map', {
        minZoom : 10,
        maxZoom : 18,
    }).setView(L.latLng(43.297, 5.370), 12);

    /* Add OSM/OpenTransport layers. TODO Add MapBox layer. */
    gui.osmLayer = new L.TileLayer("http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png", {
        subdomains : [ "otile1", "otile2", "otile3", "otile4" ],
        maxZoom : 18,
        attribution : "Map data &copy; OpenStreetMap contributors"
    });
    gui.otLayer = new L.TileLayer(
            "http://{s}.tile.thunderforest.com/transport/{z}/{x}/{y}.png",
            {
                subdomains : [ "a", "b", "c" ],
                maxZoom : 18,
                attribution : "Map data &copy; OpenStreetMap contributors & <a href='http://www.thunderforest.com/'>Thunderforest</a>"
            });
    gui.map.addLayer(gui.otLayer);

    /* Create 2 layer groups for easier display / management */
    gui.gradientLayerGroup = new L.LayerGroup([]);
    gui.isochronesLayerGroup = new L.LayerGroup([]);
    gui.map.addLayer(gui.gradientLayerGroup);
    gui.map.addLayer(gui.isochronesLayerGroup);

    /* Add controls to the map */
    L.control.layers({
        "Transport" : gui.otLayer,
        "OSM" : gui.osmLayer
    }, {
        "Gradient" : gui.gradientLayerGroup,
        "Isochrones" : gui.isochronesLayerGroup,
    }).addTo(gui.map);

    /* Load populations */
    gui.colleges = new otp.analyst.Population();
    gui.colleges.loadFromCsv("colleges.csv", {
        lonColName : "X",
        latColName : "Y",
        nameColName : "DESIGNATIO"
    });
    gui.collegesPlaces = new otp.analyst.Population();
    gui.collegesPlaces.loadFromCsv("colleges.csv", {
        lonColName : "X",
        latColName : "Y",
        nameColName : "DESIGNATIO",
        weightColName : "TOTAL"
    });
    gui.lycees = new otp.analyst.Population();
    gui.lycees.loadFromCsv("lycees.csv", {
        lonColName : "X",
        latColName : "Y",
        nameColName : "DESIGNATIO"
    });

    /* Select client-wide locale */
    otp.setLocale(otp.locale.French);

    /* Create a request parameter widget */
    gui.widget1 = new otp.analyst.ParamsWidget($('#widget1'), {
        coordinateOrigin : gui.GRID_ORIGIN,
        selectMaxTime : true,
        map : gui.map
    });

    /* Called whenever some parameters have changed. */
    gui.refresh = function() {
        /* Disable the refresh button to prevent too many calls */
        $("#refresh").prop("disabled", true);
        /* Get the current parameter values */
        var params1 = gui.widget1.getParameters();
        var max = params1.zDataType == "BOARDINGS" ? 10
                : params1.zDataType == "WALK_DISTANCE" ? params1.maxWalkDistance * 1.2 : params1.maxTimeSec;
        /* Get a TimeGrid from the server. */
        gui.timeGrid = new otp.analyst.TimeGrid(params1).onLoad(function(timeGrid) {
            /* Create a ColorMap */
            gui.colorMap = new otp.analyst.ColorMap({
                max : max,
                zDataType : params1.zDataType
            });
            gui.colorMap.setLegendCanvas($("#legend").get(0));
            /* Clear old layers, add a new one. */
            gui.gradientLayerGroup.clearLayers();
            gui.layer = otp.analyst.TimeGrid.getLeafletLayer(gui.timeGrid, gui.colorMap);
            gui.layer.setOpacity(0.5);
            gui.gradientLayerGroup.addLayer(gui.layer);
            gui.layer.bringToFront(); // TODO Leaflet bug?
            /* Re-enable refresh button */
            $("#refresh").prop("disabled", false);

            /* Update scores */
            var scorer = new otp.analyst.Scoring();
            var edge30 = otp.analyst.Scoring.stepEdge(1800);
            var edge60 = otp.analyst.Scoring.stepEdge(3600);
            $("#c30").text(scorer.score(timeGrid, gui.colleges, edge30, 1.0));
            $("#c60").text(scorer.score(timeGrid, gui.colleges, edge60, 1.0));
            $("#cp30").text(scorer.score(timeGrid, gui.collegesPlaces, edge30, 1.0));
            $("#cp60").text(scorer.score(timeGrid, gui.collegesPlaces, edge60, 1.0));
            $("#l30").text(scorer.score(timeGrid, gui.lycees, edge30, 1.0));
            $("#l60").text(scorer.score(timeGrid, gui.lycees, edge60, 1.0));
        });

        /* Check if we should display vector isochrones. */
        var isoEnable = $("#isoEnable").is(":checked");
        $("#downloadIsoVector").prop("disabled", !isoEnable);
        gui.isochronesLayerGroup.clearLayers();
        if (isoEnable) {
            /* Get the cutoff times from the input, in minutes */
            var isotimes = [];
            var isostr = $("#cutoffSec").val().split(";");
            for (var i = 0; i < isostr.length; i++) {
                isotimes.push(parseInt(isostr[i]) * 60);
            }
            /* Get the isochrone GeoJSON features from the server */
            gui.isochrone = new otp.analyst.Isochrone(params1, isotimes).onLoad(function(iso) {
                for (var i = 0; i < isotimes.length; i++) {
                    var isoLayer = L.geoJson(iso.getFeature(isotimes[i]), {
                        style : {
                            color : "#0000FF",
                            weight : 1,
                            dashArray : (i % 2) == 1 ? "5,5" : "",
                            fillOpacity : 0.0,
                            fillColor : "#000000"
                        }
                    });
                    gui.isochronesLayerGroup.addLayer(isoLayer);
                }
            });
        }
    };

    /* Plug the refresh callback function. */
    gui.widget1.onRefresh(gui.refresh);
    $("#refresh").click(gui.refresh);
    $("#isoEnable").click(gui.refresh);
    /* Refresh to force an initial load. */
    gui.widget1.refresh();

    /* Download image button */
    $('#downloadIsoimage').click(function() {
        var image = otp.analyst.TimeGrid.getImage(gui.timeGrid, gui.colorMap, {
            width : 2000, // TODO parameter
            // Default to map bounds
            southwest : gui.map.getBounds().getSouthWest(),
            northeast : gui.map.getBounds().getNorthEast()
        });
        window.open(image.src);
    });

    /* Download vector isochrone button */
    $("#downloadIsoVector").prop("disabled", true).click(function() {
        window.open(gui.isochrone.getUrl("iso.zip"));
    });

    /* About / contact buttons */
    $("#about").click(function() {
        $("#aboutContent").dialog({
            width : 800,
            height : 600
        }).show();
    });
    $("#contact").click(function() {
        $("#contactContent").dialog().show();
    });
});
