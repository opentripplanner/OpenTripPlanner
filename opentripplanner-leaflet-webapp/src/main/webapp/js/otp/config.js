// step 1: make sure we have some type of otp.config, and otp.config.locale defined
if(typeof(otp) == "undefined" || otp == null) otp = {};
if(typeof(otp.config) == "undefined" || otp.config == null) otp.config = {};
//if(typeof(otp.config.locale) == "undefined" || otp.config.locale == null) otp.config.locale = otp.locale.English;


// step 2: create an object of default otp.config default values (see step3 where we apply this to any existing config)
otp.config = {

    //hostname : "http://trimet-tomcat.deployer.opentripplanner.org",
    //municoderHostname : "http://trimet-tomcat.deployer.opentripplanner.org",
    
    hostname : "http://localhost:8080",
    //municoderHostname : "http://localhost:8080",
      
    baseLayers: [
        {
            name: 'MapQuest OSM',
            tileUrl: 'http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png',
            subdomains : ['otile1','otile2','otile3','otile4'],
            attribution : 'Data, imagery and map information provided by <a href="http://open.mapquest.com" target="_blank">MapQuest</a>, <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors.'
        },
        {
            name: 'MapQuest Aerial',
            tileUrl: 'http://{s}.mqcdn.com/tiles/1.0.0/sat/{z}/{x}/{y}.png',
            subdomains : ['otile1','otile2','otile3','otile4'],
            attribution : 'Data, imagery and map information provided by <a href="http://open.mapquest.com" target="_blank">MapQuest</a>, <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors.'
        },           
        {
            name: 'MapBox Light',
            tileUrl: 'http://{s}.tiles.mapbox.com/v3/demory.map-hmr94f0d/{z}/{x}/{y}.png',
            //subdomains : ['otile1','otile2','otile3','otile4'],
            attribution : 'Data, imagery and map information provided by <a href="http://www.mapbox.com" target="_blank">MapBox</a>, <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors.'
        },           
        
    ],
    
    // map start location and zoom settings 
    initLatLng : new L.LatLng(45.523307,-122.676086), // portland
    initZoom : 14,
    minZoom : 10,
    maxZoom : 20,

    showLogo:           true,
    showTitle:          true,
    showModuleSelector: true,

    logoGraphic :       'images/otp_logo_darkbg_40px.png',

    siteName    : "My OTP Instance",
    siteURL     : "[link to site]",
    siteDescription  : "An OpenTripPlanner deployment.",
    
    // bikeshareName : "",

    //loggerURL : 'http://trimet-logger.deployer.opentripplanner.org',
    loggerURL : 'http://localhost:9000',
    // dataStorageUrl : '[link]',
    
    modules : [
        {
            className : 'otp.modules.calltaker.CallTakerModule',
            defaultBaseLayer : 'MapQuest OSM'
        },
        {
            className : 'otp.modules.fieldtrip.FieldTripModule',
            defaultBaseLayer : 'MapQuest OSM'
        },
        {
            className : 'otp.modules.multimodal.MultimodalPlannerModule',
            defaultBaseLayer : 'MapQuest OSM',
            isDefault: true
        },
        {
            className : 'otp.modules.analyst.AnalystModule',
            defaultBaseLayer : 'MapBox Light'
        }
    ],
    
    geocoders : [
        {
            name : 'TriMet Default',
            className : 'otp.core.Geocoder',
            url : 'http://maps5.trimet.org/geocoder/geocode',
            addressParam : 'address'
        },
        {
            name : 'SOLR',
            className : 'otp.core.SOLRGeocoder',
            url : 'http://maps5.trimet.org/solr/select',
            addressParam : 'q'
        }
    ],
            
    infoWidgets: [
        {
            title: 'About',
            styleId: 'otp-aboutWidget',
            content: '<p>About this site</p>',
        },
        {
            title: 'Contact',
            styleId: 'otp-contactWidget',
            content: '<p>Comments? Contact us at...</p>'
        },           
    ],
    
    showAddThis     : false,
    //addThisPubId    : 'your-addthis-id',
    //addThisTitle    : 'Your title for AddThis sharing messages',
    
    timeFormat  : "h:mma",
    dateFormat  : "MMM. Do YYYY"
};

