otp.namespace("otp.locale");

/**
 *
 */
otp.locale.Spanish = {

    analyst : {

        differentOrigin : "Origen diferente",
        refresh : "Refrescar",
        modes : "Modos",
        inheritValue : "(same)",
        walkLabel : "Distancia máxima a pie / velocidad",
        bikeLabel : "Velocidad en bicicleta",
        maxTimeLabel : "Tiempo máximo",
        dataTypeLabel : "Datos a mostrar",

        arriveDepart : [ [ 'false', 'Salir de' ], [ 'true', 'Llegar a' ] ],

        maxWalkDistance : [ [ '200', '200 m' ], [ '500', '500 m' ], [ '750', '750 m' ], [ '1000', '1 km' ],
            [ '1500', '1.5 km' ], [ '2000', '2 km' ], [ '3000', '3 km' ], [ '4000', '4 km' ], [ '5000', '5 km' ] ],

        walkSpeed : [ [ '0.278', '1 km/h' ], [ '0.556', '2 km/h' ], [ '0.833', '3 km/h' ], [ '1.111', '4 km/h' ],
            [ '1.389', '5 km/h' ], [ '1.667', '6 km/h' ], [ '1.944', '7 km/h' ], [ '2.222', '8 km/h' ],
            [ '2.500', '9 km/h' ], [ '2.778', '10 km/h' ] ],

        bikeSpeed : [ [ '2.778', '10 km/h' ], [ '3.333', '12 km/h' ], [ '4.167', '15 km/h' ], [ '5.556', '20 km/h' ] ],

        modes : [ [ "TRANSIT,WALK", "Transporte publico" ], [ "BICYCLE", "Bicicleta" ], [ "WALK", "Andando" ], [ "CAR", "Coche" ],
            [ "CAR_PARK,WALK,TRANSIT", "P+R (Coche y transporte público)" ] ],

        transitModes : [ [ "BUS", "Autobús" ], [ "TRAM", "Tranvía" ], [ "SUBWAY", "Metro" ], [ "RAIL", "Tren" ] ],

        maxTime : [ [ "1800", "0:30" ], [ "2700", "0:45" ], [ "3600", "1:00" ], [ "5400", "1:30" ], [ "7200", "2:00" ],
            [ "9000", "2:30" ], [ "10800", "3:00" ] ],

        dataType : [ [ "TIME", "Tiempo" ], [ "BOARDINGS", "# de transbordos" ], [ "WALK_DISTANCE", "Distancia andando" ] ],
    },

    CLASS_NAME : "otp.locale.Spanish"
};
