otp.config = {
    //If enabled it shows popup window with all planner responses in JSON
    //Can be also enabled in URL parameters as ?debug=true
    debug: false,
    //If enabled it shows inspector layers overlays which can be used for Graph
    //debugging
    //Can be also enabled in URL parameters as ?debug_layers=true
    debug_layers: true,

    //This is default locale when wanted locale isn't found
    //Locale language is set based on wanted language in url >
    //user cookie > language set in browser (Not accept-language) 
    locale: otp.locale.English,

    //All avalible locales
    //key is translation name. Must be the same as po file or .json file
    //value is name of settings file for localization in locale subfolder
    //File should be loaded in index.html
    locales : {
        'ca_ES': otp.locale.Catalan,
        'de': otp.locale.German,
        'en': otp.locale.English,
        'es': otp.locale.Spanish,
        'fr': otp.locale.French,
        'hu': otp.locale.Hungarian,
        'it': otp.locale.Italian,
        'no': otp.locale.Norwegian,
        'pl': otp.locale.Polish,
        'pt': otp.locale.Portuguese,
        'sl': otp.locale.Slovenian
    },

    languageChooser : function() {
        var active_locales = _.values(otp.config.locales);
        var str = "<ul>";
        var localesLength = active_locales.length;
        var param_name = i18n.options.detectLngQS;
        for (var i = 0; i < localesLength; i++) {
            var current_locale = active_locales[i];
            var url_param = {};
            url_param[param_name] = current_locale.config.locale_short;
            str += '<li><a href="?' + $.param(url_param) + '">' + current_locale.config.name + ' (' + current_locale.config.locale_short + ')</a></li>';
        }
        str += "</ul>";
        return str;
    },


    /**
     * The OTP web service locations
     */
    hostname : "",
    //hostname : "http://localhost:8080",
    //municoderHostname : "http://localhost:8080",
    //datastoreUrl : 'http://localhost:9000',
    // In the 0.10.x API the base path is "otp-rest-servlet/ws"
    // From 0.11.x onward the routerId is a required part of the base path.
    // If using a servlet container, the OTP WAR should be deployed to context path /otp
    restService: "otp/routers/default",

    /**
     * Base layers: the base map tile layers available for use by all modules.
     * Expressed as an array of objects, where each object has the following 
     * fields:
     *   - name: <string> a unique name for this layer, used for both display
     *       and internal reference purposes
     *   - tileUrl: <string> the map tile service address (typically of the
     *       format 'http://{s}.yourdomain.com/.../{z}/{x}/{y}.png')
     *   - attribution: <string> the attribution text for the map tile data
     *   - [subdomains]: <array of strings> a list of tileUrl subdomains, if
     *       applicable
     *       
     */

    baseLayers: [
        {
            name: 'Stamen Terrain',
            tileUrl: 'http://tile.stamen.com/terrain/{z}/{x}/{y}.png',
            attribution : 'Map tiles by <a href="http://stamen.com">Stamen Design</a>, under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a href="http://openstreetmap.org">OpenStreetMap</a>, under <a href="http://www.openstreetmap.org/copyright">ODbL</a>.',
            maxZoom: 22,
            maxNativeZoom: 18
        },
        {
            name: 'Carto Positron',
            tileUrl: 'http://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
            attribution : 'Map tiles by Carto/MapZen. Map data by <a href="http://openstreetmap.org">OpenStreetMap</a>, under <a href="http://www.openstreetmap.org/copyright">ODbL</a>.',
            maxZoom: 22,
            maxNativeZoom: 22
        },
        {
            name: 'Transport Tiles',
            tileUrl: 'http://{s}.tile.thunderforest.com/transport/{z}/{x}/{y}.png',
            subdomains : ['a','b','c'],
            attribution: 'Data from <a href="http://www.openstreetmap.org/" target="_blank">OpenStreetMap</a> and contributors. Tiles from <a href="http://www.thunderforest.com/transport/">Andy Allan</a>',
            maxZoom: 22,
            maxNativeZoom: 22
        },
        {
            name: 'Stamen Toner Lite',
            tileUrl: 'http://tile.stamen.com/toner-lite/{z}/{x}/{y}.png',
            attribution : 'Map tiles by <a href="http://stamen.com">Stamen Design</a>, under <a href="http://creativecommons.org/licenses/by/3.0">CC BY 3.0</a>. Data by <a href="http://openstreetmap.org">OpenStreetMap</a>, under <a href="http://www.openstreetmap.org/copyright">ODbL</a>.',
            maxZoom: 22,
            maxNativeZoom: 20
        },
        {
            name: 'Carto Dark Matter',
            tileUrl: 'http://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
            attribution : 'Map tiles by Carto/MapZen. Map data by <a href="http://openstreetmap.org">OpenStreetMap</a>, under <a href="http://www.openstreetmap.org/copyright">ODbL</a>.',
            maxZoom: 22,
            maxNativeZoom: 22
        },
        {
            name: 'OSM Standard Tiles',
            tileUrl: 'https://a.tile.openstreetmap.org/{z}/{x}/{y}.png',
            attribution : 'Map data and tiles © OpenStreetMap contributors',
            isDefault: true,
            maxZoom: 22,
            maxNativeZoom: 19
        }
    ],
    

    /**
     * Map start location and zoom settings: by default, the client uses the
     * OTP routerInfo API call to center and zoom the map. The following
     * properties, when set, override that behavioir.
     */
     
    // initLatLng : new L.LatLng(<lat>, <lng>),
    // initZoom : 14,
    // minZoom : 10,
    // maxZoom : 20,
    
    /* Whether the map should be moved to contain the full itinerary when a result is received. */
    zoomToFitResults    : false,

    /**
     * Site name / description / branding display options
     */

    siteName            : "My OTP Instance",
    siteDescription     : "An OpenTripPlanner deployment.",
    logoGraphic         : 'images/otp_logo_darkbg_40px.png',
    // bikeshareName    : "",
    //Enable this if you want to show frontend language chooser
    showLanguageChooser : true,

    showLogo            : true,
    showTitle           : true,
    showModuleSelector  : true,
    metric              : false,


    /**
     * Modules: a list of the client modules to be loaded at startup. Expressed
     * as an array of objects, where each object has the following fields:
     *   - id: <string> a unique identifier for this module
     *   - className: <string> the name of the main class for this module; class
     *       must extend otp.modules.Module
     *   - [defaultBaseLayer] : <string> the name of the map tile base layer to
     *       used by default for this module
     *   - [isDefault]: <boolean> whether this module is shown by default;
     *       should only be 'true' for one module
     */
    
    modules : [
        {
            id : 'planner',
            className : 'otp.modules.multimodal.MultimodalPlannerModule',
            isDefault: true
        }
    ],
    
    
    /**
     * Geocoders: a list of supported geocoding services available for use in
     * address resolution. Expressed as an array of objects, where each object
     * has the following fields:
     *   - name: <string> the name of the service to be displayed to the user
     *   - className: <string> the name of the class that implements this service
     *   - url: <string> the location of the service's API endpoint
     *   - addressParam: <string> the name of the API parameter used to pass in
     *       the user-specifed address string
     */

    geocoders : [
        {
            name: 'OTP built-in geocoder',
            className: 'otp.core.GeocoderBuiltin'
            // URL and query parameter do not need to be set for built-in geocoder.
        }
    ],

    

    //This is shown if showLanguageChooser is true
    infoWidgetLangChooser : {
        title: '<img src="/images/language_icon.svg" onerror="this.onerror=\'\';this.src=\'/images/language_icon.png\'" width="30px" height="30px"/>', 
        languages: true
    },
    
    
    /**
     * Support for the "AddThis" display for sharing to social media sites, etc.
     */
     
    showAddThis     : false,
    //addThisPubId    : 'your-addthis-id',
    //addThisTitle    : 'Your title for AddThis sharing messages',


    /**
     * Formats to use for date and time displays, expressed as ISO-8601 strings.
     */    
     
    timeFormat  : "h:mma",
    dateFormat  : "MMM Do YYYY",
    apiTimeFormat : "h:mma",
    apiDateFormat  : "MM-DD-YYYY"
};
var options = {
	resGetPath: 'js/otp/locale/__lng__.json',
	fallbackLng: 'en',
        nsseparator: ';;', //Fixes problem when : is in translation text
        keyseparator: '_|_',
	preload: ['en'],
        //TODO: Language choosing works only with this disabled
        /*lng: otp.config.locale_short,*/
        /*postProcess: 'add_nekaj', //Adds | around every string that is translated*/
        /*shortcutFunction: 'sprintf',*/
        /*postProcess: 'sprintf',*/
	debug: true,
	getAsync: false, //TODO: make async
	fallbackOnEmpty: true,
};
var _tr = null; //key
var ngettext = null; // singular, plural, value
var pgettext = null; // context, key
var npgettext = null; // context, singular, plural, value

i18n.addPostProcessor('add_nekaj', function(val, key, opts) {
    return "|"+val+"|";
});

i18n.init(options, function(t) {
    //Sets locale and metric based on currently selected/detected language
    if (i18n.lng() in otp.config.locales) {
        otp.config.locale = otp.config.locales[i18n.lng()];
        otp.config.metric = otp.config.locale.config.metric;
        //Conditionally load datepicker-lang.js?
    } 

    //Use infoWidgets from locale
    //Default locale is English which has infoWidgets
    if ("infoWidgets" in otp.config.locale) {
        otp.config.infoWidgets=otp.config.locale.infoWidgets;
    } else {
        otp.config.infoWidgets=otp.locale.English.infoWidgets;
    }

    if (otp.config.showLanguageChooser) {
        otp.config.infoWidgets.push(otp.config.infoWidgetLangChooser);
    }
    //Accepts Key, value or key, value1 ... valuen
    //Key is string to be translated
    //Value is used for sprintf parameter values
    //http://www.diveintojavascript.com/projects/javascript-sprintf
    //Value is optional and can be one parameter as javascript object if key
    //has named parameters
    //Or can be multiple parameters if used as positional sprintf parameters
    _tr = function() {
        var arg_length = arguments.length;
        //Only key
        if (arg_length == 1) {
            key = arguments[0];
            return t(key); 
        //key with sprintf values
        } else if (arg_length > 1) {
            key = arguments[0];
            values = [];
            for(var i = 1; i < arg_length; i++) {
                values.push(arguments[i]);
            }
            return t(key, {postProcess: 'sprintf', sprintf: values}); 
        } else {
            console.error("_tr function doesn't have an argument");
            return "";
        }
    };
    ngettext = function(singular, plural, value) {
        return t(singular, {count: value, postProcess: 'sprintf', sprintf: [value]});
    };
    pgettext = function(context, key) {
        return t(key, {context: context});
    };
    npgettext = function(context, singular, plural, value) {
        return t(singular, {context: context,
                 count: value,
                 postProcess: 'sprintf',
                 sprintf: [value]});
    };

});

otp.config.modes = {
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "TRANSIT,WALK"             : _tr("Transit"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "BUS,WALK"                 : _tr("Bus Only"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "TRAM,RAIL,SUBWAY,FUNICULAR,GONDOLA,WALK": _tr("Rail Only"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "AIRPLANE,WALK"            : _tr("Airplane Only"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "BUS,TRAM,RAIL,FERRY,SUBWAY,FUNICULAR,GONDOLA,WALK" : _tr("Transit, No Airplane"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "BICYCLE"                  : _tr('Bicycle Only'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "TRANSIT,BICYCLE"          : _tr("Bicycle &amp; Transit"),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "WALK"                     : _tr('Walk Only'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "CAR"                      : _tr('Car Only'),
    "CAR_PICKUP"               : _tr('Taxi'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "CAR_PARK,TRANSIT"         : _tr('Park and Ride'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "CAR_PICKUP,TRANSIT"       : _tr('Ride and Kiss (Car Pickup)'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "CAR_DROPOFF,TRANSIT"      : _tr('Kiss and Ride (Car Dropoff)'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "BICYCLE_PARK,TRANSIT"     : _tr('Bike and Ride'),
    //uncomment only if bike rental exists in a map
    // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
    //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
    // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    'BICYCLE_RENT'             : _tr('Rented Bicycle'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    'TRANSIT,BICYCLE_RENT'     : _tr('Transit & Rented Bicycle'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    'SCOOTER_RENT'             : _tr('Rented Scooter'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    'TRANSIT,SCOOTER_RENT'     : _tr('Transit & Rented Scooter'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "FLEX_ACCESS,WALK,TRANSIT" : _tr('Transit with flex access'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "FLEX_EGRESS,WALK,TRANSIT" : _tr('Transit with flex egress'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "FLEX_ACCESS,FLEX_EGRESS,TRANSIT" : _tr('Transit with flex access and egress'),
    //TRANSLATORS: Travel by: mode of transport (Used in selection in Travel Options widgets)
    "FLEX_DIRECT"              : _tr('Direct flex search'),
    "CARPOOL,WALK"             : _tr("Carpool"),
    "CAR_HAIL,TRANSIT,WALK"    : _tr("Car hailing and transit")
};

let limitTo = _tr("Limit to ");

otp.config.debugItinerarys = {
    "OFF"      : _tr('Off'),
    "LIST_ALL" : _tr('Show all'),
    "LIMIT_TO_NUM_OF_ITINERARIES" : limitTo + '<code>numItineraries</code>',
    "LIMIT_TO_SEARCH_WINDOW" : limitTo + '<code>searchWindow</code>'
};
