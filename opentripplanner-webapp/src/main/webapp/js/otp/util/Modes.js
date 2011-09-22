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

    getTransitModes : function()
    {
        return [this.TRAM, this.SUBWAY, this.BUS, this.RAIL, this.GONDOLA, this.FERRY, this.CABLE_CAR, this.FUNICULAR, this.TRANSIT, this.TRAINISH, this.BUSISH];
    },

    isTransit : function(mode)
    {
        if(mode == null) return false;
        return otp.util.ObjUtils.isInArray(mode.toUpperCase(), this.getTransitModes());
    },


    getTrainModes : function()
    {
        return [this.TRAM, this.SUBWAY, this.RAIL, this.GONDOLA, this.CABLE_CAR, this.FUNICULAR, this.TRAINISH];
    },

    isTrain : function(mode)
    {
        if(mode == null) return false;
        return otp.util.ObjUtils.isInArray(mode.toUpperCase(), this.getTrainModes());
    },

    getBusModes : function()
    {
        return [this.BUS, this.BUSISH];
    },

    isBus : function(mode)
    {
        if(mode == null) return false;
        return otp.util.ObjUtils.isInArray(mode.toUpperCase(), this.getBusModes());
    },

    getBicycleModes : function()
    {
        return [this.BICYCLE, "BIKE"];
    },

    isBikeAndTransit : function(mode)
    {
        if(mode == null) return false;

        var hasBike    = false;
        var hasTransit = false;

        var keys = null;
        if(mode.indexOf(','))
            keys = mode.split(',');
         else
            keys = mode.split('_');

         if(keys)
         {
             for (var i = 0; i < array.length; i++)
             {
                 var m = array[i];
                 if (otp.util.ObjUtils.isInArray(mode.toUpperCase(), this.getTransitModes())) 
                    hasTransit = true;
                 if (otp.util.ObjUtils.isInArray(mode.toUpperCase(), this.getBicycleModes())) 
                    hasBike = true;
             }
         }

         var retVal = hasBike && hasTransit;
         return retVal;
    },

    /** return mode name from locale */
    translate : function(mode, locale)
    {
        if(mode == null) return mode;
        if(locale == null)
            locale = otp.config.locale;

        var retVal = mode;
        try {
            retVal = locale.modes[mode];
        } catch(e) {
        }

        return retVal;
    },


    CLASS_NAME : "otp.util.Modes"
};
