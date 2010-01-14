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

otp.namespace("otp.core");

/** singleton / static reference to our 'Map' */
otp.core.MapSingleton = null;

/**
 * Map is just that.  A simple class that encapsulates the building of a map. 
 * @class 
 */
otp.core.MapStatic = {

//http://maps.opengeo.org/geowebcache/service/wms?LAYERS=openstreetmap&FORMAT=image%2Fpng&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application%2Fvnd.ogc.se_inimage&SRS=EPSG:4326&BBOX=-8240523.1442212,4972687.3114282,-8238077.1593164,4975133.296333&WIDTH=256&HEIGHT=256

    locale            : otp.locale.English,

    map               : null,
    baseLayer         : null,

    // config for 4326
    mapDiv            : "map",
    url               : "http://maps.opengeo.org/geowebcache/service/wms",
    srsName           : "EPSG:4326",
    center            : {x:-122.66, y:45.53},
    layerNames        : ['openstreetmap'], 
    units             : null,
    numZoomLevels     : null,
    maxResolution     : null,
    maxExtent         : null,

/*
    // config for 4326
    maxResolution     : 0.703125,
    maxExtent         : new OpenLayers.Bounds(-180.0,-90.0,180.0,90.0),
    units             : "meters", // note meters & feed cause line to be NaN
    numZoomLevels     : 19,

    // config for google's projection
    srsName           : "EPSG:900913",
    units             : "meters",
    //maxResolution     : 156543.03396025,
    maxResolution     : 156543.03396025,
    maxExtent         : new OpenLayers.Bounds(-2.003750834E7,-2.003750834E7, 2.003750834E7,2.003750834E7),
    center            : {x:-13655812, y:5704158},
*/

    /** use attribution if you want the same attribute on both tile sets */
    attribution       : null,
    transitionEffect  :'resize',

    /** OL Controls on/off switches */
    zoomWheelEnabled  : true,
    handleRightClicks : false,
    permaLinkEnabled  : false,
    historyEnabled    : true,
    controls          : [],

    /** 
     * Creates a new Map -- called by default when a new map is created
     * 
     * @param controls:null to get the default control (or supply an array of custom controls)
     * @param numZoomLevels can be increased to 14...but not recommended for a public map.
     * 
     * @methodOf otp.Map
     */
    initialize : function(config)
    {
        console.log("enter Map constructor");
        otp.configure(this, config);

        this.map       = otp.util.OpenLayersUtils.makeMap(this.controls, this.srsName, this.mapDiv, this.numZoomLevels, this.units, this.maxExtent, this.maxResolution);
        this.baseLayer = otp.util.OpenLayersUtils.makeMapBaseLayer(this.map, this.url, this.layerNames, this.tileBuffer, this.transitionEffect, this.attribution);
        this.map.setBaseLayer(this.baseLayer, true);

        // if we have an empty array of controls, then add the defaults
        // if this.controls == null, then don't create controls (default skinny nav control will appear)
        if(this.controls != null && this.controls.length == 0)
        {
            this.controls = otp.util.OpenLayersUtils.defaultControls(this.map, this.zoomWheelEnabled, this.handleRightClicks, this.permaLinkEnabled, this.attribution, this.historyEnabled);
        }

        this.clear();
        otp.core.MapSingleton = this;
        console.log("exit Map constructor");
    },

    /** */
    getContextMenu : function(cm)
    {
        if(cm != null)
        {
            this.contextMenu = cm;
        }


        var retVal = [
            {
                text    : this.locale.contextMenu.centerHere,
                iconCls : 'cCenterMap',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                }
            },
            {
                text    : this.locale.contextMenu.zoomInHere,
                iconCls : 'cZoomIn',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                    this.map.zoomIn();
                }
            }
            ,
            {
                text    : this.locale.contextMenu.zoomOutHere,
                iconCls : 'cZoomOut',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                    this.map.zoomOut();
                }
            }
        ];

        // nav history
        if(this.controls && this.controls.hist) 
        {
            retVal.push(
            {
                text:    this.locale.contextMenu.previous,
                iconCls: 'cPrevious',
                handler: this.controls.hist.previous.trigger
            });
            retVal.push(
            {
                text: this.locale.contextMenu.next,
                iconCls: 'cNext',
                handler: this.controls.hist.next.trigger
            });
        }

        return retVal;
    },

    /**
     * @methodOf otp.core.Map
     * @returns a pointer to the OpenLayers map object
     */
    getMap : function()
    {
        return this.map;
    },

    /** */
    updateSize : function()
    {
        this.map.updateSize();
    },

    /** */
    clear : function()
    {
        this.updateSize();
        this.map.setCenter(new OpenLayers.LonLat(this.center.x, this.center.y), 11);
    },

    /** */
    centerMapAtPixel: function(x, y)
    {
        var px = new OpenLayers.Pixel(x, y);
        var ll = this.map.getLonLatFromPixel(px);
        this.map.panTo(ll);
    },

    /** */
    zoomIn : function()
    {
        this.map.zoomIn();
    },

    /** */
    zoomOut : function()
    {
        this.map.zoomOut();
    },

    /** */
    zoom : function(x, y, zoom, minZoom, maxZoom)
    {
        if(minZoom == null)
            minZoom = 4
        if(maxZoom == null)
            maxZoom = minZoom + 3;

        top.util.OpenLayersUtils.panZoomWithLimits(this.map, x, y, zoom, minZoom, maxZoom);
    },

    /** */
    zoomToExtent : function(extent)
    {
        try
        {
            this.map.zoomToExtent(extent);
        }
        catch(e)
        {
            console.log("exception Map.zoomToExtent" + e)
        }
    },
    
    CLASS_NAME : "otp.core.Map"
};

otp.core.Map = new otp.Class(otp.core.MapStatic);
