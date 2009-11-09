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
    getCoordinate : function(coord, defVal)
    {
        var retVal = '0.0,0.0';
        if(defVal != null) retVal = defVal;

        try
        {
            var c = coord.split(",");
            var X = c[0].trim();
            var Y = c[1].trim();
            if(X >= 0.0 && X < 100000000.0 && Y >= 0.0 && Y < 100000000.0)
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