/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

otp.namespace("otp.locale");

/**
 * 
 */
otp.locale.French = {

    analyst : {

        differentOrigin : "Origine différente",
        refresh : "Rafraîchir",
        modesLabel : "Modes",
        inheritValue : "(identique)",
        walkLabel : "Marche max. / vitesse",
        bikeLabel : "Vitesse vélo",
        maxTimeLabel : "Durée max.",
        dataTypeLabel : "Donnée à afficher",

        arriveDepart : [ [ 'false', 'Partir à' ], [ 'true', 'Arriver à' ] ],

        maxWalkDistance : [ [ '200', '200 m' ], [ '500', '500 m' ], [ '750', '750 m' ], [ '1000', '1 km' ],
                [ '1500', '1,5 km' ], [ '2000', '2 km' ], [ '3000', '3 km' ], [ '4000', '4 km' ], [ '5000', '5 km' ] ],

        walkSpeed : [ [ '0.278', '1 km/h' ], [ '0.556', '2 km/h' ], [ '0.833', '3 km/h' ], [ '1.111', '4 km/h' ],
                [ '1.389', '5 km/h' ], [ '1.667', '6 km/h' ], [ '1.944', '7 km/h' ], [ '2.222', '8 km/h' ],
                [ '2.500', '9 km/h' ], [ '2.778', '10 km/h' ] ],

        bikeSpeed : [ [ '2.778', '10 km/h' ], [ '3.333', '12 km/h' ], [ '4.167', '15 km/h' ], [ '5.556', '20 km/h' ] ],

        modes : [ [ "TRANSIT,WALK", "Transport en commun" ], [ "BICYCLE", "Vélo" ], [ "WALK", "Marche à pied" ],
                [ "CAR", "Voiture" ], [ "CAR_PARK,WALK,TRANSIT", "P+R (Voiture puis TC)" ] ],

        transitModes : [ [ "BUS", "Bus/Car" ], [ "TRAM", "Tramway" ], [ "SUBWAY", "Métro" ], [ "RAIL", "Train" ] ],

        maxTime : [ [ "1800", "0:30" ], [ "2700", "0:45" ], [ "3600", "1:00" ], [ "5400", "1:30" ], [ "7200", "2:00" ],
                [ "9000", "2:30" ], [ "10800", "3:00" ] ],

        dataType : [ [ "TIME", "Temps de parcours" ], [ "BOARDINGS", "Nb d'embarquements" ],
                [ "WALK_DISTANCE", "Distance de marche" ] ],
    },

    CLASS_NAME : "otp.locale.French"
};
