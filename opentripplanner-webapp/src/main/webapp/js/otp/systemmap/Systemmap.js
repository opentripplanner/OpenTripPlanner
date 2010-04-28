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
    layerRoutesHighlighted: null,
    layerStopsHighlighted: null,

    layerUrlRoutes: null,
    layerUrlStops: null,
    layerUrlRoutesHighlighted: null,
    layerUrlStopsHighlighted: null,
    layerNamesRoute: null,
    layerNamesStop: null,
    layerNamesRouteHighlighted: null,
    layerNamesStopHighlighted: null,

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
            this.layerRoutesHighlighted = new OpenLayers.Layer.WMS('systemmap-wms-layer-routes-highlighted',
                                                        this.layerUrlRoutesHighlighted,
                                                        {layers: this.layerNamesRouteHighlighted, isBaseLayer: false, transparent: true});
            this.layerStopsHighlighted = new OpenLayers.Layer.WMS('systemmap-wms-layer-routes-highlighted',
                                                        this.layerUrlStopsHighlighted,
                                                        {layers: this.layerNamesStopHighlighted, isBaseLayer: false, transparent: true});

            map.addLayers([this.layerRoutes, this.layerStops, this.layerRoutesHighlighted, this.layerStopsHighlighted]);
            this.layerRoutesHighlighted.setVisibility(false);
            this.layerStopsHighlighted.setVisibility(false);

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
        var self = this;

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
                            //delete self.layerStopsHighlighted.params['CQL_FILTER'];
                            //self.layerStopsHighlighted.redraw();
                            self.layerStopsHighlighted.setVisibility(false);
                            //self.layerStops.setOpacity(1.0);
                        },
                        getfeatureinfo: function(event) {
                            try {
                                var doc = Ext.util.JSON.decode(event.text);
                            } catch(err) {
                                return;
                            }
                            if (!doc || !doc.type || doc.type !== 'stop') {
                                return;
                            }
                            doc = doc.stop;
                            var popup = new otp.systemmap.Popup({map: map,
                                                                 doc: doc,
                                                                 klass: 'olHoverPopup',
                                                                 displayDepartures: false,
                                                                 xy: event.xy,
                                                                 sysmap: self
                                });
                            // keep a reference to the last popup displayed
                            // which will get closed when the next hover is triggered
                            lastHoveredPopup = popup;

                            var stopId = doc.stopId;
                            self.layerStopsHighlighted.mergeNewParams({featureId: stopId});
                            self.layerStopsHighlighted.setVisibility(true);
                            //self.layerStops.setOpacity(0.3);
                        }
                    }
            });

            this.controlStopsClick = new OpenLayers.Control.WMSGetFeatureInfo({
                    hover: false,
                    url: this.controlStopsUrl,
                    layerUrls: [this.layerUrlStops],
                    layers: [this.layerRoutes, this.layerStops],
                    eventListeners: {
                        beforegetfeatureinfo: function(event) {
                            self.layerStopsHighlighted.setVisibility(false);
                            self.layerRoutesHighlighted.setVisibility(false);

                            self.layerStops.setOpacity(1.0);
                            self.layerRoutes.setOpacity(1.0);
                        },
                        getfeatureinfo: function(event) {
                            try {
                                var doc = Ext.util.JSON.decode(event.text);
                            } catch (err) {
                                return;
                            }
                            if (!doc || !doc.type || !(doc.type === 'stop' || doc.type === 'routes')) {
                                return;
                            }
                            if (doc.type === 'routes') {

                                // highlight all stops on the routes
                                var stopIds = [];
                                Ext.each(doc.stopids, function(stopId) {
                                    stopIds.push("stops." + stopId);
                                });
                                stopIds = stopIds.join(",");
                                the_stopids = stopIds;
                                self.layerStopsHighlighted.mergeNewParams({featureId: stopIds});
                                self.layerStopsHighlighted.setVisibility(true);

                                // highlight all routes
                                var routeIds = [];
                                Ext.each(doc.routes, function(route) {
                                        routeIds.push("routes." + route.routeId);
                                    });
                                routeIds = routeIds.join(",");
                                self.layerRoutesHighlighted.mergeNewParams({featureId: routeIds});
                                self.layerRoutesHighlighted.setVisibility(true);
                                
                            } else {
                                doc = doc.stop;
                                var popup = new otp.systemmap.Popup({map: map,
                                                                     doc: doc,
                                                                     displayDepartures: true,
                                                                     xy: event.xy,
                                                                     sysmap: self
                                    });
                                var stopId = "stops." + doc.stopId;
                                self.layerStopsHighlighted.mergeNewParams({featureId: stopId});
                                self.layerStopsHighlighted.setVisibility(true);
    
                                // highlight all routes serviced by this stop
                                var routes = doc.routes;
                                var routeIds = [];
                                Ext.each(routes, function(route) {
                                        routeIds.push("routes." + route.routeId);
                                    });
                                var featureId = routeIds.join(",");
                                self.layerRoutesHighlighted.mergeNewParams({featureId: featureId});
                                self.layerRoutesHighlighted.setVisibility(true);
                            }

                            // and dim the map
                            self.layerRoutes.setOpacity(0.4);
                            self.layerStops.setOpacity(0.4);
                        }
                    }
                });

            map.addControl(this.controlStopsHover);
            this.controlStopsHover.activate();
            map.addControl(this.controlStopsClick);
            this.controlStopsClick.activate();
        }
    },

    popupClosed: function() {
        this.layerStops.setOpacity(1.0);
        this.layerRoutes.setOpacity(1.0);
        this.layerRoutesHighlighted.setVisibility(false);
    },
 
    CLASS_NAME: "otp.systemmap.Systemmap"
};

otp.systemmap.Systemmap = new otp.Class(otp.systemmap.Systemmap);
