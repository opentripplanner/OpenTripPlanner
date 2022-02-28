otp.namespace("otp.locale");

/**
 *
 */
otp.locale.Catalan = {

    analyst : {

        differentOrigin : "Origen diferent",
        refresh : "Actualitzar",
        modes : "Modes",
        inheritValue : "(idèntic)",
        walkLabel : "Caminar max. / velocitat",
        bikeLabel : "Velocitat en bici",
        maxTimeLabel : "Durada màxima",
        dataTypeLabel : "Dades per mostrar",

        arriveDepart : [ [ 'false', 'Anar a' ], [ 'true', 'Arribar a' ] ],

        maxWalkDistance : [ [ '200', '200 m' ], [ '500', '500 m' ], [ '750', '750 m' ], [ '1000', '1 km' ],
            [ '1500', '1.5 km' ], [ '2000', '2 km' ], [ '3000', '3 km' ], [ '4000', '4 km' ], [ '5000', '5 km' ] ],

        walkSpeed : [ [ '0.278', '1 km/h' ], [ '0.556', '2 km/h' ], [ '0.833', '3 km/h' ], [ '1.111', '4 km/h' ],
            [ '1.389', '5 km/h' ], [ '1.667', '6 km/h' ], [ '1.944', '7 km/h' ], [ '2.222', '8 km/h' ],
            [ '2.500', '9 km/h' ], [ '2.778', '10 km/h' ] ],

        bikeSpeed : [ [ '2.778', '10 km/h' ], [ '3.333', '12 km/h' ], [ '4.167', '15 km/h' ], [ '5.556', '20 km/h' ] ],

        modes : [ [ "TRANSIT,WALK", "Transport en comú" ], [ "BICYCLE", "Bicicleta" ], [ "WALK", "Caminant" ], [ "CAR", "Cotxe" ],
            [ "CAR_PARK,WALK,TRANSIT", "P+R (Cotxe després amb transport)" ] ],

        transitModes : [ [ "BUS", "Bus" ], [ "TRAM", "Tramvia" ], [ "SUBWAY", "Metro" ], [ "RAIL", "Tren" ] ],

        maxTime : [ [ "1800", "0:30" ], [ "2700", "0:45" ], [ "3600", "1:00" ], [ "5400", "1:30" ], [ "7200", "2:00" ],
            [ "9000", "2:30" ], [ "10800", "3:00" ] ],

        dataType : [ [ "TIME", "Temps de viatge" ], [ "BOARDINGS", "Nombre d'embarcaments" ], [ "WALK_DISTANCE", "Distància caminant" ] ],
    },

    CLASS_NAME : "otp.locale.Catalan"
};
