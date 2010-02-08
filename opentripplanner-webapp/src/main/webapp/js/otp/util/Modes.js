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
otp.util.Modes = {

    WALK        : 'WALK', 
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

    transitModes : null,

    /** IMPORTANT: always call init prior to using any other methods (or value arrays) */
    init : function()
    {
        if(this.transitModes == null)
            this.transitModes = [this.TRAM, this.SUBWAY, this.BUS, this.RAIL, this.GONDOLA, this.FERRY, this.CABLE_CAR, this.FUNICULAR, this.BUSISH, this.TRANSIT, this.TRAINISH];

    },

    isTransit : function(mode)
    {
        this.init();

        var retVal = false;
        for(var i = 0; i < this.transitModes.length; i++)
        {
            var m = this.transitModes[i];
            if(mode == m || mode.toUpperCase() == m)
            {
                retVal = true;
                break;
            }
        }

        return retVal;
    },

    CLASS_NAME       : "otp.util.Modes"
};
