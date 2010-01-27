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
  * Web TripPlanner
  * 
  * otp.planner.Itinerary's purpose is represent a single trip (eg: from point X to point Y).  It contains logic for both
  * parsing the trip XML data into object form, as well as handling events (clicks, etc...) of the trip.  
  *
  * The basic object model is Planner --> TripTab --> Itinerary (with Itinerary being the lowest-level object)
  */

otp.planner.Itinerary = {
    
    // raw data
    xml            : null,
    from           : null,
    to             : null,
    id             : null,
    geoURL         : '',

    lineStyle      : otp.util.OpenLayersUtils.RED_STYLE,

    // stored data
    m_legStore     : null,
    m_fromStore    : null,
    m_toStore      : null,
    m_routes       : null,

    m_startTime    : null,
    m_endTime      : null,

    // geo data
    m_vectors      : null,
    m_markers      : null,
    m_extent       : null,

    // misc
    m_valid        : false,

    /** */
    initialize : function(config)
    {
        console.log("enter planner.Itinerary constructor");
        otp.configure(this, config);

        this.m_vectors    = new Array();
        this.m_markers    = new Array();

        this.m_legStore   = otp.planner.Utils.makeLegStore();
        this.m_fromStore  = otp.planner.Utils.makeFromStore();
        this.m_toStore    = otp.planner.Utils.makeToStore();
        
        this.load();
        console.log("exit planner.Itinerary constructor");
    },

    /** */
    load : function()
    {
        console.log("enter Itinerary.load");
        this.m_legStore.loadData(this.xml.node);
        this.m_fromStore.loadData(this.xml.node);
        this.m_toStore.loadData(this.xml.node);

        // check for valid load
        this.m_valid = false;
        if(this.m_legStore.getCount() > 0)
        {
            if(this.m_fromStore.getCount() == this.m_toStore.getCount() 
            && this.m_fromStore.getCount() == this.m_legStore.getCount())
                this.m_valid = true;
        }

        // get start & end time
        this.makeStartEndTime();

        console.log("exit Itinerary.load");
        return this.m_valid;
    },


    /**
     * draws the route vectors on the passed in vector layer 
     * NOTE: will create & cache the vectors from the itinerary, if they are not cached 
     */
    draw : function(vLayer, mLayer)
    {
        console.log("enter Itinerary.draw");
        try
        {
            if(this.m_vectors.length < 1)
            {
                this.makeRouteLines();
                this.makeWalkLines();
            }
            
            if( this.m_markers.length < 1) {
                this.makeMarkers();
            }
            
            vLayer.addFeatures(this.m_vectors);
            mLayer.addFeatures(this.m_markers);
            this.m_extent = mLayer.getDataExtent();
        }
        catch(e)
        {
            console.log("exception Itinerary.draw " + e);
        }

        console.log("exit Itinerary.draw ");
    },

    /** */
    getRoutes : function()
    {
        return this.m_routes;
    },

    /** */
    getExtent : function()
    {
        return this.m_extent;
    },

    /** */
    getMarkers : function()
    {
        return this.m_markers;
    },

    /**
     * returns route vectors 
     * NOTE: will create & cache the vectors from the itinerary, if they are not already there
     * BUT:  the line might not be there fully if this is the first call to get the vector from AJAX, 
     *       since the route vector may not be completely returned from the async call.
     */
    getVectors : function()
    {
        var retVal = null;
        try
        {
            if(this.m_vectors.length < 1)
            {
                this.makeRouteLines()
                this.makeWalkLines();
            }
            retVal = this.m_vectors;
        }
        catch(e)
        {
            console.log("exit Itinerary.getVectors " + e);
        }
        
        return retVal;
    },


    /**
     *  pushes a new vector into the line array 
     */
    pushVector : function(vector)
    {
        if(vector != null)
            this.m_vectors.push(vector);
    },

    /**
     *  pushes an array of vectors into the vector array 
     */
    concatVectors : function(vectors)
    {
        if(vectors && vectors.length > 0)
            this.m_vectors = this.m_vectors.concat(vectors);
    },

    /** */
    getFrom : function()
    {
        return this.from;
    },

    /** */
    getTo : function()
    {
        return this.to;
    },

    /** */
    getId : function()
    {
        return this.id;
    },

    /** */
    isValid : function()
    {
        return this.m_valid;
    },

    /**
     * 
     */
    getParams : function()
    {
        console.log("enter Itinerary.getParams");
        var retVal = {};

        try
        {
           this.m_startTime = this.xml.data.startTime;
           this.m_endTime   = this.xml.data.endTime;
        }
        catch(e)
        {
            console.log("exception Itinerary.getParams " + e);
        }

        console.log("exit Itinerary.getParams");
        return retVal;
    },


    /**
     * 
     */
    makeStartEndTime : function()
    {
        console.log("enter Itinerary.makeStartEndTime");

        try
        {
           this.m_startTime = this.xml.data.startTime;
           this.m_endTime   = this.xml.data.endTime;
        }
        catch(e)
        {
            console.log("exception Itinerary.makeStartEndTime " + e);
        }

        console.log("exit Itinerary.makeStartEndTime");
    },


    /**
     * 
     */
    makeRouteLines : function(vLayer)
    {
        console.log("enter Itinerary.makeRouteLines");

        var vectors  = new Array();
        
        var endIndex = this.m_fromStore.getCount() - 1;
        for(var i = 0; i <= endIndex; i++) 
        {
            var from = this.m_fromStore.getAt(i);
            var leg = this.m_legStore.getAt(i);
            var mode = from.get('mode');

            if(mode == 'BUS' || mode == 'TRAM' || mode == 'RAIL' || mode == 'SUBWAY' || mode == 'GONDOLA') 
            {
                try
                {
                	var geoJson = leg.get('legGeometry');
                	var geoLine = new OpenLayers.Feature.Vector(geoJson,
                			null,
                			otp.util.OpenLayersUtils.RED_STYLE);
                			
                    var newLine = otp.util.OpenLayersUtils.makeStraightLine(from, this.m_toStore.getAt(i));
                    vectors.push(geoLine);
                }
                catch(e)
                {
                    console.log("exception Itinerary.makeRouteLines " + e);
                }
            }
        }
      
        if(vectors.length > 0)
        {
            this.concatVectors(vectors);
            if(vLayer)
                vLayer.addFeatures(vectors);
        }

        console.log("exit Itinerary.makeRouteLines");
    },


    /**
     * makes lines between from / to / transfers
     * NOTE: should only be called when creating a new itinerary (not every time that itinerary is drawn)
     */
    makeWalkLines : function(vLayer)
    {
        console.log("enter Itinerary.makeWalkLines");

        var vectors  = new Array();
        
        var endIndex = this.m_fromStore.getCount() - 1;
        for(var i = 0; i <= endIndex; i++) 
        {
            var from = this.m_fromStore.getAt(i);
            var leg = this.m_legStore.getAt(i);
            
            var mode = from.get('mode');
            if(mode == 'WALK') 
            {
                try
                {
                	var geoLine = new OpenLayers.Feature.Vector(leg.get('legGeometry'),
                			null,
                			otp.util.OpenLayersUtils.BLACK_STYLE);
                			
                    var newLine = otp.util.OpenLayersUtils.makeStraightLine(from, this.m_toStore.getAt(i));
                    vectors.push(geoLine);
                }
                catch(e)
                {
                    console.log("exception Itinerary.makeWalkLines " + e);
                }
            }
        }
      
        if(vectors.length > 0)
        {
            this.concatVectors(vectors);
            if(vLayer)
                vLayer.addFeatures(vectors);
        }

        console.log("exit Itinerary.makeWalkLines");
    },



   /**
    * Gets a new Marker Layer for drawing the trip plan's features upon
    */
    makeMarkers : function()
    {
        console.log("enter Itinerary.makeMarkers");
        try
        {
            var startIndex = 0;
            var endIndex   = this.m_fromStore.getCount() - 1; 

            // do the FROM marker 
            var from = this.m_fromStore.getAt(startIndex);
            var fromP = from.get('geometry');
            var mode = from.get('mode');
            if(mode != 'WALK') 
            {
                // if the first leg isn't a walk, then assume it's a transit leg
                // so paint the route icon (eg: fromStore.getAt(0))
                startIndex = 0;
                otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, 'fromMarker', this.m_markers);
            }
            else
            {
                // first leg is a walk leg, so mark this point with the from icon that has the walking guy, and move on to next leg in store...
                startIndex = 1;
                otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, 'fromWalkMarker', this.m_markers);
            }

            // if the last leg is a walk, then paint it now & don't print a route icon (eg: endIndex--)
            var walk = this.m_fromStore.getAt(endIndex);
            var walkP = walk.get('geometry');
            var mode = walk.get('mode');
            if(mode == 'WALK')
            {
                endIndex--;
                otp.util.OpenLayersUtils.makeMarker(walkP.x, walkP.y, 'walkMarker', this.m_markers);
            }

            // save the list of routes for this itinerary the first time around
            var doRoutes = false;
            if(this.m_routes == null)
            {
                this.m_routes = new Array();
                doRoutes = true;
            }

            // draw the itinerary 
            for(var i = startIndex; i <= endIndex; i++) 
            {
                var from  = this.m_fromStore.getAt(i);
                var to    = this.m_toStore.getAt(i);
                var thru  = from.get('order');
                var route = from.get('routeID');
                
                var fromP = from.get('geometry');
                var toP = to.get('geometry');

                // save the route number off (eg: used to show vehicles on the map for these routes, etc...)
                if(doRoutes && route != null && route.length > 0)
                   this.m_routes.push(route);

                // only show the route bubble if we're drawing the beginning of the block (eg not a thru route transfer / stay on bus)
                if(thru == null || thru != 'thru-route')
                {
                    otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, 'diskMarker', this.m_markers);
                    otp.util.OpenLayersUtils.makeRouteMarker(fromP.x, fromP.y, route, this.m_markers);
                }

                // put a disk at the end of this route segment
                otp.util.OpenLayersUtils.makeMarker(toP.x, toP.y, 'diskMarker', this.m_markers);
            }

            // do the TO (end) marker 
            var to = this.m_toStore.getAt(this.m_toStore.getCount() - 1);
            var toP = to.get('geometry');
            otp.util.OpenLayersUtils.makeMarker(toP.x, toP.y, 'toMarker', this.m_markers);
        }
        catch(e)
        {
            console.log("exit Itinerary.makeMarkers" + e);
        }
        console.log("exit Itinerary.makeMarkers");
    },
        
//
// TREE STUFF
//
    LEG_ID : '-leg-',

    /** */
    getLegStartPoint : function(id)
    {
        try
        {
            var nid = id.substring(id.lastIndexOf(this.LEG_ID) + this.LEG_ID.length);
            var retVal = this.m_fromStore.getAt(nid);
            return retVal;
        }
        catch(e)
        {
            console.log("exit Itinerary.getLegStartPoint " + e);
        }
    },

    /** */
    getTreeNodes : function(clickCallback, scope)
    {
        return this.makeTreeNodes(this.m_legStore, this.xml, this.from, this.to, clickCallback, scope);
    },

  /**
    * this method creates new tree nodes, based on the leg store.  each time an itinerary is  
    * selected, this method is called to populate the legs of the itinerary
    * 
    * m_treeNodes = makeTreeNodes(m_legStore, m_itin, from, to, this.legClick);
    *  
    * NOTE: Ext tree nodes (v2 RC1) will not render afert being 'deleted' from their parent.
    *       So we only render a copy of the trip nodes...cleanup is provided via clearNodes above.
    */
    makeTreeNodes : function(store, itin, from, to, clickCallback, scope)
    {
        var fmTxt = otp.planner.Templates.TP_START.applyTemplate(from.data);
        var toTxt = otp.planner.Templates.TP_END.applyTemplate(to.data);
        var tpTxt = otp.planner.Templates.TP_TRIPDETAILS.applyTemplate(itin.data);

        var fmId  = this.id + '-' + otp.planner.Utils.FROM_ID;
        var toId  = this.id + '-' + otp.planner.Utils.TO_ID;
        var tpId  = this.id + '-' + otp.planner.Utils.TRIP_ID;

        var retVal = new Array();
        retVal.push(otp.util.ExtUtils.makeTreeNode(fmId, fmTxt, 'itiny',      'start-icon', true,  clickCallback, scope));

        for(var i = 0; i < store.getCount(); i++)
        {
            var leg = store.getAt(i);
            var text;
            var hasKids = true;
            var iconCLS = 'bus-icon';
            var sched = null;
            if (leg.get('mode') == 'walk') 
            {
                text = otp.planner.Templates.TP_WALK_LEG.applyTemplate(leg.data);
                hasKids = false;
                iconCLS = 'walk-icon';
            }
            else
            {
                if (leg.get('order') == 'thru-route') {
                    text = otp.planner.Templates.getInterlineLeg().applyTemplate(leg.data);
                }
                else 
                {
                    text  = otp.planner.Templates.getTransitLeg().applyTemplate(leg.data);
                    //sched = otp.planner.Templates.makeTTPUBLinkFromData(leg.data);
                }

                if(leg.get('mode') == 'bus')
                    iconCLS = 'bus-icon';
                else if(leg.get('mode') == 'tram')
                    iconCLS = 'tram-icon';
                else if(leg.get('mode') == 'streetcar')
                    iconCLS = 'streetcar-icon';
                else if(leg.get('mode') == 'rail')
                    iconCLS = 'commrail-icon';
                else
                    iconCLS = 'ltrail-icon';
            }
            retVal.push(otp.util.ExtUtils.makeTreeNode(this.id + this.LEG_ID + i, text, 'itiny', iconCLS, hasKids, clickCallback, scope));
        }
        retVal.push(otp.util.ExtUtils.makeTreeNode(toId, toTxt, 'itiny', 'end-icon', true, clickCallback, scope));
        retVal.push(otp.util.ExtUtils.makeTreeNode(tpId, tpTxt, 'trip-details-shell', 'no-icon', true, clickCallback, scope));

        return retVal;
    },

    CLASS_NAME: "otp.planner.Itinerary"
}

otp.planner.Itinerary = new otp.Class(otp.planner.Itinerary);
