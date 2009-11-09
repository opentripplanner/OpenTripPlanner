otp.namespace("otp.planner.poi");

/**
 * @class
 */
otp.planner.poi.Style = {
    green : {
              strokeColor: "#00FF00",
              strokeWidth: 3,
              strokeDashstyle: "dashdot",
              pointRadius: 6,
              pointerEvents: "visiblePainted"
    },

    fromTrip : {
              graphicWidth:     20,
              graphicHeight:    34,
              graphicXOffset:  -10,
              graphicYOffset:  -34,
              externalGraphic: "/images/map/trip/start.png",
              cursor:          "pointer", 
              fillOpacity:     "1.0"
    },
    toTrip : {
              graphicWidth:     20,
              graphicHeight:    34,
              graphicXOffset:  -10,
              graphicYOffset:  -34,
              externalGraphic: "/images/map/trip/end.png",
              cursor:          "pointer", 
              fillOpacity:     "1.0"
    },

    CLASS_NAME: "otp.planner.poi.Style"
};

