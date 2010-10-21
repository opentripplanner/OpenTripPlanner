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
        graphicWidth:     21,
        graphicHeight:    39,
        graphicXOffset:  -11,
        graphicYOffset:  -39,
        externalGraphic: "images/map/trip/start.png",
        cursor:          "pointer", 
        fillOpacity:     "1.0"
    },
    toTrip : {
        graphicWidth:     21,
        graphicHeight:    39,
        graphicXOffset:  -11,
        graphicYOffset:  -39,
        externalGraphic: "images/map/trip/end.png",
        cursor:          "pointer", 
        fillOpacity:     "1.0"
    },

    CLASS_NAME: "otp.planner.poi.Style"
};

