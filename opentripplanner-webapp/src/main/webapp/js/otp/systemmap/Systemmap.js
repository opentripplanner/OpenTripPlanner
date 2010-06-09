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

    // we'll need to keep track of the highlighted stops that we need to display as state
    // so that a hover popup will only add to it instead of replacing it
    lastClickedPopup: null,
    lastHoveredPopup: null,

    // need to keep track of the state of the currently selected stops
    highlightedStops: [],

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
                        if (typeof layer.name === 'string' &&
                            layer.name.indexOf('trip') !== -1) {
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
     * return a comma separated list of currently active stops
     * concatentated with the stop ids passed into the function
     * note that this does not alter the highlightedStops array
     * This is the format that's required to pass to geoserver
     */
    highlightedStopFeatureIds: function() {
        var newids = Ext.flatten(arguments);
        var allids = this.highlightedStops.concat(newids);
        return allids.join(",");
    },

    /**
     * display a highlighted layer with the appropriate features highlighted
     * featureIdsAsString should be a comma separated string
     */
    displayHighlightedLayer: function(layer, featureIdsAsString) {
        if (featureIdsAsString === "") {
            delete layer.params['featureId'];
            layer.setVisibility(false);
        } else {
            layer.mergeNewParams({featureId: featureIdsAsString});
            layer.setVisibility(true);
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
            this.controlStopsHover = new OpenLayers.Control.WMSGetFeatureInfo({
                    hover: true,
                    url: this.controlStopsUrl,
                    layerUrls: [this.layerUrlStops],
                    layers: [this.layerStops],
                    eventListeners: {
                        getfeatureinfo: function(event) {
                            var nostops = false;
                            try {
                                var doc = Ext.util.JSON.decode(event.text);
                            } catch(err) {
                                nostops = true;
                            }
                            if (!doc || !doc.type || doc.type !== 'stop') {
                                nostops = true;
                            }
                            if (nostops) {
                                self.displayHighlightedLayer(self.layerStopsHighlighted, self.highlightedStopFeatureIds());
                                if (self.lastHoveredPopup) {
                                    self.lastHoveredPopup.triggerClose(500);
                                    self.lastHoveredPopup = null;
                                }
                                return;
                            }

                            doc = doc.stop;
                            var stopId = 'stops.' + doc.stopId;

                            // if we're hovering over the same stop, we don't need to do anything
                            if (self.lastHoveredPopup && self.lastHoveredPopup.id === stopId) {
                                return;
                            } else if (self.lastHoveredPopup) {
                                // but we need to clean up the old popup if it's a different one
                                self.lastHoveredPopup.triggerClose(500);
                                self.lastHoveredPopup = null;
                            }

                            // if a click popup exists for the same stop, we don't want to show a hover popup
                            if (self.lastClickedPopup && self.lastClickedPopup.id === stopId) {
                                self.displayHighlightedLayer(self.layerStopsHighlighted, self.highlightedStopFeatureIds());
                                return;
                            }

                            var popup = new otp.systemmap.Popup({map: map,
                                                                 id: stopId,
                                                                 doc: doc,
                                                                 klass: 'olHoverPopup',
                                                                 displayDepartures: false,
                                                                 xy: event.xy,
                                                                 sysmap: self
                                });
                            // keep a reference to the last popup displayed
                            // which will get closed when the next hover is triggered
                            self.lastHoveredPopup = popup;

                            self.displayHighlightedLayer(self.layerStopsHighlighted, self.highlightedStopFeatureIds(stopId));
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
                            if (self.lastClickedPopup) {
                                self.lastClickedPopup.triggerClose(500);
                                self.lastClickedPopup = null;
                            }
                            self.layerStopsHighlighted.setVisibility(false);
                            self.layerRoutesHighlighted.setVisibility(false);

                            self.layerStops.setOpacity(1.0);
                            self.layerRoutes.setOpacity(1.0);
                        },
                        getfeatureinfo: function(event) {
                            var nodata = false;
                            try {
                                var doc = Ext.util.JSON.decode(event.text);
                            } catch (err) {
                                nodata = true;
                            }
                            if (!doc || !doc.type || !(doc.type === 'stop' || doc.type === 'routes')) {
                                nodata = true;
                            }
                            if (nodata) {
                                self.highlightedStops = [];
                                return;
                            }
                            if (doc.type === 'routes') {

                                // highlight all stops on the routes
                                var stopIds = [];
                                Ext.each(doc.stopids, function(stopId) {
                                    stopIds.push("stops." + stopId);
                                });

                                // here we'll lose the previously selected stops, ok?
                                self.highlightedStops = stopIds;
                                
                                self.displayHighlightedLayer(self.layerStopsHighlighted, self.highlightedStopFeatureIds());

                                // highlight all routes
                                var routeIds = [];
                                Ext.each(doc.routes, function(route) {
                                        routeIds.push("routes." + route.routeId);
                                    });
                                routeIds = routeIds.join(",");
                                self.displayHighlightedLayer(self.layerRoutesHighlighted, routeIds);
                                
                            } else {
                                doc = doc.stop;
                                var stopId = "stops." + doc.stopId;
                                var popup = new otp.systemmap.Popup({map: map,
                                                                     id: stopId,
                                                                     doc: doc,
                                                                     displayDepartures: true,
                                                                     xy: event.xy,
                                                                     sysmap: self
                                    });
                                self.lastClickedPopup = popup;

                                // if we have a hover popup for the same stop, close that one immediately
                                if (self.lastHoveredPopup && self.lastHoveredPopup.id === stopId) {
                                    self.lastHoveredPopup.removePopup();
                                    self.lastHoveredPopup = null;
                                }
                                

                                // we lose previously selected stopids here
                                self.highlightedStops = [stopId];

                                self.displayHighlightedLayer(self.layerStopsHighlighted, self.highlightedStopFeatureIds());
    
                                // highlight all routes serviced by this stop
                                var routes = doc.routes;
                                var routeIds = [];
                                Ext.each(routes, function(route) {
                                        routeIds.push("routes." + route.routeId);
                                    });
                                var featureId = routeIds.join(",");
                                self.displayHighlightedLayer(self.layerRoutesHighlighted, featureId);
                            }

                            // and dim the map
                            self.layerRoutes.setOpacity(0.1);
                            self.layerStops.setOpacity(0.1);
                        }
                    }
                });

            map.addControl(this.controlStopsHover);
            this.controlStopsHover.activate();
            map.addControl(this.controlStopsClick);
            this.controlStopsClick.activate();
        }
    },

    popupClosed: function(popupId) {
        this.layerStops.setOpacity(1.0);
        this.layerRoutes.setOpacity(1.0);
        this.layerRoutesHighlighted.setVisibility(false);
        this.layerStopsHighlighted.setVisibility(false);
        this.highlightedStops.remove(popupId);
    },
 
    CLASS_NAME: "otp.systemmap.Systemmap"
};

otp.systemmap.Systemmap = new otp.Class(otp.systemmap.Systemmap);
