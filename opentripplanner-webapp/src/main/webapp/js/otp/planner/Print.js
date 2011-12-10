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

    // passed via global variable set in Planner.js (TODO: if no planner, create with new itinerary via url params)
    planner     : null,
    templates   : null,
    locale      : null,
    itinerary   : null,
    options     : null,
    current_map : null,
 
    // created  
    config      : null,
    params      : null,
    print_map   : null,
    dialog      : null,

    MAP_DIV     : 'map-print',

    /** CONTROLLER for print dialog */
    initialize : function(config)
    {
        otp.configure(this, config);
        this.config = otp.util.ObjUtils.getConfig(config);

        var p = window.opener.otp.planner.PrintStatic.planner;
        if(p)
            this.configViaPlanner(p);

        // do things like localize HTML strings, and custom icons, etc...
        otp.util.HtmlUtils.fixHtml(this.config);

        if(Ext.isIE)         // TODO: fix OL errors on IE
            otp.util.HtmlUtils.hideShowElement(this.MAP_DIV);
        else
            this.safeRenderMap();  
        this.safeWriteItinerary();
    },

    /** takes in a Planner.js object, and assigns important objects to this object */
    configViaPlanner : function(planner)
    {
        this.planner     = planner;
        this.templates   = planner.templates;
        this.locale      = planner.locale;
        this.itinerary   = planner.getActiveItinerary();
        this.options     = planner.map.options;
        this.current_map = planner.map.getMap();
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
        options.div=this.MAP_DIV;
        options.controls=controls;
        this.print_map = new OpenLayers.Map(options);

        // step 2: have to add all markers in a separate new layer (as opposed to vector layers, which are added directly)
        var markers = new OpenLayers.Layer.Markers( "print-markers" );
        this.print_map.addLayer(markers);
        markers.setZIndex(400);

        // step 3: add layer data to the map
        var lyrs = this.current_map.layers;
        for (var i = 0; i < lyrs.length; i++ )
        {
            // step 3a: ignore specially marked layers
            if (lyrs[i].DONT_PRINT) continue;

            // step 3b: handle marker data in a special layer
            if (lyrs[i].CLASS_NAME == "OpenLayers.Layer.Markers")
            {
                for (var j = 0; j < lyrs[i].markers.length; j++){
                    markers.addMarker(new OpenLayers.Marker(lyrs[i].markers[j].lonlat.clone(),lyrs[i].markers[j].icon.clone()) );
                }
                console.log("Print._makeMap Markers: " + lyrs[i].name);
            }
            // step 3c: clone other layers and add them to our map
            else
            {
                // NOTE: cloning vector layers seems to change the visibility
                var lyr = lyrs[i].clone(); 
                if (lyrs[i].visibility == true)
                    lyr.visibility = true;
                try {
                    this.print_map.addLayer(lyr);
                }
                catch(e) {}
                console.log("Print._makeMap Layer: " + lyr.name +  "  " + lyr.visibility + " " + lyr.getZIndex()); 
            }
        }

        // step 3: zoom to our map location
        try {
            this.print_map.setCenter(this.current_map.getCenter(), this.current_map.getZoom());
        }
        catch(e) {}
    },

    /** */
    safeRenderMap : function()
    {
        try
        {
            this.renderMap();
        }
        catch(e)
        {}
    },

    /**  */
    writeItinerary : function()
    {
        console.log('enter writeItinerary');

        // step 1: get itinerary data
        console.log('writeItinerary - step 1: get itinerary data');
        var cfg  = {templates:this.templates, locale:this.locale, store:this.itinerary.m_legStore, modes:this.itinerary.m_modes, details:this.itinerary.xml, from:this.itinerary.from, to:this.itinerary.to, id:1, dontEditStep:true};
        var itin = otp.planner.ItineraryDataFactoryStatic.factory(cfg);

        // step 2: get divs that we will write into
        console.log('writeItinerary - step 2: get divs that we will write into');
        var headerDIV  = otp.util.HtmlUtils.getElement("header");
        var detailsDIV = otp.util.HtmlUtils.getElement("details");
        var legsDIV    = otp.util.HtmlUtils.getElement("legs");

        console.log(headerDIV);
        console.log(detailsDIV);
        console.log(legsDIV);

        // step 3: write the header
        console.log('writeItinerary - step 3: write the header'); 
        var from = itin.from.makeDiv(itin.from.makeImg(true) + ' ' + itin.from.text);
        var to   = itin.to.makeDiv(  itin.to.makeImg(true)   + ' ' + itin.to.text);
        headerDIV.innerHTML = from + to;


        // step 4: write the itinerary
        var text = '';
        text += from;

        if(itin.steps)
        for(var i = 0; i < itin.steps.length; i++)
        {
            // step 4a: make the leg div
            var step = itin.steps[i];
            text += step.makeDiv(step.makeImg(true) + step.text);

            // step 4b: iterate any sub-leg instructions (walking, biking)
            if(step.instructions && step.instructions.length > 0)
            {
                text += '<div class="instructions">';
                for(var j = 0; j < step.instructions.length; j++)
                {
                    var inst = step.instructions[j];
                    text += inst.makeDiv(inst.makeImg(true) + inst.text);
                }
                text += '</div>';
            }
        }

        // step 4c: close the itinerary
        text += to;
        legsDIV.innerHTML = text;

        // step 5: write the trip details
        var details = "";
        if(itin.notes)   details += itin.notes.makeDiv(itin.notes.makeImg(true) + itin.notes.text);
        if(itin.details) details += itin.details.text;
        detailsDIV.innerHTML = details;
    },

    /** */
    safeWriteItinerary : function()
    {
        try
        {
            this.writeItinerary();
        }
        catch(e)
        {}
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
