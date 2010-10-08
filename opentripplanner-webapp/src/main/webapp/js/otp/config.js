otp.config = {
  
    'systemMap': {
        // If true, a system map will be used in the display
        enabled: false,

        // uris and layer names
        // these match up with geoserver
        layerUrlRoutes: 'http://localhost:5180/geoserver/wms',
        layerUrlStops: 'http://localhost:5180/geoserver/wms',
        layerUrlRoutesHighlighted: 'http://localhost:5180/geoserver/wms',
        layerUrlStopsHighlighted: 'http://localhost:5180/geoserver/wms',
        layerNamesRoute: 'routes',
        layerNamesStop: 'stops',
        layerNamesRouteHighlighted: 'routes_highlighted',
        layerNamesStopHighlighted: 'stops_highlighted',
        
        // this is the uri to the extended api that proxies to geoserver
        controlStopsUrl: '/opentripplanner-api-extended/wms'
    },

    'planner': {
        'url'            : null,
        'linkTemplates'  : [
            {name:'Link to this trip (OTP)',  url:'index.html?' + otp.planner.ParamTemplate},

            {separator:true, name:'This trip on other transit planners'},
            {name: 'Google Transit',       url:'http://www.google.com/maps?<tpl if="arriveBy == \'Arrive\'">ttype=arr&</tpl>date={date}&time={time}&daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=r'},
//            {name:'TriMet (Map Planner)',  url:'http://maps.trimet.org?<tpl if="opt == \'TRANSFERS\'">min=X&</tpl><tpl if="maxWalkDistance &gt; 1000.0">walk=0.9999&</tpl>arr={arriveBy}&date={date}&time={time}&from={fromLat},{fromLon}&to={toLat},{toLon}&submit'},
//            {name:'TriMet (Text Planner - time and date reset to current)', url:'http://trimet.org/go/cgi-bin/plantrip.cgi?<tpl if="arriveBy == \'Arrive\'">Arr=A&</tpl><tpl if="opt == \'TRANSFERS\'">Min=X&</tpl><tpl if="maxWalkDistance &gt; 1000.0">Walk=0.9999&</tpl>date={date}&time={time}&from={fromLat},{fromLon}&to={toLat},{toLon}&submit'},            

            {separator:true, name:'On other bike trip planners'},
            {name: 'Google Bikes',    url:'http://www.google.com/maps?daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=b'},
//            {name: 'ByCycle',         url:'http://bycycle.org/regions/portlandor/routes/find?s=longitude%3D{fromLon}%20latitude%3D{fromLat}&e=longitude%3D{toLon},%20latitude%3D{toLat}&pref=default'},

            {separator:true, name:'On other walking direction planners'},
            {name: 'Google Walking',  url:'http://www.google.com/maps?daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=w'}
        ],
        // this controls whether selecting some options, like mode, affects
        // what other options are shown in the form
        'useOptionDependencies': false,
        'geocoder': {
            enabled: false,
            url: "/geocoder/geocode",
            addressParamName: "address"
        },
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
            numZoomLevels: 20
        },
        
        // Instead of specifying just the base layer options, you can instead
        // specify the full base layer object.
        // The example below creates a new base layer that uses the default OSM
        // tiles.
        
        baseLayer: new OpenLayers.Layer.OSM({
        url: [
              "http://a.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png",
              "http://b.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png",
              "http://c.tah.openstreetmap.org/Tiles/tile/${z}/${x}/${y}.png"
          ],
        numZoomLevels: 20
        }),
        
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

    // presents a dialog on initial startup of the app, with a message for your customers
    'splashScreen': {
        enabled: false,
        timeout: 20,   // seconds to stay open - if <= ZERO, then dialog does not timeout and requires the customer to close the dialog
        title:   'Important: Please read',
        html:    '<p class="splash-screen">'
                 + 'Please note that the trip routing presented here is for demonstration purposes of the <a href="http://opentripplanner.com" target="#">OpenTripPlanner (OTP)</a> only, '
                 + 'and not intended as a travel resource.  You will begin to see improvements in the planned trips as the project matures.  A public beta is scheduled for spring 2011. '
                 + '</p>'
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
