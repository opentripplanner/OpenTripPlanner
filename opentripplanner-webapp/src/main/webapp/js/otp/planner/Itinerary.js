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
    // config
    map            : null,
    triptab        : null,
    locale         : null,
    templates      : null,
    planner        : null,

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
    m_modes        : null,

    /** */
    initialize : function(config)
    {
        otp.configure(this, config);

        this.m_legStore   = otp.planner.Utils.makeLegStore();
        this.m_fromStore  = otp.planner.Utils.makeFromStore();
        this.m_toStore    = otp.planner.Utils.makeToStore();

        this._load();
        this.m_modes = new otp.util.ItineraryModes(config, this);
    },

    /** */
    _load : function()
    {
        this.m_legStore.loadData(this.xml.node);
        this.m_fromStore.loadData(this.xml.node);
        this.m_toStore.loadData(this.xml.node);

        // check for valid load
        this.m_valid = false;
        if(this.m_legStore.getCount() > 0)
        {
            if(this.m_fromStore.getCount() == this.m_toStore.getCount() 
               && this.m_fromStore.getCount() == this.m_legStore.getCount()) {
                this.m_valid = true;
            }
        }

        // get start & end time
        this.makeStartEndTime();

        return this.m_valid;
    },


    /**
     * draws the route vectors on the passed in vector layer 
     * NOTE: will create & cache the vectors from the itinerary, if they are not cached 
     */
    draw : function(vLayer, mLayer)
    {
        // step 1: draw route lines (vectors) and adjust the map extent to the extent of the vLayer 
        if (vLayer)
        {
            this.getVectors(vLayer.map.getProjection());
            if(this.m_vectors && this.m_vectors.length >= 1)
            {
                vLayer.addFeatures(this.m_vectors);
                this.m_extent = vLayer.getDataExtent();
            }
        }

        // step 2: draw markers (note mLayer is null on alt-route preview), and increase the extent 
        if(mLayer)
        {
            this.getMarkers(mLayer.map.getProjection());
            if(this.m_markers && this.m_markers.length >= 1)
            {
                mLayer.addFeatures(this.m_markers);
                if(this.m_extent)
                    this.m_extent.extend(mLayer.getDataExtent());
            }
        }
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

    /**
     * returns route vectors 
     * NOTE: will create & cache the vectors from the itinerary, if they are not already there
     * BUT:  the line might not be there fully if this is the first call to get the vector from AJAX, 
     *       since the route vector may not be completely returned from the async call.
     *       
     * IMPORTANT -- July 6 2012 / OpenLayers 2.12: something in OL 2.11 & 2.12 makes vectors.geometry 
     *              become empty (I think we're doing a delete all when clearing an itinery, and newer OL
     *              libs are now kicking out the geom data).  So we have refactored geom building routines,
     *              so it's worth watching whether the app degrades in some way with these changes... 
     */
    getVectors : function(proj)
    {
        // rebuild the cache if necessary
        if(!this.m_vectors || this.m_vectors.length < 1 || !this.m_vectors[0].geometry)
        {
            console.log("Itinerary.getVectors: rebuilding vector cache for this itinerary");
            this.m_vectors = new Array();
            this.makeRouteLines();
            this.makeWalkLines();
        }

        // reproject layer data if necessary
        if(this.m_vectors && proj && proj != this.map.dataProjection)
        {
            for(var i = 0; i < this.m_vectors.length; ++i)
            {
                if (this.m_vectors[i].geometry && !this.m_vectors[i].geometry._otp_reprojected)
                {
                    try
                    {
                        // OL 2.9 hack -- map.getProj() comes back as a string, so we'll convert and retest here
                        // NOTE: this proj check is here because it's only going to get exectued once via the _otp_reprojected check above
                        if(typeof(proj) == 'string')
                        {
                            proj = new OpenLayers.Projection(proj);
                            // retest projection objects and breaking out if same proj as layer
                            if(proj == this.map.dataProjection)
                                break;
                        }

                        // reproject geometry
                        this.m_vectors[i].geometry.transform(this.map.dataProjection, proj);
                        this.m_vectors[i].geometry._otp_reprojected = true;
                    }
                    catch(e)
                    {
                        console.log("EXCEPTION: Itinerary.getVectors() reproject: " + e);
                    }
                }
            }
        }

        return this.m_vectors;
    },

    /**
     * returns route markers annotating the itinerary on the map.
     * NOTE: will create & cache the markers from the itinerary, if they are not already there
     *
     * IMPORTANT -- July 6 2012 / OpenLayers 2.12: something in OL 2.11 & 2.12 makes vectors.geometry 
     *              become empty (I think we're doing a delete all when clearing an itinery, and newer OL
     *              libs are now kicking out the geom data).  So we have refactored geom building routines,
     *              so it's worth watching whether the app degrades in some way with these changes... 
     * 
     */
    getMarkers : function(proj)
    {
        // rebuild the cache if necessary
        if(!this.m_markers || this.m_markers.length < 1 || !this.m_markers[0].geometry)
        {
            console.log("Itinerary.getMarkers: rebuilding marker cache for this itinerary");
            this.m_markers = new Array();
            this.makeMarkers();
        }

        // reproject layer data if necessary
        if(this.m_markers && proj && proj != this.map.dataProjection)
        {
            for (var i = 0; i < this.m_markers.length; ++i)
            {
                if (this.m_markers[i].geometry && !this.m_markers[i].geometry._otp_reprojected)
                {
                    try
                    {
                        // OL 2.9 hack -- map.getProj() comes back as a string, so we'll convert and retest here
                        // NOTE: this proj check is here because it's only going to get exectued once via the _otp_reprojected check above
                        if(typeof(proj) == 'string')
                        {
                            proj = new OpenLayers.Projection(proj);
                            // retest projection objects and breaking out if same proj as layer
                            if(proj == this.map.dataProjection)
                                break;
                        }

                        // reproject geometry
                        this.m_markers[i].geometry.transform(this.map.dataProjection, proj);
                        this.m_markers[i].geometry._otp_reprojected = true;
                    }
                    catch(e)
                    {
                        console.log("EXCEPTION: Itinerary.getMarkers() reproject: " + e);
                    }
                }
            }
        }

        return this.m_markers;
    },


    /**
     * pushes a new vector into the line array
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
        var retVal = {};

        this.m_startTime = this.xml.data.startTime;
        this.m_endTime = this.xml.data.endTime;

        return retVal;
    },


    /**
     * 
     */
    makeStartEndTime : function()
    
    {
        this.m_startTime = this.xml.data.startTime;
        this.m_endTime = this.xml.data.endTime;
    },


    /**
     * 
     */
    makeRouteLines : function(vLayer)
    {
        var vectors = new Array();

        var endIndex = this.m_fromStore.getCount() - 1;
        for ( var i = 0; i <= endIndex; i++) {
            var from = this.m_fromStore.getAt(i);
            var leg = this.m_legStore.getAt(i);
            var mode = from.get('mode');

            if (otp.util.Modes.isTransit(mode)) {
                var geoJson = leg.get('legGeometry');
                var geoLine = new OpenLayers.Feature.Vector(geoJson, null, otp.util.OpenLayersUtils.RED_STYLE);
                var newLine = otp.util.OpenLayersUtils.makeStraightLine(from, this.m_toStore.getAt(i));
                vectors.push(geoLine);
            }
        }

        if (vectors.length > 0) {
            this.concatVectors(vectors);
            if (vLayer) {
                vLayer.addFeatures(vectors);
            }
        }
    },


    /**
     * makes lines between from / to / transfers NOTE: should only be called
     * when creating a new itinerary (not every time that itinerary is drawn)
     */
    makeWalkLines : function(vLayer)
    {
        var vectors = new Array();

        var endIndex = this.m_fromStore.getCount() - 1;
        for ( var i = 0; i <= endIndex; i++) {
            var from = this.m_fromStore.getAt(i);
            var leg = this.m_legStore.getAt(i);

            var mode = from.get('mode');
            if (mode === 'WALK' || mode === 'BICYCLE' || mode === 'TRANSFER' || mode == 'CAR') {
                var geoLine = new OpenLayers.Feature.Vector(leg.get('legGeometry'), null, otp.util.OpenLayersUtils.BLACK_STYLE);
                var newLine = otp.util.OpenLayersUtils.makeStraightLine(from, this.m_toStore.getAt(i));
                vectors.push(geoLine);
            }
        }

        if (vectors.length > 0) {
            this.concatVectors(vectors);
            if (vLayer) {
                vLayer.addFeatures(vectors);
            }
        }
    },

    createAndAddMarker: function(x, y, options)
    {
        var marker = otp.util.OpenLayersUtils.makeMarker(x, y, options);
        this.m_markers.push(marker);
    },


   /**
    * Gets a new Marker Layer for drawing the trip plan's features upon
    */
    makeMarkers : function()
    {
        var startIndex = 0;
        var endIndex = this.m_fromStore.getCount() - 1;

        var markersToAdd = [];

        // do the FROM marker
        var from = this.m_fromStore.getAt(startIndex);
        var fromP = from.get('geometry');
        var mode = from.get('mode');

        if (otp.util.Modes.isTransit(mode)) {
            // so paint the route icon (eg: fromStore.getAt(0))
            startIndex = 0;
            this.createAndAddMarker (fromP.x, fromP.y, {
                type : 'fromMarker',
                mode : mode
            });
        } else {
            // first leg is a walk leg, so mark this point with the from icon
            // that has the walking guy, and move on to next leg in store...
            startIndex = 1;
            var markerType;
            if (mode === 'WALK') {
                markerType = 'fromWalkMarker';
            } else if (mode === 'BICYCLE') {
                markerType = 'fromBicycleMarker';
            } else {
                markerType = 'fromMarker';
            }
            this.createAndAddMarker(fromP.x, fromP.y, {
                type : markerType,
                mode : mode
            });
        }

        // if the last leg is a walk, then paint it now & don't print a route
        // icon (eg: endIndex--)
        var walk = this.m_fromStore.getAt(endIndex);
        var walkP = walk.get('geometry');
        mode = walk.get('mode');
        // Don't draw another walk marker if the first leg is a walk or bike and there's only one leg
        if ((mode === 'WALK' || mode === 'BICYCLE') && endIndex > 0) {
            endIndex--;
            var markerType = (mode === 'BICYCLE') ? 'bicycleMarker' : 'walkMarker';
            markersToAdd.push([walkP.x, walkP.y, {
                type : markerType,
                mode : mode
            }]);
        }

        // save the list of routes for this itinerary the first time around
        var doRoutes = false;
        if (this.m_routes == null) {
            this.m_routes = new Array();
            doRoutes = true;
        }

        // draw the itinerary
        for ( var i = startIndex; i <= endIndex; i++) {
            var from = this.m_fromStore.getAt(i);
            var to   = this.m_toStore.getAt(i);
            var leg  = this.m_legStore.getAt(i);
            var interline = leg.get('interline');
            var route = from.get('routeID');
            var mode = from.get('mode');

            var fromP = from.get('geometry');
            var toP = to.get('geometry');

            // save the route number off (eg: used to show vehicles on the map
            // for these routes, etc...)
            if (doRoutes && route != null && route.length > 0)
                this.m_routes.push(route);

            // only show the route bubble if we're drawing the beginning of the
            // block (eg not a thru route transfer / stay on bus)
            if(interline == null || (interline != "true" && interline !== true))
            {
                this.createAndAddMarker(fromP.x, fromP.y, {
                    type : 'diskMarker',
                    mode : mode
                });
                // TODO: How should street transit links be rendered?
                if (route == "street transit link" || mode == "TRANSFER") {
                    markersToAdd.push([fromP.x, fromP.y, {
                        type : 'walkMarker',
                        mode : mode
                    }]);
                } else {
                    var agencyId = from.get('agencyId');
                    markersToAdd.push([fromP.x, fromP.y, {
                        type : 'routeMarker',
                        mode : mode,
                        route : route,
                        agencyId : agencyId
                    }]);
                }
            }

            // put a disk at the end of this route segment
            this.createAndAddMarker(toP.x, toP.y, {
                type : 'diskMarker'
            });
        }

        this.assignDirectionToMarkers(markersToAdd);
        for (var i = 0; i < markersToAdd.length; ++i) {
            var marker = markersToAdd[i];
            if (marker[2].direction == 'left') {
                if (marker[2].type === 'walkMarker') {
                    marker[2].type = 'walkMarkerLeft';
                } else if (marker[2].type === 'routeMarker') {
                    marker[2].type = 'routeMarkerLeft';
                }
            }
            this.createAndAddMarker(marker[0], marker[1], marker[2]);
        }

        // do the TO (end) marker
        var to = this.m_toStore.getAt(this.m_toStore.getCount() - 1);
        var toP = to.get('geometry');
        this.createAndAddMarker(toP.x, toP.y, {
            type : 'toMarker'
        });

        //create special markers for bike/walk elevation
        var bikeTopoMarker = otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, {
            type : 'fromBicycleMarker',
            mode : 'BICYCLE'
        });
        bikeTopoMarker.id = 'bicycle-topo-marker';
        bikeTopoMarker.style = { display : 'none' };
        this.m_markers.push(bikeTopoMarker);

        var walkTopoMarker = otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, {
            type : 'fromWalkMarker',
            mode : 'WALK'
        });
        walkTopoMarker.id = 'walk-topo-marker';
        walkTopoMarker.style = { display : 'none' };
        this.m_markers.push(walkTopoMarker);

    },

    /** */
    assignDirectionToMarkers : function(markers) 
    {
        if (markers.length === 0) {
	    return;
        }
        bestDistance = 1000;
        bestMarkerIdx = -1;
        for (var i = 0; i < markers.length - 1; ++i) {
            var x1 = markers[i][0];
            var y1 = markers[i][1];
            var mark1 = markers[i][2];
            var x2 = markers[i+1][0];
            var y2 = markers[i+1][1];
            var mark2 = markers[i+1][2];
            if (undefined === mark1.direction && undefined === mark2.direction) {
                //this pair has not yet been assigned; are they the closest?
                var distance = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestMarkerIdx = i;
                }
            }
        }
        if (bestMarkerIdx === -1) {
            //we have applied direction to all nearest pairs
            //now we want to apply to whatever's left

            //the first marker
            if (undefined === markers[0][2].direction) {
                if (markers.length === 1 || markers[0][1] > markers[1][1]) {
                    //0th marker is right of 1st marker
                    markers[0][2].direction = 'right';
                } else {
                    markers[0][2].direction = 'left';
                }
            }
            //the last marker
            var last = markers.length - 1;
            if (undefined === markers[last][2].direction) {
                if (markers[last][1] > markers[last - 1][1]) {
                    //0th marker is right of 1st marker
                    markers[last][2].direction = 'right';
                } else {
                    markers[last][2].direction = 'left';
                }
            }
            for (var i = 1; i < last; ++i) {
                if (undefined != markers[i].direction) {
                    continue;
                }
                var x0 = markers[i-1][0];
                var y0 = markers[i-1][1];
                var mark0 = markers[i-1][2];
                var x1 = markers[i][0];
                var y1 = markers[i][1];
                var mark1 = markers[i][2];
                var x1 = markers[i][0];
                var y1 = markers[i][1];
                var mark1 = markers[i][2];
                var x2 = markers[i+1][0];
                var y2 = markers[i+1][1];
                var mark2 = markers[i+1][2];

                var distance0 = Math.sqrt((x1-x0)*(x1-x0)+(y1-y0)*(y1-y0));
                var distance1 = Math.sqrt((x1-x2)*(x1-x2)+(y1-y2)*(y1-y2));
                if (distance0 > distance1) {
                    if (x0 > x1) {
                        markers[1][2].direction = 'right';
                    } else {
                        markers[1][2].direction = 'left';
                    }
                } else {
                    if (x0 > x1) {
                        markers[1][2].direction = 'right';
                    } else {
                        markers[1][2].direction = 'left';
                    }
                }
            }
        } else {
            //we have a best pair, so we should make mark their directions
            if (markers[bestMarkerIdx][0] > markers[bestMarkerIdx + 1][0]) {
                markers[bestMarkerIdx][2].direction = "right";
                markers[bestMarkerIdx + 1][2].direction = "left";
            } else {
                markers[bestMarkerIdx][2].direction = "left";
                markers[bestMarkerIdx + 1][2].direction = "right";
            }
            //assign to rest
            this.assignDirectionToMarkers(markers);
        }
    },
//
// TREE STUFF
//
    LEG_ID : '-leg-',

    /** */
    getLegStartPoint : function(id) {
        var nid = id.substring(id.lastIndexOf(this.LEG_ID) + this.LEG_ID.length);
        var retVal = this.m_fromStore.getAt(nid);
        return retVal;
    },

    /** */
    getTreeNodes : function(clickCallback, scope)
    {
        return this.makeTreeNodes(clickCallback, scope);
    },

    /**  */
    makeTreeNodes : function(clickCallback, scope)
    {
        var retVal = [];

        // step 1: get itinerary data
        var cfg  = otp.inherit(null, this);
        cfg.store=this.m_legStore;
        cfg.modes=this.m_modes;
        cfg.details=this.xml;
        var itin = otp.planner.ItineraryDataFactoryStatic.factory(cfg);

        // step 2: start node
        retVal.push(otp.util.ExtUtils.makeTreeNode(itin.from, clickCallback, scope));

        // step 3: itinerary legs
        if(itin.steps)
        for(var i = 0; i < itin.steps.length; i++)
        {
            // step 3a: get the step
            var step = itin.steps[i];

            // step 3b: make this leg (tree) node
            var node;
            if(!step.leaf && itin.steps.length > 2)
            {
                // show/hide instructions if our trip has more than 2 legs 
                step.expanded = false;
                step.singleClickExpand = true;
                var id = 'showDetails-' + this.triptab.id + "-" + step.id;
                step.text += '<div id="' + id + '" class="show-hide-details"> ' + this.templates.getShowDetails() + '</div>';
                node = otp.util.ExtUtils.makeTreeNode(step, clickCallback, scope);
                node.showDetailsId = id;
                node.showing = false;
            }
            else
            {
                node = otp.util.ExtUtils.makeTreeNode(step, clickCallback, scope);
            }

            // step 3c: if we have instruction sub-nodes, add them to the tree...
            if(!step.leaf)
            {
                // step 3c: loop through step instructions, creating tree nodes for each...
                var inodes = [];
                for(var j = 0; j < step.instructions.length; j++)
                {
                    var inst  = step.instructions[j];
                    var inode = otp.util.ExtUtils.makeTreeNode(inst, this.instructionClickCB, this, this.instructionHoverCB, this.instructionOutCB);
                    inode.m_step = inst.originalData;
                    inodes.push(inode);
                }
                node.appendChild(inodes);
            }

            // step 3d: push this tree node 
            retVal.push(node);
        }

        // step 4: close the itinerary
        retVal.push(otp.util.ExtUtils.makeTreeNode(itin.to, clickCallback, scope));

        // step 5: optional mode note and details nodes
        if(itin.notes)
        {
            retVal.push(otp.util.ExtUtils.makeTreeNode(itin.notes, clickCallback, scope));
        }
        retVal.push(otp.util.ExtUtils.makeTreeNode(itin.details, clickCallback, scope));

        return retVal;
    },

    outCount : 0,
    clicked  : null,

    /** */
    instructionClickCB : function(node, m)
    {
        if(node && node.m_step)
        {
            this.clicked  = node;
            this.outCount = 0;
            this.map.tooltipCleared = false;
            this.map.pan(node.m_step.lon, node.m_step.lat);
            this.instructionHoverCB(node, m);
        }
    },


    /** hover steps to show a small popup with direction icon and street name */
    instructionHoverCB : function(node, m)
    {
        if (node && node.m_step)
        {
            this.map.tooltip(node.m_step.lon, node.m_step.lat, node.m_step.bubbleHTML, node.m_step.bubbleLen);
        }
    },

    /** mouse out callback */
    instructionOutCB : function(node, m)
    {
        // clear all map tooltips
        this.map.tooltipHide();
        this.map.streetviewHide();

        // stopping condition for clicked tooltips
        if(this.outCount >= 5 || this.map.tooltipCleared) 
        {
            this.clicked = null;
        }
        this.outCount++;

        // if we had an earlier click event (and haven't yet exceeded the outCount), show the clicked tooltip again 
        if(this.clicked)
        {
            // reset the map tool-tip back to our 'clicked' node 
            this.instructionHoverCB(this.clicked);
        }
    },

    CLASS_NAME: "otp.planner.Itinerary"
};
otp.planner.Itinerary = new otp.Class(otp.planner.Itinerary);
