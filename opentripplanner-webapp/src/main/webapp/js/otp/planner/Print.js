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
 * responsible for building and wiring together the trip planner application
 * will also parse any command line parameters, and execute any commands therein
 */
otp.planner.PrintStatic = {

    // passed
    map         : null,
    locale      : null,
    options     : null,
    current_map : null,
    itinerary   : null,
    planner     : null,
    url         : 'print.html',
 
    // created  
    config      : null,
    params      : null,
    print_map   : null,
    dialog      : null,

    /** CONTROLLER for print dialog */
    initialize : function(config)
    {
        otp.configure(this, config);
        otp.configure(this, window.opener.otp.planner.PrintStatic);
        this.config = otp.util.ObjUtils.getConfig(config);

        // do things like localize HTML strings, and custom icons, etc...
        otp.util.HtmlUtils.fixHtml(this.config);

        this.renderMap();
        this.writeItinerary();
    },

    /** static method to open printing dialog */
    print : function(url)
    {
        console.log("Print.print: open window & bring it to the front");
        otp.planner.PrintStatic.dialog = window.open(url,'WORKING','width=850,height=630,resizable=1,scrollbars=1,left=100,top=100,screenX=100,screenY=100');
        otp.planner.PrintStatic.dialog.focus();
        otp.util.Analytics.gaEvent(otp.util.Analytics.OTP_TRIP_PRINT);
    },

   /**
    * will call map & trip planner form routines in order to populate based on URL params
    */
    renderMap : function()
    {
        var controls = [];

        // step 0: debug (mouse control) when on localhost
        if(otp.isLocalHost())
        {
            var n = new OpenLayers.Control.Navigation({zoomWheelEnabled:true, handleRightClicks:true});
            controls.push(n);
        }

        // step 1: make a new print map
        var options = {}; 
        otp.extend(options, this.options);
        options.div='map-print';
        options.controls=controls;
        this.print_map = new OpenLayers.Map(options);

        // NOTE: have to add all markers in a separate new layer (as opposed to vector layers, which are added directly)
        var markers = new OpenLayers.Layer.Markers( "print-markers" );
        this.print_map.addLayer(markers);
        markers.setZIndex(400);

        // step 2: add layer data to the map
        var lyrs = this.current_map.layers;
        for (var i = 0; i < lyrs.length; i++ )
        {
            // step 2a: handle marker data in a special layer
            if (lyrs[i].CLASS_NAME == "OpenLayers.Layer.Markers")
            {
                for (var j = 0; j < lyrs[i].markers.length; j++){
                    markers.addMarker(new OpenLayers.Marker(lyrs[i].markers[j].lonlat.clone(),lyrs[i].markers[j].icon.clone()) );
                }
                console.log("Print._makeMap Markers: " + lyrs[i].name);
            }
            // step 2b: clone other layers and add them to our map
            else
            {
                // NOTE: cloning vector layers seems to change the visibility
                var lyr = lyrs[i].clone(); 
                if (lyrs[i].visibility == true)
                    lyr.visibility = true;
                this.print_map.addLayer(lyr);
                console.log("Print._makeMap Layer: " + lyr.name +  "  " + lyr.visibility + " " + lyr.getZIndex()); 
            }
        }

        // step 3: zoom to our map location
        this.print_map.zoomToExtent(this.current_map.getExtent());
    },

    /**  */
    writeItinerary : function()
    {
        // TODO:  refactor this to use same code as in Itinerary.js makeTreeNodes() -- line 600 
        
        
        var itin = this.itinerary;
        var map  = this.print_map;

        var tpl = new otp.planner.Templates(this);
        var fromTXT    = this.getFromTXT(tpl, itin.from.data);
        var toTXT      = this.getToTXT(tpl, itin.to.data);

        var containsBikeMode    = false;
        var containsCarMode     = false;
        var containsTransitMode = false;

        var headerDIV  = Ext.get("header");
        var detailsDIV = Ext.get("details");
        var legsDIV    = Ext.get("legs");
        headerDIV.update(fromTXT + toTXT);

        var reportText = fromTXT;
        for(var i = 0; i < itin.m_legStore.getCount(); i++)
        {
            var leg = itin.m_legStore.getAt(i);
            leg.data.showStopIds = this.showStopIds;

            var text;
            var hasKids = true;
            var sched = null;

            var routeName = leg.get('routeName');
            var agencyId = leg.get('agencyId');
            var mode = this.getMode(leg);

            if(mode === 'walk' || mode === 'bicycle')
            {
                var verb = (mode === 'bicycle') ? 'Bike' : 'Walk';
                hasKids = false;

                var steps = leg.data.steps;
                var stepText = "";
                var noStepsYet = true;

                for (var j = 0; j < steps.length; j++)
                {
                    step = steps[j];
                    if (step.streetName == "street transit link")
                    {
                        // TODO: Include explicit instruction about entering/exiting transit station or stop?
                        continue;
                    }
                    stepText = "<li>";
                    var relativeDirection = step.relativeDirection;
                    if (relativeDirection == null || noStepsYet == true)
                    {
                        stepText += itin.locale.directions[verb] + ' ' + itin.locale.directions['to'] 
                                     + ' <strong>' + itin.locale.directions[step.absoluteDirection.toLowerCase()] 
                                     + '</strong> ' + itin.locale.directions['on'] + ' <strong>' + step.streetName + '</strong>';
                        noStepsYet = false;
                    }
                    else
                    {
                        relativeDirection = relativeDirection.toLowerCase();
                        var directionText = itin.locale.directions[relativeDirection];
                        directionText = directionText.substr(0,1).toUpperCase() + directionText.substr(1);
                        if (relativeDirection == "continue")
                        {
                            stepText += directionText + ' <strong>' + steps[j].streetName + '</strong>';
                        }
                        else if (step.stayOn == true)
                        {
                            stepText += directionText + " " + itin.locale.directions['to_continue'] + ' <strong>' + step.streetName + '</strong>';
                        }
                        else if (step.becomes == true)
                        {
                            stepText += directionText + ' <strong>' + steps[j-1].streetName + '</strong> ' +  itin.locale.directions['becomes'] + ' <strong>' + step.streetName + '</strong>';
                        }
                        else
                        {
                            stepText += directionText + " " + itin.locale.directions['at'] + ' <strong>' + step.streetName + '</strong>';
                        }
                    }
                    stepText += ' (' + otp.planner.Utils.prettyDistance(step.distance) + ')';
                } // for

                if (leg.data.toDescription == '')
                    leg.data.toDescription = null;

                // TODO: refactor see Itinerary.js line 600
                if(mode === 'bicycle') 
                    containsBikeMode = true;

                var template = mode === 'walk' ? 'TP_WALK_LEG' : 'TP_BICYCLE_LEG';
                text = tpl[template].applyTemplate(leg.data);
            }
            else
            {
                var order = leg.get('order');
                if (order == 'thru-route')
                {
                    text = tpl.getInterlineLeg().applyTemplate(leg.data);
                }
                else 
                {
                    leg.data.mode = OpenLayers.Lang.translate(leg.data.mode);
                    text = tpl.getTransitLeg().applyTemplate(leg.data);
                }
            }
            icon = otp.util.imagePathManager.imagePath({agencyId: agencyId, mode: mode, route: routeName});
            reportText = reportText + '<img src="' + icon + '"/> ' + text;
        }

        // step 3: build details node's content
        var tripDetailsDistanceVerb = this.locale.instructions.walk_verb;
        if(containsBikeMode)
            tripDetailsDistanceVerb = this.locale.instructions.bike_verb;
        else if(containsCarMode) 
            tripDetailsDistanceVerb = this.locale.instructions.car_verb;
        var tripDetailsData = Ext.apply({}, itin.xml.data, {distanceVerb: tripDetailsDistanceVerb});
        var detailsTXT = tpl.TP_TRIPDETAILS.applyTemplate(tripDetailsData);
        detailsDIV.update(detailsTXT);

        // TODO Itin note  Itinerary.js line 683

        reportText = reportText + toTXT;
        legsDIV.update(reportText);
    },

    getFromTXT : function(tpl, data)
    {
        var img = '<img class="start-icon" unselectable="on" src="images/ui/s.gif"/>'
        var txt = tpl.TP_START.applyTemplate(data);
        return img + txt;
    },

    getToTXT : function(tpl, data)
    {
        var img = '<img class="end-icon" unselectable="on" src="images/ui/s.gif"/>'
        var txt = tpl.TP_END.applyTemplate(data);
        return img + txt;
    },

    /** return mode string */
    getMode : function(leg)
    {
        return leg.get('mode').toLowerCase();
    },

    /** 1/2 thought out show/hide link */
    makeLink : function(linkDiv, xx)
    {
        var linkOpts    = Ext.get("link-options");
        var linkOptions = '<span class="span_hide_options" id="linklogo" onclick="showHideMap(\'logo\')">' + loc.buttons.hideBanner + '</span>'
                        + '<div style="clear:both"></div>';
        linkOpts.update(linkOptions);
    },

    CLASS_NAME : "otp.planner.Print"
};

otp.planner.Print = new otp.Class(otp.planner.PrintStatic);


/** 

    drawItineraryIntoPrintMap : function(itin, map)
    {
        // create vector & marker layers
        if(this.m_vectorLayer==null)
        {
            // step a: create vector layer
            var vrconfig = {isBaseLayer:false,isFixed:false,visibility:true,projection:new OpenLayers.Projection("EPSG:4326")};
            this.m_vectorLayer=new OpenLayers.Layer.Vector("trip-vector-layer", vrconfig);
            map.addLayer(this.m_vectorLayer);

            // step b: create marker layer
            var s=otp.util.OpenLayersUtils.getMarkerStyle();
            var sm=new OpenLayers.StyleMap(s);
            var mr=otp.util.OpenLayersUtils.getMarkerUniqueValueRules();
            sm.addUniqueValueRules("default","type",mr);
    
            var mrconfig={isBaseLayer:false,rendererOptions:{yOrdering:true},projection:new OpenLayers.Projection("EPSG:4326"),styleMap:sm};
            this.m_markerLayer=new OpenLayers.Layer.Vector("trip-marker-layer",mrconfig);
            map.addLayer(this.m_markerLayer)
        }
    
        this.m_itinerary=itin;
        this.m_vectorLayer.features=itin.m_vectors;
        this.m_markerLayer.features=itin.m_markers;
    },

 */