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
    templates   : null,
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
        // step 1: get itinerary data
        var cfg  = {templates:this.templates, locale:this.locale, store:this.itinerary.m_legStore, modes:this.itinerary.m_modes, details:this.itinerary.xml, from:this.itinerary.from, to:this.itinerary.to, id:1, dontEditStep:true};
        var itin = otp.planner.ItineraryDataFactoryStatic.factory(cfg);

        // step 2: get divs that we'll write into
        var headerDIV  = Ext.get("header");
        var detailsDIV = Ext.get("details");
        var legsDIV    = Ext.get("legs");

        // step 3: write the header
        var from = itin.from.makeDiv(itin.from.makeImg(true) + ' ' + itin.from.text);
        var to   = itin.to.makeDiv(  itin.to.makeImg(true)   + ' ' + itin.to.text);
        headerDIV.update(from + to);


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
        legsDIV.update(text);

        // step 5: write the trip details
        var details = "";
        if(itin.notes)   details += itin.notes.makeDiv(itin.notes.makeImg(true) + itin.notes.text);
        if(itin.details) details += itin.details.text;
        detailsDIV.update(details);
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
