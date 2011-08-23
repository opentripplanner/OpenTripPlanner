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

otp.namespace("otp.planner");

/**
  * ModeOptions
  * 
  * Contains the stores for vairous travel modes (e.g., 'Safest' and 'Quick' for 
  * BIKE modes; 'Quick', 'No tolls' and 'No Highways' for cars, etc...)
  */
otp.planner.ModeOptions = {

    bicycleStore : null,
    carStore     : null,
    transitStore : null,
    walkStore    : null,
    defaultStore : null,


    /** */
    initialize : function(config)
    {
        otp.configure(this, config);
    },

    /** */
    getStore : function(mode) 
    {
    },

    /** */
    getStoreValue : function(mode, index)
    {
    },

    /** */
    _modeToStore : function(mode, defaultMode)
    {
        var retVal = null;
        try 
        {
            switch (mode.toUpper()) 
            {
                case "TRAM":
                case "SUBWAY":
                case "RAIL":
                case "BUS":
                case "FERRY":
                case "CABLE_CAR":
                case "GONDOLA":
                case "FUNICULAR":
                case "TRANSIT":
                    retVal = this.transitStore;
                    break;
                case "WALK":
                    retVal = this.walkStore;
                    break;
                case "BICYCLE":
                    retVal = this.bicycleStore;
                    break;
                case "CAR":
                    retVal = this.carStore;
                    break;
            }
        } 
        catch (e)
        {
        }

        if(retVal == null)
            retVal = defaultStore;

        return retVal;
    },
    

    CLASS_NAME: "otp.planner.ModeOptions"
};

otp.planner.ModeOptions = new otp.Class(otp.planner.ModeOptions);