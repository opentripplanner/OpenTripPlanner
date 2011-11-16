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
    options     : null,
    current_map : null,
    itinerary   : null,
    tripTab     : null,
 
    // created  
    config      : null,
    params      : null,
    print_map   : null,

    /** */
    initialize : function(config)
    {
        otp.configure(this, config);
        otp.configure(this, window.opener.otp.planner.PrintStatic);

        this._makeMap();
        this._renderTrip();
    },


    /** render trip on map */
    _renderTrip : function()
    {
        if(this.itinerary != null)
        {
            this.drawItineraryIntoPrintMap(this.itinerary, this.print_map);
        }
    },

   /**
    * will call map & trip planner form routines in order to populate based on URL params
    */
    _makeMap : function()
    {
        // step 1: make a new print map
        var options = {div:'map-print', controls:[]}; 
        otp.extend(options, this.options);
        this.print_map = new OpenLayers.Map(options);

        // debug (mouse control) when on localhost
        if(otp.isLocalHost())
        {
            var n = new OpenLayers.Control.Navigation({zoomWheelEnabled:true, handleRightClicks:true});
            this.print_map.addControl(n);
        }

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
    },

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
        map.zoomToExtent(itin.map.getMap().getExtent())
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
