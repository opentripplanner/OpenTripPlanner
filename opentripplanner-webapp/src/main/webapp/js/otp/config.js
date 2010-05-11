otp.config = {
  
    'systemMap': {
        // If true, a system map will be used in the display
        enabled: false,

        // uris and layer names
        // these match up with geoserver
        layerUrlRoutes: 'http://routelayer.example.com/wms',
        layerUrlStops: 'http://stoplayer.example.com/wms',
        layerUrlRoutesHighlighted: 'http://routelayerhighlighted.example.com/wms',
        layerUrlStopsHighlighted: 'http://stoplayerhighlighted.example.com/wms',
        layerNamesRoute: 'routeLayerName',
        layerNamesStop: 'stopLayerName',
        layerNamesRouteHighlighted: 'routeLayerHighlightedName',
        layerNamesStopHighlighted: 'stopLayerHighlightedName',
        
        // this is the uri to the extended api that proxies to geoserver
        controlStopsUrl: '/opentripplanner-api-extended/wms'
    },

    'planner': {
        'url'            : null,
        'fromToOverride' : new Ext.Template("<div class='mapHelp'>Right-click on the map to designate the start and end of your trip.</div>")
    },

    'map': {
        // The default extent to zoom the map to when the web app loads.
        // This can either be an OpenLayers.Bounds object or the string "automatic"
        // If set to "automatic", the client will ask the server for the default extent.
        'defaultExtent': "automatic",
     
        // These options are passed directly to the OpenLayers.Map constructor.
        'options': {
            projection: "EPSG:4326",
            numZoomLevels: 17
        },
        
        // Instead of specifying just the base layer options, you can instead
        // specify the full base layer object.
        // The example below creates a new base layer that uses the default OSM
        // tiles.
        /*
        baseLayer: new OpenLayers.Layer.OSM({
        url: [
              "http://a.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png",
              "http://b.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png",
              "http://c.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png"
          ],
        numZoomLevels: 18
        }), */
        
        // Base tile information 
        'baseLayerOptions': {
            url: 'http://maps.opengeo.org/geowebcache/service/wms',
            layers: ['openstreetmap'],
            format: 'image/png',
            transitionEffect: 'resize'
        }
    },

    // when enabled, adds another item to the accordion for attribution
    'attributionPanel': {
        enabled: true,
        // this shows up as the title of the accordion item
        panelTitle: 'License Attribution',
        // the actual html that appears in the panel
        attributionHtml: '<p class="disclaimer">Disclaimer goes here</p>'
    },

    // if specified, uri path to a custom logo
    // otherwise use the default "images/ui/logoSmall.png"
    'logo': null,

    // List of agency IDs (as specified in GTFS) for which custom icons should be used.
    // Icons should be placed in the custom directory (e.g., custom/nyct)
    // Ex. ['nyct', 'mnr']
    'useCustomIconsForAgencies': [],

    // Metrics system
    //metricsSystem : 'international', // => meters, km
    metricsSystem : 'english', // => feet, miles

    // Context menu with trip planning options (e.g., "Start trip here")
    plannerContextMenu : true,

    // Context menu with general map features (e.g., "Center map here")
    mapContextMenu : false

};
