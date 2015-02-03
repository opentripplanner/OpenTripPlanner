/**
 * OTPA TimeGrid lib demo.
 */

$(function() {

    /* --- GLOBAL STUFF --- */
    var gui = {};
    gui.GRID_ORIGIN = L.latLng(43.3, 5.4); // Grid reference point

    /* --- LEAFLET STUFF --- */
    // Initialize a map
    gui.map = L.map('map', {
        minZoom : 10,
        maxZoom : 18,
    }).setView(L.latLng(43.297, 5.370), 12);

    // Add OSM/OpenTransport layers
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

    // Isochrone layer group
    gui.isochrones = new L.LayerGroup([]);
    gui.map.addLayer(gui.isochrones);

    // Add control
    L.control.layers({
        "Transport" : gui.otLayer,
        "OSM" : gui.osmLayer
    }, {
        "Gradient" : gui.isochrones,
    }).addTo(gui.map);

    /* --- OTPA STUFF --- */
    otp.setLocale(otp.locale.English);
    gui.widget1 = new otp.analyst.ParamsWidget($('#widget1'), {
        coordinateOrigin : gui.GRID_ORIGIN,
        selectMaxTime : true,
        map : gui.map
    });
    gui.widget2 = new otp.analyst.ParamsWidget($('#widget2'), {
        extend : gui.widget1,
        selectMaxTime : true,
        map : gui.map
    });
    /* Called whenever some parameters have changed. */
    gui.refresh = function() {
        /* Display the leaflet from the gui.timeGrid */
        function updateLayer() {
            gui.layer = otp.analyst.TimeGrid.getLeafletLayer(gui.timeGrid, gui.colorMap);
            gui.layer.setOpacity(0.5);
            gui.isochrones.clearLayers();
            gui.isochrones.addLayer(gui.layer);
            gui.layer.bringToFront();
            $("#refresh").prop("disabled", false);
        }
        $("#refresh").prop("disabled", true);
        var diffMode = $("#diffMode").is(":checked");
        gui.widget2.setVisible(diffMode);
        var params1 = gui.widget1.getParameters();
        if (!diffMode) {
            var max = params1.zDataType == "BOARDINGS" ? 10
                    : params1.zDataType == "WALK_DISTANCE" ? params1.maxWalkDistance * 1.2 : params1.maxTimeSec;
            gui.timeGrid = new otp.analyst.TimeGrid(params1).onLoad(function(timeGrid) {
                gui.colorMap = new otp.analyst.ColorMap({
                    max : max,
                    zDataType : params1.zDataType
                });
                gui.colorMap.setLegendCanvas($("#legend").get(0));
                updateLayer();
            });
        } else {
            var max = params1.zDataType == "BOARDINGS" ? 5
                    : params1.zDataType == "WALK_DISTANCE" ? params1.maxWalkDistance / 2 : params1.maxTimeSec / 5;
            var params2 = gui.widget2.getParameters();
            gui.timeGrid1 = new otp.analyst.TimeGrid(params1).onLoad(function(timeGrid) {
                gui.timeGrid2 = new otp.analyst.TimeGrid(params2).onLoad(function(timeGrid) {
                    // TODO Parametrize compose function
                    gui.timeGrid = new otp.analyst.TimeGridComposite(gui.timeGrid1, gui.timeGrid2);
                    gui.colorMap = new otp.analyst.ColorMap({
                        delta : true,
                        max : max,
                        zDataType : params1.zDataType
                    });
                    gui.colorMap.setLegendCanvas($("#legend").get(0));
                    updateLayer();
                });
            });
        }
    };

    gui.widget1.onRefresh(gui.refresh);
    $("#diffMode").click(gui.refresh);
    gui.widget2.onRefresh(gui.refresh);
    $("#refresh").click(gui.refresh);
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
    $('#downloadIsochrone').click(function() {
        var params = gui.widget1.getParameters();
        var isochrone = new otp.analyst.Isochrone(params, 3600, {
            load : false
        });
        window.open(isochrone.getUrl());
    });
});
