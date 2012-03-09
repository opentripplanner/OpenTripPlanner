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
  * Web Map / TripPlanner
  * 
  * Will render a trip itinerary into various mediums (Ext Trees, OL Map Layers, plain html page, email, etc...).
  * 
  * Renderer is created by Planner.  It is used by Planner & TripTab to produce output of the trip for the given endpoint.
  */
otp.planner.Renderer = {

    map                      : null,
    locale                   : null,
    templates                : null,

    m_markerLayer            : null,
    m_vectorLayer            : null,

    m_markerLayerAlternative : null,
    m_vectorLayerAlternative : null,

    // these members are set & re-set by the TripTab routines 
    m_tree        : null,
    m_itinerary   : null,

    // controls how far to zoom into the map on leg clicks -- see legClick() below...
    zoomInLegClick         : 4,
    zoomInStartClick       : 2,
    zoomInEndClick         : 2,
    zoomInShowDetailsClick : 0,
    zoomInHideDetailsClick : 5,

    /** */
    initialize : function(config)
    {
        otp.configure(this, config);
    },

    /** */
    draw : function(itin, tree)
    {
        if(itin != null)
        {
            this.m_itinerary = itin;
            this.m_tree      = tree;
            this.drawItineraryOntoMap();
        }
    },

    /** */
    clear : function()
    {
        this.map.cleanMap();
    },

    /** clear the ghost geom from mouse hover */
    clearAlternatives : function(itin) {
        if (itin == this.m_itinerary)
            return;

        this.m_vectorLayerAlternative.destroyFeatures(this.m_vectorLayerAlternative.features);
    },


    /** */
    drawItineraryOntoMap : function()
    {
        if(this.m_vectorLayer == null)
        {
            var vectorLayerOptions = {
                    isBaseLayer: false,
                    isFixed: false,
                    visibility: true,
                    projection: this.map.dataProjection,
                    displayInLayerSwitcher: false
            };
            this.m_vectorLayer = new OpenLayers.Layer.Vector('trip-vector-layer', vectorLayerOptions);
            this.m_vectorLayer.OTP_LAYER = true;
            this.map.getMap().addLayer(this.m_vectorLayer);
            this.m_vectorLayer.setZIndex(222);   // HACK: sets click index of trip back for clicability of other map layers

            var style = otp.util.OpenLayersUtils.getMarkerStyle();
            var styleMap = new OpenLayers.StyleMap(style);
            var uniqueValueRules = otp.util.OpenLayersUtils.getMarkerUniqueValueRules();
            styleMap.addUniqueValueRules("default", "type", uniqueValueRules);

            var markerLayerOptions = {
                    isBaseLayer: false,
                    rendererOptions: {yOrdering: true},
                    projection: this.map.dataProjection,
                    styleMap: styleMap,
                    displayInLayerSwitcher: false
            };
            this.m_markerLayer = new OpenLayers.Layer.Vector('trip-marker-layer', markerLayerOptions);
            this.m_markerLayer.OTP_LAYER = true;
            this.map.getMap().addLayer(this.m_markerLayer);
            this.m_markerLayer.setZIndex(223);   // HACK: sets click index of trip back for clickability of other map layers
        }

        // draw graphic plan on the map
        this.clear();
        this.m_itinerary.draw(this.m_vectorLayer, this.m_markerLayer);
        this.map.zoomToExtent(this.m_itinerary.getExtent());

        // draw text plan on the ui (tree)
        otp.util.ExtUtils.clearTreeNodes(this.m_tree);
        var n = this.m_itinerary.getTreeNodes(this.legClick, this);
        this.m_tree.root.appendChild(n);
    },

     /** */
    drawItineraryIntoPrinter : function()
    {
    },

    /** */
    drawItineraryIntoEmail : function()
    {
    },

    /** draw the ghost hover of alternative routes */
    drawItineraryAlternative : function(itin)
    {
        if(this.m_vectorLayerAlternative == null)
        {
            var vectorLayerOptions = {
                    isBaseLayer: false,
                    isFixed: false,
                    visibility: true,
                    projection: this.map.dataProjection
            };
            this.m_vectorLayerAlternative = new OpenLayers.Layer.Vector('trip-vector-layer-alternative', vectorLayerOptions);
            this.m_vectorLayerAlternative.setOpacity(0.5);
            this.m_vectorLayerAlternative.OTP_LAYER = true;
            this.map.getMap().addLayer(this.m_vectorLayerAlternative);
                                    
            var style = otp.util.OpenLayersUtils.getMarkerStyle();
            var styleMap = new OpenLayers.StyleMap(style);
            var uniqueValueRules = otp.util.OpenLayersUtils.getMarkerUniqueValueRules();
            styleMap.addUniqueValueRules("default", "type", uniqueValueRules);
            
            var markerLayerOptions = {
                    isBaseLayer: false,
                    rendererOptions: {yOrdering: true},
                    projection: this.map.dataProjection,
                    styleMap: styleMap
            };
            this.m_markerLayerAlternative = new OpenLayers.Layer.Vector('trip-marker-layer-alternative', markerLayerOptions);
            this.m_markerLayerAlternative.setOpacity(0.4);
        }

        // draw graphic plan on the map
        this.m_vectorLayerAlternative.destroyFeatures(this.m_vectorLayerAlternative.features);
        if (itin == this.m_itinerary)
            return;

        itin.draw(this.m_vectorLayerAlternative, null);
    },


    /**
     * callback to the itinerary legs for mouse-click / zoom purposes
     */
    legClick : function(node, event)
    {
        if(node.id.indexOf(otp.planner.Utils.TRIP_ID) >= 0)
        {
            try
            {
                this.map.zoomToExtent(this.m_markerLayer.getDataExtent());
            }
            catch(e)
            {}
        }
        else
        {
            var zInc = null; 
            try
            {
                // change the 'Show Details...' / 'Hide Details...' link string when sub-tree is open / close
                if(node.showDetailsId)
                {
                    var content;
                    if(node.showing)
                    {
                        content = this.templates.getShowDetails();
                        node.showing = false;
                        zInc = this.zoomInShowDetailsClick; 
                    }
                    else
                    {
                        content = this.templates.getHideDetails();
                        node.showing = true;
                        showDetails = false;
                        zInc = this.zoomInHideDetailsClick;
                    }
                    Ext.fly(node.showDetailsId).update(content);
                }
            }
            catch(e)
            {
                console.log("EXCEPTION leg click callback - expand : " + e);
            }

            try
            {
                var coord = null;
                if(node.id.indexOf(otp.planner.Utils.FROM_ID) >= 0)
                {
                    coord = this.m_itinerary.getFrom();
                    zInc = this.zoomInStartClick;
                }
                else if(node.id.indexOf(otp.planner.Utils.TO_ID) >= 0)
                {
                    coord = this.m_itinerary.getTo();
                    zInc = this.zoomInEndClick;
                }
                else 
                {
                    coord = this.m_itinerary.getLegStartPoint(node.id);
                    if(zInc === null)
                        zInc = this.zoomInLegClick;
                }
                coord = coord.get('geometry');

                // zoom the map in x number of zoom levels off of the extent
                this.map.zoomToExtent(this.m_itinerary.getExtent());
                var z = this.map.getZoom();
                this.map.zoom(coord.x, coord.y, z + (zInc || 0));
            }
            catch(e)
            {
                console.log("EXCEPTION leg click callback - zoom to leg : " + e);
            }
        }
    },

    CLASS_NAME: "otp.planner.Renderer"
};

otp.planner.Renderer = new otp.Class(otp.planner.Renderer);
