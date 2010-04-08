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

otp.namespace("otp.systemmap");

/**
  * Web System Map
  * 
  * otp.systemmap.Systemmap's purpose is to act as the main controller for the system map.  
  *
  * Coordinates the rendering of the system map, and interactions with it
  */
otp.systemmap.Systemmap = {

    locale        : otp.locale.English,

    map : null,
    layerRoutes : null,
    layerStops : null,

    layerUrlRoutes: null,
    layerUrlStops: null,
    layerNamesRoute: null,
    layerNamesStop: null,

    controlStopsHover: null,
    controlStopsClick: null,
    controlStopsUrl: null,

    initialize : function(config)
    {
        this.systemmap = this;
        otp.configure(this, config);
        this.loadSystemMap();
    },

    /**
     * load the system map behaviors to the map
     */
    loadSystemMap : function()
    {
        var map = this.map.getMap();
        if (!this.layerRoutes) {
            this.layerRoutes = new OpenLayers.Layer.WMS('systemmap-wms-layer-routes',
                                                        this.layerUrlRoutes,
                                                        {layers: this.layerNamesRoute, isBaseLayer: false, transparent: true});
            this.layerStops = new OpenLayers.Layer.WMS('systemmap-wms-layer-stops',
                                                       this.layerUrlStops,
                                                       {layers: this.layerNamesStop, isBaseLayer: false, transparent: true});

            map.addLayers([this.layerRoutes, this.layerStops]);

            // FIXME should make this configurable, if we want this at all?
            var layerSwitcherControl = new OpenLayers.Control.LayerSwitcher();
            map.addControl(layerSwitcherControl);
            layerSwitcherControl.activate();

            this.loadPopupBehavior();

            // FIXME this is a hack to get around the wms layers shadowing the trip vector layers
            // when a trip is planned
            // the trip layers are already on top of of the system map layers
            // so it's unclear to me why this doesn't already work
            // but calling raiseLayer seems to force the issue
            var raiseTripLayers = function() {
                var layersToRaise = [];
                Ext.each(map.layers, function(layer) {
                        if (layer.name.indexOf('trip') !== -1) {
                            layersToRaise.push(layer);
                        }
                    });
                Ext.each(layersToRaise, function(layer) {
                        map.raiseLayer(layer, 1);
                    });
            };
            this.layerRoutes.events.on({loadend: raiseTripLayers});
            this.layerStops.events.on({loadend: raiseTripLayers});
        }
    },

    /**
     * add the logic for the popups to the system map
     * safe to call multiple times
     */
    loadPopupBehavior : function() {
        var map = this.map.getMap();

        if (!this.controlStopsHover) {
            var lastHoveredPopup = null;
            this.controlStopsHover = new OpenLayers.Control.WMSGetFeatureInfo({
                    hover: true,
                    url: this.controlStopsUrl,
                    layerUrls: [this.layerUrlStops],
                    layers: [this.layerStops],
                    eventListeners: {
                        beforegetfeatureinfo: function(event) {
                            if (lastHoveredPopup) {
                                lastHoveredPopup.triggerClose(500);
                                lastHoveredPopup = null;
                            }
                        },
                        getfeatureinfo: function(event) {
                            try {
                                var stopInfo = Ext.util.JSON.decode(event.text);
                            } catch(err) {
                                return;
                            }
                            var popup = new otp.systemmap.Popup({map: map,
                                                                 doc: stopInfo,
                                                                 klass: 'olHoverPopup',
                                                                 displayDepartures: false,
                                                                 xy: event.xy
                                });
                            // keep a reference to the last popup displayed
                            // which will get closed when the next hover is triggered
                            lastHoveredPopup = popup;
                        }
                    }
            });

            this.controlStopsClick = new OpenLayers.Control.WMSGetFeatureInfo({
                    hover: false,
                    url: this.controlStopsUrl,
                    layerUrls: [this.layerUrlStops],
                    layers: [this.layerStops],
                    eventListeners: {
                        getfeatureinfo: function(event) {
                            try {
                                var stopInfo = Ext.util.JSON.decode(event.text);
                            } catch (err) {
                                return;
                            }
                            var popup = new otp.systemmap.Popup({map: map,
                                                                 doc: stopInfo,
                                                                 displayDepartures: true,
                                                                 xy: event.xy
                                });
                        }
                    }
                });

            map.addControl(this.controlStopsHover);
            this.controlStopsHover.activate();
            map.addControl(this.controlStopsClick);
            this.controlStopsClick.activate();
        }
    },
 
    CLASS_NAME: "otp.systemmap.Systemmap"
};

otp.systemmap.Systemmap = new otp.Class(otp.systemmap.Systemmap);
