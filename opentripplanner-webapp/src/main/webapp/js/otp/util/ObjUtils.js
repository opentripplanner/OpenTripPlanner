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

// TODO -- clean this up...there are StringUtils and DateUtils here...

/**
 * Utility routines for operating on unknown objects
 */
otp.util.ObjUtils = {

    m_includeNameAlways    : false,
    m_includeNameForSmalls : true,
    m_smallStringLength    : 4,
    m_fixNameSpacing       : true,
    m_capitolize           : true,

    /**
     * predicate to check if string looks like a lat,lon string
     */
    isCoordinate : function(value) {
        var lat, lon;

        try
        {
            lat = parseFloat(otp.util.ObjUtils.getLat(value));
            lon = parseFloat(otp.util.ObjUtils.getLon(value));
        }
        catch(e)
        {
        }

        return !isNaN(lat) && !isNaN(lon);
    },

    isNumber : function(value)
    {
        var retVal = false;
        try
        {
            if(value)
                retVal = !isNaN(value - 111);
        }
        catch(e)
        {
        }

        return retVal; 
    },


    /** returns the second value from a comma separated string eg: 0.0,0.returnMe*/
    getLon : function(coord) {
        var retVal = null;

        try
        {
            retVal = coord.substring(coord.indexOf(',') + 1); 
        }
        catch(e)
        {
        }

        return retVal;
    },

    /** returns the first value from a comma separated string eg: 0.returnMe,0.0 */
    getLat : function(coord) {
        var retVal = null;

        try
        {
            retVal = coord.substring(0, coord.indexOf(',')); 
        }
        catch(e)
        {
        }

        return retVal;
    },

    /**  */
    getConfig : function(config)
    {
        var retVal = otp.config;
        if(config != null && config.map != null)
            retVal = config;
        return retVal;
    },

    /** string & sub-string search of an array (slightly different than array.indexOf) */
    isInArray : function (str, array)
    {
        var retVal = false;
        try
        {
            for(var i = 0; i < array.length; i++)
            {
                var m = array[i];
                if(str.indexOf(m) >= 0 )
                {
                    retVal = true;
                    break;
                }
            }
        }
        catch(e)
        {
            console.log("isInArray error" + e);
        }

        return retVal;
     },


    /** */
    isArray : function (obj)
    {
        var retVal = false;
        try
        {
            if(obj.constructor.toString().indexOf("Array") == -1)
               retVal = false;
            else
               retVal = true;
        }
        catch(e)
        {}

        return retVal;
     },

    /** */
    deleteFromArray : function (arr, val, all) 
    {
        var retVal = arr;
        try
        {
            if(this.isArray(arr))
            {
                retVal = new Array();
                var cullVal = true;
                for(var i = 0; i < arr.length; i++)
                {
                    if(arr[i] == val && cullVal)
                    {
                        // if we're only culling the first, set our flag
                        if(!all)
                            cullVal = false;

                        // move to next value without copying to output array
                        continue;
                    }
                    // add this value (!= val) to return array 
                    retVal.push(arr[i]);
                } 
            }
        }
        catch(e)
        {}

        return retVal;
     },
    

    /** */
    getBottomArray : function(arr, maxDepth)
    {
        var retVal = null;
        var depth  = 10;
        if(maxDepth)
            depth = maxDepth;

        try
        {
            var az = arr;
            for(var i =  0; i < depth; i++)
            {
                if(!this.isArray(az[0]))
                {
                    retVal = az;
                    break;
                }
                az = az[0];
            }
        }
        catch(e)
        {}

        return retVal;
    },

    /** controller has a callback interface */ 
    defaultController : function(controller)
    {
        if(controller            == null) controller = {};
        if(controller.activate   == null) controller.activate   = function(){};
        if(controller.deactivate == null) controller.deactivate = function(){};

        return controller;
    },

    /** */ 
    fixWord : function(name)
    {
        var retVal = name;

        try
        {
            var retVal = retVal.trim();
            if(this.m_fixNameSpacing) 
                retVal = retVal.replace('_', ' ');
            
            if(this.m_capitolize)
            {
                var tmp = retVal.charAt(0).toUpperCase();
                if(retVal.length > 1)
                    tmp += retVal.slice(1).toLowerCase();
            
                retVal = tmp;
            }
        }
        catch(e)
        {
        }

        return retVal;
    },


    /** */
    toFloat : function(num, defVal)
    {
        var retVal = defVal;
        try
        {
            if(num)
            {
                retVal = num * 0.999999999;
                if(isNaN(retVal))
                    retVal = defVal;
            }
        }
        catch(e)
        {
            retVal = defVal;
        }

        return retVal;
    },

    /** */
    getCoordinate : function(coord, defVal)
    {
        var retVal = '0.0,0.0';
        if(defVal != null) retVal = defVal;

        try
        {
            var c = coord.split(",");
            var X = c[0].trim();
            var Y = c[1].trim();
            if(X >= -180.0 && X < 100000000.0 && Y >= -180.0 && Y < 100000000.0)
                retVal = X + "," + Y;
        }
        catch(e)
        {
            console.log("otp.util.getCoordinate error for " + coord + "; exception " + e);
        }

        return retVal;
    },

    /** will fix a name/value pair, by appending the name to a returned value
     *  see the m_ member variable settings above for rules
     */
    fixNameValue : function(nv)
    {
        var retVal = nv.v + "";

        // only append the attribute name if we're told to do so (or told to do so for small strings)
        if(this.m_includeNameAlways || (this.m_includeNameForSmalls && retVal.length <= this.m_smallStringLength))
        {
            var name = this.fixWord(nv.n);
            retVal = name + ' = ' + retVal;
        }
        
        return retVal;
    },

    /** gets the first non-null property */
    getFirstAttribute : function(obj, defVal)
    {
        var retVal = defVal;
        for(var name in obj)
        {
            retVal = this.fixNameValue({n: name, v: obj[name]});
        if(retVal != null && retVal.length > 0)
                break;
        }

        return retVal;
    },

    /** gets the first non-null property */
    getNamedAttribute : function(obj, target, defVal)
    {
        var retVal = defVal;

        for(var name in obj)
        {
            if(name != null && name == target)
            {
                // OK, we found the target in the data object
                var tmp = obj[name];
                
                // let's make sure it's not blank
                if(tmp != null && tmp.length > 0)
                {
                    // OK, not blank...clean string up and then assign it to retVal
                    tmp = this.fixNameValue({n: name, v: tmp});
                    if(tmp != null && tmp.length > 0)
                        retVal = tmp;
                }
                break;
            }
        }

        return retVal;
    },

    /** return a <name>::<coord> from an object with .name, .x, .y, .lat and .lon elements */
    getNamedCoordRecord : function(data, isMercator)
    {
        var retVal = {};

        var X='x'; var Y='y';
        retVal.zoom = 6;
        retVal.isMercator = isMercator;
        if(isMercator)
        {
            X='lon';
            Y='lat';
            retVal.zoom = 15;
        }

        // handle the <name>:: part of the coord
        retVal.coord = '';
        if(data['name'] && data['name'].length > 0)
            retVal.coord =  data['name'] + '::';
        retVal.name = data['name'] || '';

        // do the X,Y part of the named coord
        retVal.x = data[X]
        retVal.y = data[Y], 
        retVal.coord += retVal.x + "," + retVal.y;

        return retVal;
    },


    /** gets the number of properties */
    numProperties : function(feature, defVal) 
    {
        var i = 0;
        for(var name in properties)
        {
            i++;
        }

        return i;
    },

    CLASS_NAME : "otp.util.ObjUtils"
};