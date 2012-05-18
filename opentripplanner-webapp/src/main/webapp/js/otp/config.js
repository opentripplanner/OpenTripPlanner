/**
 * make sure that otp.config.locale is set to your default language
 *
 * NOTE:
 *   - for apps that support a single language simply set your locale here (or in your .html file prior to including config.js)
 *   - for apps that support multiple languages, you will need a scheme that determines the user's desired language,  
 *     which assigns the proper otp.locale.<Language> to otp.config.locale prior to including config.js (this file)
 */
// step 1: make sure we have some type of otp.config, and otp.config.local defined
if(typeof(otp) == "undefined" || otp == null) otp = {};
if(typeof(otp.config) == "undefined" || otp.config == null) otp.config = {};
if(typeof(otp.config.locale) == "undefined" || otp.config.locale == null) otp.config.locale = otp.locale.English;

// step 2: create an object of default otp.config default values (see step3 where we apply this to any existing config)
otp.config_defaults = {
    routerId      : "",
    locale        : otp.config.locale,
    metricsSystem : otp.config.locale.config.metricsSystem,  // Metrics system (e.g., 'english' == feet, miles, other value or null is metric system)

    planner : {
        url            : null,
        printUrl       : "print.html",
        maxTransfers   : null,  // when maxTransfers != null, value is sent down as maxTransfers param to the api (current api default maxTransfers=2)

        // options to turn stuff on / off on the planner
        options        : {
            showElevationGraph    : true,   // turn on/off the southern panel that displays the elevation data
            showBikeshareMode     : true,   // turn on/off the bikeshare options in the mode pull down
            showTrainMode         : true,   // turn on/off the train options in the mode pull down
            showWheelchairForm    : true,   // turn on/off the wheelchair check box (on by default)
            showIntermediateForms : true,   // turn on/off the ability to plan routes with intermediate points 
            showStopCodes         : true,   // show stop codes as part of the itinerary
            showAgencyInfo        : true,   // show the 'service run by Yolobus' on each itinerary leg
            showFareInfo          : true,   // show the fare information in the itinerary
            showReverseButton     : true,   // turn on/off itinerary reverse button
            showEditButton        : true,   // turn on/off itinerary edit button
            showPrintButton       : true,   // turn on/off itinerary print button
            showLinksButton       : true,   // turn on/off itinerary links button
            useOptionDependencies : true,   // trip form changes based on mode and optimize flags (e.g., bike mode has no wheelchair or walk distance forms etc...) 
            useRouteLongName      : false,  // format route name with both short-name and long-name...see / override Itinerary.makeRouteName() for different formatting options
            appendGeocodeName     : true,   // true = send string:lat,lon parameter format to OTP, else just lat,lon goes to OTP 
            OPTIONS_NOTE: "THIS IS A STRUCTURE USED TO CUSTOMIZE THE TRIP FORMS AND OTHER BEHAVIORS"
        },

        // will add a tree node to the bottom of the itinerary with this message
        itineraryMessages : {
            icon            : null,
            transit         : "This is an Itinerary Message test...",
            transit         : null, 
            bus             : null,
            train           : null,
            bicycle         : null,
            bicycle_transit : null,
            walk            : null 
        },

        linkTemplates  : [
            {name:otp.config.locale.tripPlanner.link.text,  url:'index.html#/' + otp.planner.ParamTemplate}, // TODO - this will cause an error if otp.planner is not defined
            {name:otp.config.locale.tripPlanner.link.trip_separator, separator:true},
            {name:otp.config.locale.tripPlanner.link.google_transit, url: otp.config.locale.tripPlanner.link.google_domain + '/maps?<tpl if="arriveBy == \'Arrive\'">ttype=arr&</tpl>date={date}&time={time}&daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=r'},
            {name:otp.config.locale.tripPlanner.link.bike_separator, separator:true},
            {name:otp.config.locale.tripPlanner.link.google_bikes,   url:otp.config.locale.tripPlanner.link.google_domain + '/maps?daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=b'},
            {name:otp.config.locale.tripPlanner.link.walk_separator, separator:true},
            {name:otp.config.locale.tripPlanner.link.google_walk,    url:otp.config.locale.tripPlanner.link.google_domain + '/maps?daddr={toLat},{toLon}&saddr={fromLat},{fromLon}&ie=UTF8&dirflg=w'}
        ],

        geocoder  :
        {
            enabled : false,
            url     : "/geocoder/geocode",  
            addressParamName : "address"
        },
        fromToOverride : new Ext.Template('<div class="mapHelp">' + otp.config.locale.config.rightClickMsg + '</div>')

        /* debug geocoder */
        /*  *
        ,fromToOverride:null,
        geocoder  :
        {
            enabled : true,
            isSolr  : true,
            url     : "/js/otp/planner/test/solr-geo.json",
            addressParamName : "address"
        }
        /*  *
        ,fromToOverride:null,
        geocoder  :
        {
            enabled : true,
            isSolr  : false,
            url     : "/js/otp/planner/test/geo-multi.xml",
            addressParamName : "address"
        }
        /* */
    },

    map : {
        // The default extent to zoom the map to when the web app loads.
        // This can either be an OpenLayers.Bounds object or the string "automatic"
        // If set to "automatic", the client will ask the server for the default extent.
        defaultExtent: "automatic",
     
        // These options are passed directly to the OpenLayers.Map constructor.
        options : {
            projection        : new OpenLayers.Projection("EPSG:900913"),
            displayProjection : new OpenLayers.Projection("EPSG:4326"),
            numZoomLevels: 20,
            controls: []
        },

        // Instead of specifying just the base layer options, you can instead
        // specify the full base layer object.
        // If only one layer is defined in the baseLayer array, the layer switcher is disabled.
        // If there are several layers in the baseLayer array, the layer switcher is enabled and the first layer in the array becomes the default layer
        baseLayer: [
           // MapBox Streets Layer
           new OpenLayers.Layer.OSM(
               "Mapbox Streets", [
                   "http://a.tiles.mapbox.com/v3/mapbox.mapbox-streets/${z}/${x}/${y}.png",
                   "http://b.tiles.mapbox.com/v3/mapbox.mapbox-streets/${z}/${x}/${y}.png",
                   "http://c.tiles.mapbox.com/v3/mapbox.mapbox-streets/${z}/${x}/${y}.png",
                   "http://d.tiles.mapbox.com/v3/mapbox.mapbox-streets/${z}/${x}/${y}.png"
               ],
               {
                   numZoomLevels: 18,
                   attribution:"Data<a href='http://creativecommons.org/licenses/by-sa/2.0/' target='_blank'> CC-BY-SA </a>" +
                   "by<a href='http://openstreetmap.org/' target='_blank'> OpenStreetMap.</a> " +
                   "Tiles from<a href='http://mapbox.com/about/maps' target='_blank'> MapBox Streets.</a>"
               }
           ),
           // Regular Open Street Map server
           new OpenLayers.Layer.OSM(
               "Open Street Map"
           ),
           // Cycle map tiles
           new OpenLayers.Layer.OSM(
               "Open Cycle Map", [
                   "http://a.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png",
                   "http://b.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png",
                   "http://c.tile.opencyclemap.org/cycle/${z}/${x}/${y}.png"
                ],
                {
                    numZoomLevels: 17,
                    attribution:"Data <a href='http://creativecommons.org/licenses/by-sa/2.0/'> CC-BY-SA</a> by <a href='www.opencyclemap.org'>OpenCycleMap </a> and <a href='http://openstreetmap.org/'> Open Street Map</a>"
                }
           ),
           // here's the MapQuest baseMap option for basemap tiles
           new OpenLayers.Layer.OSM(
               "OSM MapQuest",[
                   "http://otile1.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                   "http://otile2.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                   "http://otile3.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png",
                   "http://otile4.mqcdn.com/tiles/1.0.0/osm/${z}/${x}/${y}.png"
               ],
               {
                   sphericalMecator : true,
                   isBaseLayer      : true,
                   numZoomLevels    : 19,
                   attribution:"Data <a href='http://creativecommons.org/licenses/by-sa/2.0/'> CC-BY-SA </a> by  <a href='http://openstreetmap.org/'> OpenStreetMap</a>."
                   +" Tiles courtesy of <a href='http://open.mapquest.com/' target='_blank'>MapQuest</a>"
               }
           )
        ],

        // NOTE: this object is ignored if a baseLayer (which is an instance of OpenLayers.Layer)
        // config object used in the creation of a new base layer for the map.
        baseLayerOptions: {
            projection : new OpenLayers.Projection("EPSG:4326"),
            url        : 'http://maps.opengeo.org/geowebcache/service/wms',
            layers     : ['openstreetmap'],
            format     : 'image/png',
            transitionEffect: 'resize'
        }
    },

    // when enabled, adds another item to the accordion for attribution
    attributionPanel : {
        enabled         : false,
        panelTitle      : otp.config.locale.config.attribution.title,
        attributionHtml : '<p class="disclaimer">' + otp.config.locale.config.attribution.content + '</p>'
    },

    // presents a dialog on initial startup of the app, with a message for your customers
    splashScreen : {
        enabled: false,
        timeout: 20,   // seconds to stay open - if <= ZERO, then dialog does not timeout and requires the customer to close the dialog
        title:   'Important: Please read',
        html:    '<p class="splash-screen">'
                 + 'Please note that the trip routing presented here is for demonstration purposes of the <a href="http://opentripplanner.com" target="#">OpenTripPlanner (OTP)</a> only, '
                 + 'and not intended as a travel resource.  You will begin to see improvements in the planned trips as the project matures.  A public beta is scheduled for spring 2011. '
                 + '</p>'
    },

    systemMap : {
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

    // if specified, uri path to a custom logo otherwise use the default "images/ui/logoSmall.png"
    logo: null,

    // List of agency IDs (as specified in GTFS) for which custom icons should be used.
    // Icons should be placed in the custom directory (e.g., custom/nyct)
    // Ex. ['nyct', 'mnr']
    useCustomIconsForAgencies : [],

    // Context menu with trip planning options (e.g., "Start trip here")
    plannerContextMenu : true,

    // Context menu with general map features (e.g., "Center map here")
    mapContextMenu : true,

    CLASS_NAME : "otp.config"
};
try {
    // step 3: apply our default to the existing (possibly empty) otp config
    otp.inherit(otp.config, otp.config_defaults);       // step 3a: build the object up
    otp.configure(otp.config, otp.config_defaults);     // step 3b: make sure any / all local changes above get applied
    console.log("otp.config updated with default items from otp.config_static");
} catch(e) {
    console.log("ERROR: was unable to run otp.inherid override in config.js - got this exception: " + e);
}
