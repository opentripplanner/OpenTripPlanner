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
 * Parses the URL parameters, and if there's an X & Y, we put that point up (along with a popup)
 * @class
 */
otp.util.ParseUrlParams = {

    staticControl : null,
    m_params      : null,
    m_arrays      : null,
    m_url         : null,

    /**
     * 
     */
    initialize : function(config) 
    {
        try
        {
            otp.configure(this, config);

            // TODO -- post a bubble
            this.m_url    = this.getUrl();
            this.m_arrays = this.parseQueryStrToArrays(this.m_url);
            this.m_params = this.arraysToStrings(this.m_arrays, null, true);
        }
        catch(e)
        {
            console.log("ParseUrlParams constructor " + e);
        }
    },


    /**
     * return the value of a given URL parameter 
     */
    getParamValue : function(name, defVal)
    {
        var retVal = defVal;
        try
        {
            if(this.m_params[name])
                retVal = this.m_params[name];
            else if(this.m_params[name.toLowerCase()])
                retVal = this.m_params[name.toLowerCase()];
        }
        catch(e)
        {
            console.log("exception: ", e, this);
        }
        
        return retVal;
    },

    /** */
    setParamValue : function(name, val)
    {
        this.m_params[name] = val;
    },

    /** */
    isDebug : function()
    {
        return this.getParamValue("debug", false);
    },

    /** */
    isFullScreen : function(name, defVal)
    {
        return this.getParamValue("fullScreen", defVal);
    },

    /** */
    hasSubmit : function(name, defVal)
    {
        return this.getParamValue("submit", defVal);
    },

    showSatelliteView : function(name, defVal)
    {
        return this.getParamValue("satellite", defVal);
    },

    /** 
     * select search point to zoom & highlight
     */
    getPoi : function(poi, map)
    {
        var retVal = {};
        try
        {
            var params = this.m_params;
            retVal.x = params.pLon;
            retVal.y = params.pLat;
            retVal.z = params.zoom;
            retVal.t = params.pText;

            if(retVal.x == null)
                retVal.x = params.lon;
            if(retVal.y == null)
                retVal.y = params.lat;

            // 
            var h = retVal;
            if(poi && h.t)
            {
                poi.highlight(h.x, h.y, h.z, h.t);
                h.exists = (h.t != null && h.x != null && h.y != null);
            }
            else if(map)
            {
                otp.util.OpenLayersUtils.setCenter(map, h.x, h.y, h.z);
            }
        }
        catch(e)
        {
            console.log("EXCEPTION: ParseUrlParams.getPoi " + e);
        }

        return retVal;
    },
    

    /**
     * parseQueryStrToStrings
     * will return an object (array) that contains a key/value pair of all params
     * in a URL.  Where the same param occures multiple times, then the first value is assigned.
     * And where there is not value to a param, then 'true' is the value assigned.
     */
    parseQueryStrToStrings: function(q)
    {
        var obj = this.parseQueryStrToArrays(q);
        return otp.util.StringFormattingUtils.arraysToStrings(obj);
    },

    /**
     * parseQueryStr is from:
     *   How to Use a JavaScript Query String Parser
     *   By Joseph K. Myers
     *   http://www.webreference.com/programming/javascript/jkm/
     */
    parseQueryStrToArrays: function(q)
    {
        var i;
        var name;
        var t;

        /* parse the query */
        /* semicolons are nonstandard but we accept them */
        var x = q.replace(/;/g, '&').split('&');
        /* q changes from string version of query to object */
        for (q = {}, i = 0; i < x.length; i++) {
            t = x[i].split('=', 2);
            name = unescape(t[0]);
            if (!q[name]) 
                q[name] = [];
            if (t.length > 1) {
                q[name][q[name].length] = unescape(t[1]);
            }
            /* next two lines are nonstandard */
            else 
                q[name][q[name].length] = true;
        }
        
        return q;
    },


    /**
     * 
     */
    getUrl : function()
    {
        try
        {
            return location.search.substring(1).replace(/\+/g, ' ');
        }
        catch(exp)
        {
        }
        
        return null;
    },

    /**
     * utility to convert z[][] to a[] (made up of z[][i] values)
     */
    arraysToStrings: function(arrays, indx, blankVal)
    {
        var retVal = [];
        var i = 0;
        if(indx != null && indx >= 0 && indx <=100)
            i = indx;

        for(var p in arrays)
        {
            var v = arrays[p][i];
            if(v == "" && blankVal)
                v = blankVal;

            retVal[p] = v; 
            //alert(p + ' : ' + retVal[p]);
        }
        
        return retVal;
    },

    /**
     * Trip Planner URL can be sent in as a url parameter (good for dev/debugging the UI)
     * GET PARAM: purl=/test/file/path.xml
     */
    getPlannerUrl : function(defVal)
    {
        var retVal = defVal;
        try
        {
            var u = this.getParamValue('purl');
            if(u != null)
                retVal = u;
        }
        catch(exp)
        {
        }
        
        return retVal;
    },

    CLASS_NAME: "otp.util.ParseUrlParams"
};

otp.util.ParseUrlParams = new otp.Class(otp.util.ParseUrlParams);