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
        if(mode == null)   return null;
        if(locale == null)
            locale = otp.config.locale;

        var retVal = null;
        try
        {
            retVal = locale.modes[mode.toUpperCase()];
        }
        catch(e)
        {
        }

        if(retVal == null)
            retVal = mode;

        return retVal;
    },


    CLASS_NAME : "otp.util.Modes"
};


/**
 * instance object that retains modes used in this itinerary
 */
otp.util.ItineraryModes = {

    itineraryMessages : null,
    itinerary         : null,

    m_message         : null,
    m_hasWalk         : false,
    m_hasTransit      : false,
    m_hasBike         : false,
    m_hasBus          : false,
    m_hasTrain        : false,


    /**
     * 
     */
    initialize : function(config, itinerary)
    {
        otp.configure(this, config);
        this.itinerary = itinerary;
        this._findModes();
        this._findMessage();
    },

    getMessage : function()
    {
        return this.m_message;
    },

    /** mark different modes this itinerary uses */
    _findModes : function()
    {
        var endIndex = this.itinerary.m_fromStore.getCount();
        for(var i = 0; i < endIndex; i++)
        {
            var from = this.itinerary.m_fromStore.getAt(i);
            var mode = from.get('mode');
            if(otp.util.Modes.isTransit(mode))
            {
                this.m_hasTransit = true;

                if(otp.util.Modes.isBus(mode))
                    this.m_hasBus = true;
                else if(otp.util.Modes.isTrain(mode))
                    this.m_hasTrain = true;
            }
            else if(otp.util.Modes.WALK == mode)
            {
                this.m_hasWalk = true;
            }
            else if(otp.util.Modes.BICYCLE == mode)
            {
                this.m_hasBike = true;
            }
        }
    },

    /** mark different modes this itinerary uses */
    _findMessage : function()
    {
        if(this.itineraryMessages)
        {
            if(this.m_hasTransit && this.m_hasBike && this.itineraryMessages.bicycle_transit)
                this.m_message = this.itineraryMessages.bicycle_transit;
            else if(this.m_hasBike && this.itineraryMessages.bicycle)
                this.m_message = this.itineraryMessages.bicycle;
            else if(this.m_hasTransit && this.itineraryMessages.transit)
            {
                this.m_message = this.itineraryMessages.transit;
                if(this.m_hasBus && this.itineraryMessages.bus)
                    this.m_message = this.itineraryMessages.bus;
                else if(this.m_hasTrain && this.itineraryMessages.train)
                    this.m_message = this.itineraryMessages.train;
            }
            else if(this.m_hasWalk && this.itineraryMessages.walk)
                this.m_message = this.itineraryMessages.walk;
        }
    },

    CLASS_NAME : "otp.util.ItineraryModes"
};
otp.util.ItineraryModes = new otp.Class(otp.util.ItineraryModes);
