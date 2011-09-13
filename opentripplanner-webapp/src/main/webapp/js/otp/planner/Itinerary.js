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
    locale         : null,
    templates      : null,
    showStopIds    : false,

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
        otp.configure(this, config);

        this.m_vectors    = new Array();
        this.m_markers    = new Array();

        this.m_legStore   = otp.planner.Utils.makeLegStore();
        this.m_fromStore  = otp.planner.Utils.makeFromStore();
        this.m_toStore    = otp.planner.Utils.makeToStore();
        
        this.load();
    },

    /** */
    load : function()
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
        if (this.m_vectors.length < 1) {
            this.makeRouteLines();
            this.makeWalkLines();
        }

        if (this.m_markers.length < 1) {
            this.makeMarkers();
        }

        // Reproject layer data for display if necessary
        if (this.map.dataProjection != vLayer.map.getProjection()) {
            for (var i = 0; i < this.m_vectors.length; ++i) {
                if (!this.m_vectors[i].geometry._otp_reprojected) {
                    this.m_vectors[i].geometry._otp_reprojected = true;
                    this.m_vectors[i].geometry.transform(
                            this.map.dataProjection, vLayer.map
                                    .getProjectionObject());
                }
            }
        }

        if (this.map.dataProjection != mLayer.map.getProjection()) {
            for (var i = 0; i < this.m_markers.length; ++i) {
                if (!this.m_markers[i].geometry._otp_reprojected) {
                    this.m_markers[i].geometry._otp_reprojected = true;
                    this.m_markers[i].geometry.transform(
                            this.map.dataProjection, 
                            mLayer.map.getProjectionObject()
                    );
                }
            }
        }

        vLayer.addFeatures(this.m_vectors);

        mLayer.addFeatures(this.m_markers);
        this.m_extent = mLayer.getDataExtent();
        this.m_extent.extend(vLayer.getDataExtent());
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

        if (this.m_vectors.length < 1) {
            this.makeRouteLines();
            this.makeWalkLines();
        }
        retVal = this.m_vectors;

        return retVal;
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
    makeRouteLines : function(vLayer) {
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
    makeWalkLines : function(vLayer) {
        var vectors = new Array();

        var endIndex = this.m_fromStore.getCount() - 1;
        for ( var i = 0; i <= endIndex; i++) {
            var from = this.m_fromStore.getAt(i);
            var leg = this.m_legStore.getAt(i);

            var mode = from.get('mode');
            if (mode === 'WALK' || mode === 'BICYCLE' || mode === 'TRANSFER') {
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
    makeMarkers : function() {
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

        //create special marker for bike elevation
        var bikeTopoMarker = otp.util.OpenLayersUtils.makeMarker(fromP.x, fromP.y, { // temp
            type : 'fromBicycleMarker',
            mode : 'BICYCLE'
        });
        bikeTopoMarker.id = 'bike-topo-marker';
        bikeTopoMarker.style = { display : 'none' };
        this.m_markers.push(bikeTopoMarker);
    },

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
        var fmTxt = this.templates.TP_START.applyTemplate(from.data);
        var toTxt = this.templates.TP_END.applyTemplate(to.data);

        var fmId  = this.id + '-' + otp.planner.Utils.FROM_ID;
        var toId  = this.id + '-' + otp.planner.Utils.TO_ID;
        var tpId  = this.id + '-' + otp.planner.Utils.TRIP_ID;
        
        var containsBikeMode    = false;
        var containsCarMode     = false;
        var containsTransitMode = false;

        var retVal = new Array();
        var numLegs = store.getCount();

        // step 1: start node
        retVal.push(otp.util.ExtUtils.makeTreeNode({id: fmId, text: fmTxt, cls: 'itiny', iconCls: 'start-icon', leaf: true}, clickCallback, scope));

        // step 2: leg and (sub-leg) instruction nodes
        for(var i = 0; i < numLegs; i++)
        {
            var leg = store.getAt(i);
            var text;
            var verb;
            var sched  = null;
            var mode = leg.get('mode').toLowerCase();
            var routeName = leg.get('routeName');
            var agencyId = leg.get('agencyId');

            var isLeaf = true;
            var instructions = null;
            var legId = this.id + this.LEG_ID + i;
            leg.data.showStopIds = this.showStopIds;

            // step 2a: build either a transit leg node, or the non-transit turn-by-turn instruction nodes
            if(otp.util.Modes.isTransit(mode))
            {
                text = this.templates.applyTransitLeg(leg);
                containsTransitMode = true;
            }
            else
            {
                var template = 'TP_WALK_LEG';
                if (mode === 'walk') {
                    verb = this.locale.instructions.walk_toward;
                }
                else if (mode === 'bicycle') {
                    verb = this.locale.instructions.bike_toward;
                    template = 'TP_BICYCLE_LEG';
                    containsBikeMode = true;
                } else if (mode === 'drive') {
                    verb = this.locale.instructions.drive_toward;
                    template = 'TP_CAR_LEG';
                    containsDriveMode = true;
                } else {
                    verb = this.locale.instructions.move_toward;
                }
                if (!leg.data.formattedSteps)
                {
                    instructions = this.makeInstructionStepsNodes(leg.data.steps, verb, legId);
                    leg.data.formattedSteps = "";
                    isLeaf = false;
                }
                text = this.templates[template].applyTemplate(leg.data);
            }

            // step 2c: make this leg (tree) node
            var icon = otp.util.imagePathManager.imagePath({mode:mode, agencyId:agencyId, route:routeName});
            var cfg = {id:legId, text:text, cls:'itiny', icon:icon, iconCls:'itiny-inline-icon', leaf:isLeaf};
            if(numLegs > 2)
            {
                // show/hide instructions if our trip has more than 2 legs 
                cfg.expanded = false;
                cfg.singleClickExpand = true;
            }
            var node = otp.util.ExtUtils.makeTreeNode(cfg, clickCallback, scope);

            // step 2d: if we have instruction sub-nodes, add them to the tree...
            if (instructions && instructions.length >= 1)
            {
                node.appendChild(instructions);
            }

            retVal.push(node);
        }

        // step 3: build details node's content 
        var tripDetailsDistanceVerb = this.locale.instructions.walk_verb;
        if(containsBikeMode)
            tripDetailsDistanceVerb = this.locale.instructions.bike_verb;
        else if(containsCarMode) 
            tripDetailsDistanceVerb = this.locale.instructions.car_verb;
        var tripDetailsData = Ext.apply({}, itin.data, {distanceVerb: tripDetailsDistanceVerb});
        var tpTxt = this.templates.TP_TRIPDETAILS.applyTemplate(tripDetailsData);

        // step 4: end and details nodes
        retVal.push(otp.util.ExtUtils.makeTreeNode({id: toId, text: toTxt, cls: 'itiny', iconCls: 'end-icon', leaf: true}, clickCallback, scope));
        retVal.push(otp.util.ExtUtils.makeTreeNode({id: tpId, text: tpTxt, cls: 'trip-details-shell', iconCls: 'no-icon', leaf: true}, clickCallback, scope));

        return retVal;
    },

    /** make bike / walk turn by turn narrative */
    makeInstructionStepsNodes : function(steps, verb, legId)
    {
        var retVal = [];
        var isFirstStep = true;

        var stepNum = 1;
        for (var i = 0; i < steps.length; i++)
        {
            var step = steps[i];
            if (step.streetName == "street transit link")
            {
                // TODO: Include explicit instruction about entering/exiting transit station or stop?
                continue;
            }

            this.addNarrativeToStep(step, verb, stepNum);
            
            var cfg = {id:legId + "-" + i, text:step.narrative, cls:'itiny-steps', icon:step.iconURL, iconCls:'itiny-inline-icon', leaf:true};
            var node = otp.util.ExtUtils.makeTreeNode(cfg, this.instructionClickCB, this, this.instructionHoverCB, this.instructionOutCB);
            node.m_step = step;
            stepNum++;

            retVal.push(node);
        }

        return retVal;
    },

    /** adds narrative and direction information to the step */ 
    addNarrativeToStep : function(step, verb, stepNum, dontEditStep)
    {
        var stepText   = "<strong>" + stepNum + ".</strong> ";
        var iconURL  = null;

        var relativeDirection = step.relativeDirection;
        if (relativeDirection == null || stepNum == 1)
        {
            var absoluteDirectionText = this.locale.directions[step.absoluteDirection.toLowerCase()];
            stepText += verb + ' <strong>' + absoluteDirectionText + '</strong> ' + this.locale.directions.on;
            iconURL = otp.util.ImagePathManagerUtils.getStepDirectionIcon();
        }
        else 
        {
            relativeDirection = relativeDirection.toLowerCase();
            iconURL = otp.util.ImagePathManagerUtils.getStepDirectionIcon(relativeDirection);

            var directionText = otp.util.StringFormattingUtils.capitolize(this.locale.directions[relativeDirection]);
            if (relativeDirection == "continue")
            {
                stepText += directionText;
            }
            else if (step.stayOn == true)
            {
                stepText += directionText + " " + this.locale.directions['to_continue'];
            }
            else
            {
                stepText += directionText;
                if (step.exit != null) {
                    stepText += " " + this.locale.ordinal_exit[step.exit] + " ";
                }
                stepText += " " + this.locale.directions['on'];
            }
        }
        stepText += ' <strong>' + step.streetName + '</strong>';
        stepText += ' (' + otp.planner.Utils.prettyDistance(step.distance) + ')';

        // edit the step object (by default, unless otherwise told)
        if(!dontEditStep)
        {
            step.narrative  = stepText;
            step.iconURL    = iconURL;
            step.bubbleHTML = '<img src="' + iconURL + '"></img> ' + ' <strong>' + stepNum + '.</strong> ' + step.streetName;
            step.bubbleLen  = step.streetName.length + 3;
        }

        return stepText;
    },

    /** */
    instructionClickCB : function(node, m)
    {
        if(node && node.m_step)
        {
            this.map.pan(node.m_step.lon, node.m_step.lat);
            this.instructionHoverCB(node, m);
            node.m_clicked = true;
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
    clickCount : 0,
    instructionOutCB : function(node, m)
    {
        if(!node.m_clicked)
        {
            this.map.tooltipHide();
            this.map.streetviewHide();
            this.clickCount = 0;
        }
        if(this.clickCount > 2) 
        {
            node.m_clicked = false;
        }
        this.clickCount++;
    },


    CLASS_NAME: "otp.planner.Itinerary"
};

otp.planner.Itinerary = new otp.Class(otp.planner.Itinerary);
