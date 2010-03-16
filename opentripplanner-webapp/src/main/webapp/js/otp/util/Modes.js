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
otp.util.Modes = (function() {
    
    var modes = {
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
            BUSISH      : 'BUSISH'
    };
    
    var transitModes = [modes.TRAM, modes.SUBWAY, modes.BUS, modes.RAIL, modes.GONDOLA, modes.FERRY, modes.CABLE_CAR, modes.FUNICULAR, modes.BUSISH, modes.TRANSIT, modes.TRAINISH];
    
    return {
        isTransit : function(mode)
        {
            var retVal = false;
            for(var i = 0; i < transitModes.length; i++)
            {
                var m = transitModes[i];
                if(mode == m || mode.toUpperCase() == m)
                {
                    retVal = true;
                    break;
                }
            }

            return retVal;
        }
    };
})();
