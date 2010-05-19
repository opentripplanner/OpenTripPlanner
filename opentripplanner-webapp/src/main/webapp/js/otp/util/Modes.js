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

otp.namespace("otp.util");

/**
 * strings that routing engine uses to define modes
 */
otp.util.Modes =  {

    WALK        : 'WALK',
    BICYCLE     : 'BICYCLE',
    TRAM        : 'TRAM', 
    SUBWAY      : 'SUBWAY',
    RAIL        : 'RAIL', 
    BUS         : 'BUS', 
    CABLE_CAR   : 'CABLE_CAR', 
    GONDOLA     : 'GONDOLA', 
    FERRY       : 'FERRY',
    FUNICULAR   : 'FUNICULAR',
    TRANSIT     : 'TRANSIT',
    TRAINISH    : 'TRAINISH', 
    BUSISH      : 'BUSISH',

    /** return transit */
    transitModes : [this.TRAM, this.SUBWAY, this.BUS, this.RAIL, this.GONDOLA, this.FERRY, this.CABLE_CAR, this.FUNICULAR, this.BUSISH, this.TRANSIT, this.TRAINISH],
    isTransit : function(mode) {
        var retVal = false;

        if(mode != null) {
            for(var i = 0; i < this.transitModes.length; i++) {
                var m = this.transitModes[i];
                if(mode.toUpperCase().indexOf(m) >= 0 ) {
                    retVal = true;
                    break;
                }
            }
        }

        return retVal;
    },

    CLASS_NAME : "otp.util.Modes"
};
