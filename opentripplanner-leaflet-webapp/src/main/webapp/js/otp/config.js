// step 1: make sure we have some type of otp.config, and otp.config.locale defined
if(typeof(otp) == "undefined" || otp == null) otp = {};
if(typeof(otp.config) == "undefined" || otp.config == null) otp.config = {};
//if(typeof(otp.config.locale) == "undefined" || otp.config.locale == null) otp.config.locale = otp.locale.English;


// step 2: create an object of default otp.config default values (see step3 where we apply this to any existing config)
otp.config = {
/*
    // OTP server address and routerId (if applicable)
    hostname : "http://nyc.deployer.opentripplanner.org",
    routerId : "req-241",

    // Base map tiles settings:
    tileUrl : 'http://{s}.tiles.mapbox.com/v3/mapbox.mapbox-streets/{z}/{x}/{y}.png',
    // overlayTileUrl : [link to tileset to overlay on base layer],
    tileAttrib : 'Routing powered by <a href="http://opentripplanner.org/">OpenTripPlanner</a>, Map tiles from MapBox (<a href="http://mapbox.com/about/maps/">terms</a>) and OpenStreetMap ',
    
    // map start location and zoom settings 
    initLatLng : new L.LatLng(40.7195,-74), // (NYC)
    initZoom : 14,
    minZoom : 13,
    maxZoom : 17,*/
    // OTP server address and routerId (if applicable)
    //hostname : "http://otpna-c.deployer.opentripplanner.org",
    //routerId : "req-1028",
    hostname : "http://localhost:8080",
        
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
    ],
    
    // map start location and zoom settings 
    initLatLng : new L.LatLng(45.523307,-122.676086), // portland
    initZoom : 14,
    minZoom : 10,
    maxZoom : 20,

    showLogo:           true,
    showTitle:          true,
    showModuleSelector: true,

    logoGraphic :       'images/openplans-logo-40x40.png',

    siteName    : "My OTP Instance",
    siteURL     : "[link to site]",
    siteDescription  : "An OpenTripPlanner deployment.",
    
    // bikeshareName : "",

    loggerURL : 'http://localhost:9000',
    // dataStorageUrl : '[link]',
            
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

