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

/***  ItineraryData.js contain's 3 classes.  Two DAO classes, plus a Factory below for creating the ItineraryData DAO ***/

/** 
 * ItineraryData
 * otp.planner.ItineraryDataFactory's purpose is represent the discrete pieces of data making up an itinerary.
 * ItineraryData is a structure to hold various pieces of otp.planner.StepData, forming the full itinerary.
 */
otp.planner.ItineraryData = {

    from    : null,
    to      : null,
    details : null,
    steps   : null,
    notes   : null,

    initialize : function(config)
    {
        otp.configure(this, config);
    },

    CLASS_NAME : "otp.planner.ItineraryData"
};
otp.planner.ItineraryData = new otp.Class(otp.planner.ItineraryData);


/** 
 * data stuct to hold itinerary step information ... drawing the itinerary will be based on iterating arrays of StepData  
 */
otp.planner.StepData = {
    id           : null,   // unique div id (suitable for Ext's id element)
    num          : null,   // number count of this type of data
    instructions : null,   // sub-array of StepData elements (often null), used for 'indented' instructions, like walk & bike instructions

    originalData : null,

    text         : null,   // content 
    cls          : null,   // element css style
    iconCls      : null,   // icon css style
    icon         : null,   // icon url
    leaf         : true,   // ext tree thang (can be ignored)

    initialize : function(config)
    {
        otp.configure(this, config);
    },

    makeImg : function(isUnselectable)
    {
        var icon = this.icon      ? this.icon : 'images/ui/s.gif';
        var cls  = this.iconCls   ? ' class="' + this.iconCls + '" ' : '';
        var sel  = isUnselectable ? ' unselectable="on" ' : '';

        return '<img src="' + icon + '" ' + cls + sel + '/>';
    },

    makeDiv : function(content, dontClose)
    {
        var id   = this.id  ? ' id="'    + this.id    + '" ' : ' ';
        var cls  = this.cls ? ' class="' + this.cls + '" ' : ' ';

        var retVal = '<div ' + cls + id + '>';
        if (content)
        {
            retVal += content;

            // when content is present, close the div by default, unless told otherwise
            if(!dontClose)
                retVal += '</div>';
        }

        return retVal;
    },

    CLASS_NAME : "otp.planner.StepData"
};
otp.planner.StepData = new otp.Class(otp.planner.StepData);


/**
  * ItineraryDataFactory
  * 
  * otp.planner.ItineraryDataFactory's purpose is represent the discrete pieces of data making up an Itinerary.
  * Here, we parse the trip XML data into object form that will be used by Itinerary.js and Printing.js objects.
  */
otp.planner.ItineraryDataFactoryStatic = {

    // requried params 
    id            : null,
    store         : null,
    templates     : null,
    locale        : null,
    showStopCodes : false,

    from          : null,
    to            : null,
    details       : null,
    modes         : null,  // otp.util.ItineraryModes (Modes.js line 126) ... requires an Itinerary object, and From store...

    // optional 
    dontEditStep     : false,
    useRouteLongName : false,
    LEG_ID           : '-leg-',

    // the thing this factory creates
    data    : null,

    /** constructor */
    initialize : function(config)
    {
        otp.configure(this, config);
        this.data    = this.makeItineraryData();
    },

    /** static factory returns @otp.planner.ItineraryData object */
    factory : function(config)
    {
        var factory = new otp.planner.ItineraryDataFactory(config);
        return factory.data;
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
    makeItineraryData : function()
    {
        var steps = new Array();
        var itinData = new otp.planner.ItineraryData({steps:steps});

        var blankImg = 'images/ui/s.gif';
        var num = 0;

        // step 1: start node
        var fmTxt = this.templates.TP_START.applyTemplate(this.from.data);
        var fmId  = this.id + '-' + otp.planner.Utils.FROM_ID;
        itinData.from = new otp.planner.StepData({id:fmId, cls:'itiny magnify', iconCls:'start-icon', icon:blankImg, text:fmTxt, num:num++, originalData:this.from});

        // step 2: leg (and sub-leg) instruction nodes
        var containsBikeMode    = false;
        var containsCarMode     = false;
        var containsTransitMode = false;

        var numLegs = this.store.getCount();
        for(var i = 0; i < numLegs; i++)
        {
            var leg                = this.store.getAt(i);
            leg.data.showStopCodes = this.showStopCodes;

            var text         = null;
            var verb         = null;
            var sched        = null;
            var mode         = leg.get('mode').toLowerCase();
            var agencyId     = leg.get('agencyId');
            var routeId      = leg.get('routeShortName');
            var instructions = null;
            var isLeaf       = true;
            var legId = this.id + this.LEG_ID + i;

            // step 2a: build either a transit leg node, or the non-transit turn-by-turn instruction nodes
            if(otp.util.Modes.isTransit(mode))
            {
                var routeName = this.makeRouteName(leg)
                leg.set('routeName', routeName);
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
                    instructions = this.makeInstructionStepsNodes(leg.data.steps, verb, legId, this.dontEditStep);
                    if(instructions && instructions.length >= 1)
                        isLeaf = false;
                    leg.data.formattedSteps = "";
                }
                text = this.templates[template].applyTemplate(leg.data);
            }

            // step 2b: make this leg (tree) node
            var icon = otp.util.imagePathManager.imagePath({mode:mode, agencyId:agencyId, route:routeId});
            var step = new otp.planner.StepData({id:legId, cls:'itiny magnify', iconCls:'itiny-inline-icon', icon:icon, text:text, num:num++, instructions:instructions, originalData:leg, leaf:isLeaf});

            // step 2c: push new step to return...
            steps.push(step);
        }

        // step 3: to node content
        var toTxt = this.templates.TP_END.applyTemplate(this.to.data);
        var toId  = this.id + '-' + otp.planner.Utils.TO_ID;
        itinData.to = new otp.planner.StepData({id:toId, cls:'itiny magnify', iconCls:'end-icon', text:toTxt, num:num++, originalData:this.to});

        // step 4: build details node's content
        var tripDetailsDistanceVerb = this.locale.instructions.walk_verb;
        if(containsBikeMode)
            tripDetailsDistanceVerb = this.locale.instructions.bike_verb;
        else if(containsCarMode) 
            tripDetailsDistanceVerb = this.locale.instructions.car_verb;
        var tripDetailsData = Ext.apply({}, this.details.data, {distanceVerb: tripDetailsDistanceVerb});

        var tpId  = this.id + '-' + otp.planner.Utils.TRIP_ID;
        var tpTxt = this.templates.TP_TRIPDETAILS.applyTemplate(tripDetailsData);
        itinData.details = new otp.planner.StepData({id:tpId, cls:'trip-details-shell', iconCls:'no-icon', text:tpTxt, num:num++});

        // step 5: mode note
        if(this.modes && this.modes.getMessage())
        {
            var m = this.modes.getMessage();
            var i = "images/ui/trip/caution.gif";
            if(this.modes.itineraryMessages && this.modes.itineraryMessages.icon)
                i = this.modes.itineraryMessages.icon;

            itinData.notes = new otp.planner.StepData({id:tpId+'-modeinfo', cls:'itiny-note', iconCls:'itiny-step-icon', icon:i, text:m, num:num++});
        }

        return itinData;
    },


    /** 
     * pass in a transit leg record, and build the route name from GTFS route-short-name and route-long-name
     * e.g, TriMet has route-short-name=1 and route-long-name=Vermont...thus the full route name of 1-Vermont
     *
     * @see override this method for your own template
     */
    makeRouteName : function(rec)
    {
        var routeName = rec.get('routeShortName');

        // step 1: configure parameter must be set to true
        if(this.useRouteLongName)
        {
            var routeLongName = rec.get('routeLongName');
    
            // step 2: make sure routeName has something
            if(routeName == null || routeName.length < 1)
                routeName = routeLongName;
    
            // step 3: construct route name as combo of routeName (id) and routeLongName
            if(routeLongName && routeLongName != routeName)
                routeName = routeName + "-" + routeLongName;
        }
        return routeName;
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

            var text = this.addNarrativeToStep(step, verb, stepNum);
            var cfg = {id:legId + "-" + i, text:step.narrative, cls:'itiny-steps', iconCls:'itiny-step-icon', icon:step.iconURL, text:text, num:stepNum++, originalData:step};
            var node = new otp.planner.StepData(cfg);
            retVal.push(node);
        }

        return retVal;
    },

    /** 
     * adds narrative and direction information to the step
     * NOTE: this method has an intentional side-effect of chaning @param step 
     *       (see below -- specifying 4th @param dontEditStep == true will avoid this)
     */ 
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
        stepText += ' - ' + otp.planner.Utils.prettyDistance(step.distance) + '';

        // edit the step object (by default, unless otherwise told)
        if(!dontEditStep)
        {
            // SIDE EFFECT -- when param dontEditStep is null or false, we'll do the following side-effects to param step
            step.narrative  = stepText;
            step.iconURL    = iconURL;
            step.bubbleHTML = '<img src="' + iconURL + '"></img> ' + ' <strong>' + stepNum + '.</strong> ' + step.streetName;
            step.bubbleLen  = step.streetName.length + 3;
        }

        return stepText;
    },

    CLASS_NAME: "otp.planner.ItineraryDataFactory"
};
otp.planner.ItineraryDataFactory = new otp.Class(otp.planner.ItineraryDataFactoryStatic);
