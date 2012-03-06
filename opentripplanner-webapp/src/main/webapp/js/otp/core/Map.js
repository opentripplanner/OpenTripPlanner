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

    routerId          : null,
    locale            : null,
    map               : null,
    baseLayer         : null,
    mapDiv            : "map",

    // list of functions that will be called before/after all features on the map are removed
    beforeAllFeaturesRemoved: [],
    allFeaturesRemoved: [],

    // Options passed into the OpenLayers.Map constructor
    options          : null,
    baseLayerOptions : null,
    THIS             : null,
    CLOSE_ZOOM       : 18,
    tooltipLinks     : true,
    tooltipCleared   : true,

    /**
     * An OpenLayers.Bounds object that defines the default extent used when
     * displaying the map.
     * 
     * Setting this to "automatic" will cause the trip planner to set this to
     * the extent of the data being routed on from the server.
     * 
     */
    defaultExtent : null,

    /** use attribution if you want the same attribute on both tile sets */
    attribution       : null,

    /** OL Controls on/off switches */
    zoomWheelEnabled  : true,
    handleRightClicks : false,
    permaLinkEnabled  : false,
    historyEnabled    : true,
    rightClickZoom    : true,
    layerSwitchEnabled: false,

    /*
     * Projections - neither should need changing. displayProjection is only
     * used within OpenLayers for now -- from/to form fields are populated using
     * dataProjection
     */
    dataProjection    : new OpenLayers.Projection("EPSG:4326"),

    /** 
     * Creates a new Map -- called by default when a new map is created
     * 
     * @param controls:null to get the default control (or supply an array of custom controls)
     * @param numZoomLevels can be increased to 14...but not recommended for a public map.
     * 
     * IMPORTANT: when adding new layers to this OL map, make sure you add layer.OTP_LAYER = true
     *            if you expect the layer to be cleared by _removeAllFeatures() below (e.g., things
     *            like OTP route highlight layers, etc...).
     * 
     * @methodOf otp.Map
     */
    initialize : function(config)
    {
        otp.configure(this, config);

        this.map = otp.util.OpenLayersUtils.makeMap(this.mapDiv, this.options);
        if (this.baseLayer == null) {
            this.baseLayer = otp.util.OpenLayersUtils.makeMapBaseLayer(this.map, this.baseLayerOptions);
        } else {
            this.map.addLayers(this.baseLayer);
            if (this.baseLayer.length > 1) {
                this.layerSwitchEnabled=true;
            }
        }
        this.map.setBaseLayer(this.baseLayer, true);
        this.map.events.register('click', this, this.closeAllPopupsCB);

        otp.core.MapSingleton = this;
        var self = this;
        otp.core.MapStatic.THIS = this;

        // if we have an empty array of controls, then add the defaults
        if (this.options.controls != null && this.options.controls.length == 0)
        {
            this.options.controls = otp.util.OpenLayersUtils.defaultControls(this.map, this.zoomWheelEnabled, this.handleRightClicks, this.permaLinkEnabled, this.attribution, this.historyEnabled, this.layerSwitchEnabled);
        }

        if (this.defaultExtent === 'automatic') {
            // ask the server for the extent

            // these two variables get enclosed over
            // the map gets zoomed when they are both true so that proper zoom levels
            // get calculated
            // without these checks, we were getting issues where the map would be zoomed
            // all the way out when the default extent was specified
            var layerLoaded = false;
            var extentRetrieved = false;
            var _params = {};
            if (this.routerId)
                _params = { routerId : this.routerId };
                OpenLayers.Request.GET({
                    // TODO: store the base /ws URL someplace else
                    url: '/opentripplanner-api-webapp/ws/metadata',
                    params: _params,
                    // TODO: switch other ajax requests from XML to JSON?
                    headers: {Accept: 'application/json'},
                    success : function(xhr) 
                    {
                          var metadata = Ext.util.JSON.decode(xhr.responseText);
                          self.defaultExtent = new OpenLayers.Bounds(
                                  metadata.minLongitude,
                                  metadata.minLatitude,
                                  metadata.maxLongitude,
                                  metadata.maxLatitude
                          );
                          extentRetrieved = true;
                          if (layerLoaded) {
                              self.zoomToDefaultExtent();
                          }
                    },
                    failure: function(xhr)
                    {
                        console.log('failure retrieving default extent');
                    }
            });

            // TODO: rethink this ... very hacky to do this, very fragile code
            var zoomOnFirstLoad = function() {
                layerLoaded = true;
                if (extentRetrieved) {
                    self.zoomToDefaultExtent();
                }
                self.map.baseLayer.events.un({loadend: zoomOnFirstLoad});
            };
            self.map.baseLayer.events.on({loadend: zoomOnFirstLoad});
        } else {
            var zoomOnFirstLoad = function() {
                self.zoomToDefaultExtent();
                self.map.baseLayer.events.un({loadend: zoomOnFirstLoad});
            };
            self.map.baseLayer.events.on({loadend: zoomOnFirstLoad});
        }
    },
    
    /** */
    zoomToDefaultExtent : function() {
        try
        {
            if(this.defaultExtent && this.defaultExtent !== 'automatic')
            {
                this.zoomToExtent(this.defaultExtent.transform(this.dataProjection, this.map.getProjectionObject()));
            }
        }
        catch(e)
        {}
    },


    /** */
    getContextMenu : function(cm) {
        if(cm != null)
        {
            this.contextMenu = cm;
        }

        var retVal = [];
        if(this.rightClickZoom)
        {
            retVal.push({
                text    : this.locale.contextMenu.centerHere,
                iconCls : 'cCenterMap',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                }
            });
            retVal.push({
                text    : this.locale.contextMenu.zoomInHere,
                iconCls : 'cZoomIn',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                    this.map.zoomIn();
                }
            });
            retVal.push({
                text    : this.locale.contextMenu.zoomOutHere,
                iconCls : 'cZoomOut',
                scope   : this,
                handler : function () {
                    this.contextMenu.centerMapFromContextMenuXY();
                    this.map.zoomOut();
                }
            });
        }

        // nav history 
        // TODO: where does this.controls.hist get set?  OL?
        if(this.historyEnabled && this.controls && this.controls.hist)
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
        this.zoomToDefaultExtent();
        this.cleanMap();
    },

    /**
     * Requests the extent of the region being routed on from the server and
     * caches it locally.
     */
    getDefaultExtentFromServer : function()
    {
        if (this.defaultExtent != "automatic" && this.defaultExtent != null)
        {
            return this.defaultExtent;
        }
        OpenLayers.Request.GET({
            // TODO: store the base /ws URL someplace else
            url : '/opentripplanner-api-webapp/ws/metadata',
            // TODO: switch other ajax requests from XML to JSON?
            headers: {Accept: 'application/json'},
            success : function(xhr) 
                      {
                          var metadata = Ext.util.JSON.decode(xhr.responseText);
                          otp.core.MapSingleton.defaultExtent = new OpenLayers.Bounds(
                                  metadata.minLongitude,
                                  metadata.minLatitude,
                                  metadata.maxLongitude,
                                  metadata.maxLatitude
                          );
                      },
            failure : function(xhr)
                      {
                          console.log("getRoutingExtent error:");
                          console.log(xhr);
                      }
        });
        return null;
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
        // bug in OL 2.8 that does not allow zoomIn() to work without first doing a zoomout...so we do it the long way...
        var zoom = this.map.getZoom();
        zoom++;
        this.map.zoomTo(zoom);
    },

    /** */
    zoomOut : function()
    {
        var self = otp.core.MapStatic.THIS;
        self.map.zoomOut();
    },

    /** */
    zoom : function(x, y, zoom, minZoom, maxZoom)
    {
        if(minZoom == null)
            minZoom = Math.floor(this.map.getNumZoomLevels() / 2);
        if(maxZoom == null)
            maxZoom = this.map.getNumZoomLevels() - 1;

        otp.util.OpenLayersUtils.panZoomWithLimits(this.map, x, y, zoom, minZoom, maxZoom);
    },

    /** */
    zoomToExtent : function(extent)
    {
        this.map.zoomToExtent(extent);
    },

    /** */
    zoomAllTheWayIn : function(x, y, zoom)
    {
        var self = otp.core.MapStatic.THIS;
        if(!zoom)
            zoom=self.CLOSE_ZOOM;

        // pan before zoom
        if(x && y)
            self.pan(x, y);

        self.map.zoomTo(zoom);
    },

    /** */
    pan : function(x, y)
    {
        otp.util.OpenLayersUtils.pan(this.map, x, y);
    },

// TODO : shouldn't this be in a spot like POI.js (there's a planner/poi, but nothing generic to the map right now, so guess that's why I put it here, but...) 

    /** */
    tooltip : function(x, y, html, contentSize)
    {
        var self = otp.core.MapStatic.THIS;

        // step 1: make lat/lon object and tooltip window (just once)
        var ll = otp.util.OpenLayersUtils.getLonLat(self.map, x, y);
        if (!self.tooltipPopup) 
        {
            // popup for map tooltips
            OpenLayers.Popup.ToolTip = OpenLayers.Class(OpenLayers.Popup, { 
                'contentDisplayClass': 'mapTooltipPopup' 
            }); 
            self.tooltipPopup = new OpenLayers.Popup.ToolTip("mapTooltipPopup", null, new OpenLayers.Size(155, 16), null, false);
            self.tooltipPopup.setOpacity(1.00);
            self.map.addPopup(self.tooltipPopup);
        }

        // step 2: add streetview and zoom in links to html
        if(self.tooltipLinks)
        {
            // TODO - localize Zoom In and StreetView strings

            // zoom in link if we're close in, else show the zoom out
            var zoom = "";
            if(self.map.getZoom() < (self.CLOSE_ZOOM - 1))
                zoom = ' <a href="#" onClick="otp.core.MapStatic.zoomAllTheWayIn(' + x + ',' + y  + ');">' + self.locale.contextMenu.zoomInHere + '</a>';
            else
                zoom = ' <a href="#" onClick="otp.core.MapStatic.zoomOut();">' + self.locale.contextMenu.zoomOutHere + '</a>';

            // IE can't do streetview in these map tooltips (freeze's the browser)
            if(Ext.isIE)
                this.noStreetview = true;

            var streetview = null;
            if(!this.noStreetview)
            {
                // if content is longer than 25 characters, we lack tooltip space, so don't break the links to next line nor use the (@Google)
                var svConf = {name:'sv', x:x, y:y};
                if(contentSize && contentSize <= 25)
                {
                    html += '<br/>';
                    svConf.name = 'Streetview (&copy; Google)'
                }
                else
                { 
                    html += ' ';
                    svConf.name = 'Streetview';
                }
                streetview = otp.planner.Templates.THIS.streetviewTemplate.applyTemplate(svConf);
            }


            // append links to tooltip content
            html += '<span class="popLinks">' +  zoom; 

            if(streetview)
            {
                 html += ' | ' + streetview
            }
            html += '</span>';
        }

        self.tooltipPopup.setContentHTML(html);
        self.tooltipPopup.lonlat = ll;
        self.tooltipPopup.updatePosition();
        self.tooltipPopup.show();
    },

    /** */
    tooltipHide : function()
    {
        var self = otp.core.MapStatic.THIS;
        if(self.tooltipPopup)
            self.tooltipPopup.hide()
    },


    /** */
    streetview : function(x, y) 
    {
        var self = otp.core.MapStatic.THIS;
        var ll = otp.util.OpenLayersUtils.getLonLat(self.map, x, y);
        if (!self.streetviewPopup) 
        {
            // popup for map tooltips
            OpenLayers.Popup.StreetView = OpenLayers.Class(OpenLayers.Popup, { 
                'contentDisplayClass': 'mapStreetviewPopup',
                'panMapIfOutOfView'  : true
            }); 
            self.streetviewPopup = new OpenLayers.Popup.StreetView("mapStreetViewPopup", null, new OpenLayers.Size(300, 200), null, true, self.streetviewHide);
            self.map.addPopup(self.streetviewPopup);
        }

        var html = '<iframe width="95%" height="90%" frameborder="0" scrolling="no" marginheight="0" marginwidth="0" '
                 + ' src="http://maps.google.com/maps?ie=UTF8&t=m&vpsrc=0&layer=c&source=embed&output=svembed&cbp=13,,,,&cbll=' + y + ',' + x + '&ll=' + y + ',' + x + '&z=18"></iframe>';

        self.streetviewPopup.setContentHTML(html);
        self.streetviewPopup.lonlat = ll;
        self.streetviewPopup.updatePosition();
        self.streetviewPopup.setBackgroundColor('0xFFFFFF');
        self.streetviewPopup.show();
    },

    /** */
    streetviewHide : function()
    {
        var self = otp.core.MapStatic.THIS;
        if(self.streetviewPopup)
            self.streetviewPopup.hide()
    },

    /** */
    cleanMap : function()
    {
        this.closeAllPopups()
        this._removeAllFeatures();
    },

    /** hide / close anything on the map */
    closeAllPopups : function()
    {
        this.tooltipHide();
        this.streetviewHide();
        this.tooltipCleared = true;
    },

    /** event callback --- override to clear popups */
    closeAllPopupsCB : function()
    {
        this.closeAllPopups();
    },

    /**
     * Remove all OTP features from non base layers on the map
     */
    _removeAllFeatures : function()
    {
        // mini events system
        // if this expands some more, we should have a more proper event system
        // with an interface in front of it to add/remove events
        Ext.each(this.beforeAllFeaturesRemoved, function(fn) {
            fn.call(this);
        }, this);
        for (var i = 0; i < this.map.layers.length; i++)
        {
            var layer = this.map.layers[i];
            if (!layer.isBaseLayer && layer.OTP_LAYER)
            {
                if (layer.isVector)
                {
                    layer.removeFeatures(layer.features);
                }
            }
        }
        Ext.each(this.allFeaturesRemoved, function(fn) {
            fn.call(this);
        }, this);
    },

    CLASS_NAME : "otp.core.Map"
};

otp.core.Map = new otp.Class(otp.core.MapStatic);
