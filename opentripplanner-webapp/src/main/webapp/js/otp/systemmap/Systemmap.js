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

    // pointer to the map / components
    map           : null,
    systemMapLayerRoutes : null,
    systemMapLayerStops : null,
    systemPanel: null,
    xmlLoaded: false,
    routeStore: null,
    stopStore: null,
    url: null,
    popupUrl: null,
    data: null,
    routeColorMap: null,
    departureStore: null,

    initialize : function(config)
    {
        this.systemmap = this;
        otp.configure(this, config);
        var self = this;
        
        // XXX color hard coded here
        this.routeColorMap = {
                'A': '#006699',
                'C': '#006699',
                'E': '#006699',
                'B': '#ff6600',
                'D': '#ff6600',
                'F': '#ff6600',
                'V': '#ff6600',
                'G': '#66cc00',
                'J': '#996600',
                'M': '#996600',
                'Z': '#996600',
                'L': '#999999',
                'N': '#ffcc00',
                'R': '#ffcc00',
                'Q': '#ffcc00',
                'W': '#ffcc00',
                'S': '#999999',
                '1': '#ff0033',
                '2': '#ff0033',
                '3': '#ff0033',
                '9': '#ff0033',
                '4': '#009933',
                '5': '#009933',
                '6': '#009933',
                '7': 'cc00cc'
        };

        var routeRecord = Ext.data.Record.create([
            'id',
            {name: 'geometry',
             mapping: 'geometry/points',
             convert: function(val, rec)
                      {
                          return otp.util.OpenLayersUtils.encoded_polyline_converter(val, rec);
                      }
            },
            {name: 'line', mapping: 'shortName'},
            {name: 'desc', mapping: 'longName'},
            {name: 'stopIds', convert: function(val, rec)
                                       {
                                           var stopIds = [];
                                           var idNodes = Ext.DomQuery.select('stop > id', rec);
                                           for (var i = 0; i < idNodes.length; i++)
                                           {
                                               var node = idNodes[i];
                                               var id = node.firstChild.nodeValue;
                                               stopIds.push(id);
                                           }
                                           return stopIds;
                                       }
            },
            'agencyId',
            'mode'
        ]);
        
        var stopRecord = Ext.data.Record.create([
            'id',
            'lat',
            'lon',
            'name',
            {name: 'route', convert: function(n, p)
                                     {
                                         var route = p.parentNode.parentNode;
                                         for (var i = 0; i < route.childNodes.length; i++)
                                         {
                                             var child = route.childNodes[i];
                                             if (child.nodeName == 'shortName') {
                                                 return child.firstChild.nodeValue;
                                             }
                                         }
                                         return "";
                                     }}
        ]);

        var routeXmlReader = new Ext.data.XmlReader({idPath: 'id', record: 'route'}, routeRecord);
        var stopXmlReader = new Ext.data.XmlReader({idPath: 'id', record: 'stop'}, stopRecord);
        
        this.routeStore = new Ext.data.Store({
            storeId: 'routeStore',
            sortInfo: {field: 'line', direction: 'ASC'},
            reader: routeXmlReader
        });
        
        this.stopStore = new Ext.data.Store({
            storeId: 'stopStore',
            reader: stopXmlReader
        });

        var departureRecord = new Ext.data.Record.create([
            'routeId',
            'headsign',
            'date',
            {name: 'dateFormatted', mapping: 'date',
             convert: function(val, rec)
                      {
                          var dateFormatted = otp.util.DateUtils.prettyTime(val);
                          return dateFormatted;
                      }
            }
        ]);
        var departureXmlReader = new Ext.data.XmlReader({record: 'departure'}, departureRecord);
        // departure stores are used to parse the response from the server for the popup
        this.departureStore = new Ext.data.Store({
            storeId: 'departureStore',
            reader: departureXmlReader
        });

        this.systemPanel = new Ext.grid.GridPanel({      
            id: 'system-panel',
            store: this.routeStore,
            columns: [
                {id:'line', header: 'Line', sortable: true, dataIndex: 'line'},
                {id:'desc', header: 'Name', sortable: true, dataIndex: 'desc'}
            ],
            stripeRows: true,
            autoExpandColumn: 'desc',
            stateful: false,
            flex: 1
        });
        
        var detailPanel = new Ext.Panel({
            html: '<p>Select an item for details</p><p class="disclaimer">Subway route symbols &trade; Metropolitan Transportation Authority. Used with permission.</p>',
            id: 'systemmap-detailpanel',
            height: 150
        });
        
        var detailTplMarkup = [
            '<img src="{routeImage}" /><p>{desc}</p><p class="disclaimer">Subway route symbols &trade; Metropolitan Transportation Authority. Used with permission.</p>'
        ];
        var detailTpl = new Ext.Template(detailTplMarkup);

        var self = this;
        this.systemPanel.getSelectionModel().on('rowselect', function(sm, rowIdx, r) {
            var route = r.get('line');
            var imagePathOptions = Ext.apply({}, r.data, {imageType: 'big', route: route});
            var routeImage = otp.util.imagePathManager.imagePath(imagePathOptions);
            detailTpl.overwrite(detailPanel.body, Ext.apply({}, r.data, {routeImage: routeImage}));
            
            if (!self.xmlLoaded)
            {
                return;
            }
            
            var featureId = r.id;
            // find the particular feature, and ask the select control to select it
            for (var i = 0; i < self.systemMapLayerRoutes.features.length; i++)
            {
                var feature = self.systemMapLayerRoutes.features[i];
                if (featureId === feature.attributes.id)
                {
                    // for some reason, we can get multiple routes in the selected list
                    // so we just unselect all of them first
                    self.selectControlRoutes.unselectAll();
                    self.selectControlRoutes.select(feature);
                    // maybe we should be smarter here and not zoom unless the route is fully on screen
                    self.map.getMap().zoomToExtent(feature.geometry.getBounds());
                    break;
                }
            }
        });

        this.m_mainPanel = new Ext.Panel({
            id:         'sm-accordion',
            title:      this.locale.systemmap.labels.panelTitle,
            iconCls:    'systemmap-panel',
            autoShow:   true,
            border:     false,
            layout:     'vBox',
            layoutConfig: {
                align: 'stretch',
                pack: 'start'
            },
            items:      [this.systemPanel, detailPanel],
            listeners:  {'expand': {fn: this.panelExpanded, scope: this}}
        });
        
        this.fetchData();                
    },

    getPanel : function()
    {
        return this.m_mainPanel;
    },
    
    panelExpanded : function()
    {
        this.map.removeAllFeatures();
        if (!this.xmlLoaded)
        {
            return;
        }
        this.loadRoutes();
    },
    
    fetchData : function(url)
    {
        if (url == null)
        {
            url = this.url;
        }
        var self = this;
        OpenLayers.Request.GET({
            url : url,
            headers: {Accept: 'application/xml'},
            success : function(xhr) 
                      {
                          self.onDataLoaded(xhr.responseXML);
                      },
            failure : function(xhr)
                      {
                          console.log("SystemMap fetchData error:");
                          console.log(xhr);
                      }
        });
    },

    loadRoutes : function()
    {
        if (!this.xmlLoaded)
        {
            return;
        }
        var map = this.map.getMap();
        var self = this;
        if (this.systemMapLayerRoutes == null)
        {
            var routeStyleDefault = {
                strokeColor: '${color}',
                strokeOpacity: 1,
                fillOpacity: 1,
                strokeWidth: 3,
                strokeLinecap: 'round'
            };
            var routeStyleSelected = {
                strokeOpacity: 1,
                strokeWidth: 6,
                label: "${line}",
                labelAlign: 'lm',
                labelXOffset: 8,
                labelYOffset: 8
            };
            var routeStyleOff = {
                strokeOpacity: 0.2,
                strokeWidth: 3
            };
            
            var stopStyleDefault = {
                strokeColor: '#ffffff',
                fillColor: '#000000',
                strokeOpacity: 1,
                fillOpacity: 1,
                pointRadius: 3
            };
            var stopStyleRouteSelected = {
                label: '${name}',
                labelAlign: 'rm',
                labelXOffset: -8,
                labelYOffset: 8,
                fontSize: '9px',
                fontFamily: 'Arial',
                fillOpacity: 1,
                pointRadius: 3
            };
            var stopStyleSelected = {
                    label: '${name}',
                    labelAlign: 'rm',
                    labelXOffset: -8,
                    labelYOffset: 8,
                    fontSize: '9px',
                    fontFamily: 'Arial',
                    fillOpacity: 0.4,
                    pointRadius: 4
            };
            var stopStyleOff = {
                fillOpacity: 0.2,
                pointRadius: 4
            };

            this.systemMapLayerRoutes = new OpenLayers.Layer.Vector('systemmap-vector-layer-routes',
                                                                    {styleMap: new OpenLayers.StyleMap({'default': routeStyleDefault, 'select': routeStyleSelected, 'off': routeStyleOff})});
            this.systemMapLayerStops = new OpenLayers.Layer.Vector('systemmap-vector-layer-stops',
                                                                    {styleMap: new OpenLayers.StyleMap({'default': stopStyleDefault, 'select': stopStyleSelected, 'routeselect': stopStyleRouteSelected, 'off': stopStyleOff})});
            map.addLayer(this.systemMapLayerRoutes);
            map.addLayer(this.systemMapLayerStops);

            var popupTplMarkup = '<h2>Departures</h2><ul>{departures}</ul>';
            var popupTpl = new Ext.Template(popupTplMarkup);
            var popupDepartureMarkup = '<li class="departure"><img src="{routeImage}" class="popup-route-icon" /> <strong>{headsign}</strong> - {dateFormatted}</li>';
            var popupDepartureTpl = new Ext.Template(popupDepartureMarkup);

            var selectControlStops = new OpenLayers.Control.SelectFeature(this.systemMapLayerStops, {
                onSelect: function(feature) {
                    var stopRecord = self.stopStore.getById(feature.attributes.id);
                    var lat = stopRecord.get('lat');
                    var lon = stopRecord.get('lon');
                    OpenLayers.Request.GET({
                        url: self.popupUrl,
                        params: {lat: lat, lon: lon, n: 5},
                        headers: {Accept: 'application/xml'},
                        success : function(xhr) 
                                  {
                                      var html = '';
                                      if (xhr.responseXML != null)
                                      {
                                          self.departureStore.loadData(xhr.responseXML);
                                          var departuresMarkup = '';
                                          for (var i = 0; i < self.departureStore.getCount(); i++)
                                          {
                                              var departureRecord = self.departureStore.getAt(i),
                                                  routeId = departureRecord.get('routeId'),
                                                  routeRecord = self.routeStore.getById(routeId);
                                              var imagePathOptions = Ext.apply({}, {route: routeRecord.get('line')}, routeRecord.data);
                                              var routeImage = otp.util.imagePathManager.imagePath(imagePathOptions);
                                              var departureMarkup = popupDepartureTpl.apply(Ext.apply({}, {routeImage: routeImage}, departureRecord.data));
                                              departuresMarkup += departureMarkup;
                                          }
                                          html = popupTpl.apply({departures: departuresMarkup});
                                      }
                                      else
                                      {
                                          html = '<p>No departures found</p>';
                                      }
                                      var popup = new OpenLayers.Popup(
                                              feature.attributes.id,
                                              feature.geometry.getBounds().getCenterLonLat(),
                                              new OpenLayers.Size(200, 200),
                                              html,
                                              true,
                                              function() { selectControlStops.unselect(feature); }
                                      );
                                      feature.popup = popup;
                                      map.addPopup(popup);
                                  },
                        failure : function(xhr)
                                  {
                                      console.log("SystemMap popup fetch data error:");
                                      console.log(xhr);
                                  }
                    });
                },
                onUnselect: function(feature) {
                    map.removePopup(feature.popup);
                    feature.popup.destroy();
                    feature.popup = null;   
                }
            });
            map.addControl(selectControlStops);
            selectControlStops.activate();
            
            var selectControlRoutes = new OpenLayers.Control.SelectFeature(this.systemMapLayerRoutes, {
                onSelect: function(feature) {                
                    for (var i = 0; i < self.systemMapLayerRoutes.features.length; i++)
                    {
                        var f = self.systemMapLayerRoutes.features[i];
                        self.systemMapLayerRoutes.drawFeature(f, 'off');
                    }
                    self.systemMapLayerRoutes.drawFeature(feature, 'select');

                    if (self.systemMapLayerStops.getVisibility())
                    {
                        var routeId = feature.attributes.id;
                        var routeRecord = self.routeStore.getById(routeId);
                        var stopIds = routeRecord.data.stopIds;
                        var stopFeatures = [];
                        var stopIdMap = {};
                        for (var i = 0; i < stopIds.length; i++)
                        {
                            stopIdMap[stopIds[i]] = null;
                        }
                        for (var i = 0; i < self.systemMapLayerStops.features.length; i++)
                        {
                            var stopFeature = self.systemMapLayerStops.features[i];
                            var stopId = stopFeature.attributes.id;
                            if (stopId in stopIdMap)
                            {
                                self.systemMapLayerStops.drawFeature(stopFeature, 'routeselect');
                            }
                            else
                            {
                                self.systemMapLayerStops.drawFeature(stopFeature, 'off');
                            }                            
                        }
                    }
                    // first we remove the feature and add it back to the layer
                    // this is a hack to make sure that it's on top of all other features
                    self.systemMapLayerRoutes.removeFeatures(feature);
                    self.systemMapLayerRoutes.addFeatures(feature);
                    // and this one here is needed to let us unselect it,
                    // probably because the remove call above removes it from the selected list
                    self.systemMapLayerRoutes.selectedFeatures.push(feature);
                },
                onUnselect: function(feature) {
                    for (var i = 0; i < self.systemMapLayerRoutes.features.length; i++)
                    {
                        var f = self.systemMapLayerRoutes.features[i];
                        self.systemMapLayerRoutes.drawFeature(f, 'default');
                    }
                    var routeId = feature.attributes.id;
                    var routeRecord = self.routeStore.getById(routeId);
                    var stopIds = routeRecord.data.stopIds;
                    var stopFeatures = [];
                    for (var i = 0; i < stopIds.length; i++)
                    {
                        var stopId = stopIds[i];
                        for (var j = 0; j < self.systemMapLayerStops.features.length; j++)
                        {
                            var stopFeature = self.systemMapLayerStops.features[j];
                            self.systemMapLayerStops.drawFeature(stopFeature, 'default');
                        }
                    }
                }
            });
            map.addControl(selectControlRoutes);
            selectControlRoutes.activate();
            
            this.selectControlRoutes = selectControlRoutes;
            
            // add a listener when the map removes all features so that we can remove all selected features
            var beforeAllFeaturesRemoved = function() {
                selectControlRoutes.unselectAll();
                selectControlStops.unselectAll();
            };
            self.map.beforeAllFeaturesRemoved.push(beforeAllFeaturesRemoved);

            // having a hover control active with a click control 
//            var hoverControlRoutes = new OpenLayers.Control.SelectFeature(this.systemMapLayerRoutes, {
//                hover: true,
//                overFeature: function(feature) {
//                    self.systemMapLayerRoutes.drawFeature(feature, 'select');
//                },
//                outFeature: function(feature) {
//                    var intent = 'default';
//                    for (var i = 0; i < self.systemMapLayerRoutes.selectedFeatures.length; i++)
//                    {
//                        var f = self.systemMapLayerRoutes.selectedFeatures[i];
//                        if (f === feature)
//                        {
//                            intent = 'select';
//                            break;
//                        }
//                    }
//                    self.systemMapLayerRoutes.drawFeature(feature, intent);
//                }
//            });
//            map.addControl(hoverControlRoutes);
//            hoverControlRoutes.activate();
        }
        // load the map features from the xml
        for (var i = 0; i < this.routeStore.getCount(); i++)
        {
            var route = this.routeStore.getAt(i);
            var geoJson = route.get('geometry');
            var line = route.get('line');
            var color = this.routeColorMap[line];
            if (!color) {
                color = '#ffffff';
            }
            var routeFeature = new OpenLayers.Feature.Vector(geoJson,
                    {type: 'route',
                     id: route.get('id'),
                     line: line,
                     desc: route.get('desc'),
                     color: color
                    }
            );
            this.systemMapLayerRoutes.addFeatures([routeFeature]);
        }
        for (var i = 0; i < this.stopStore.getCount(); i++)
        {
            var stop = this.stopStore.getAt(i);
            var lat = stop.get('lat');
            var lon = stop.get('lon');
            var geometry = new OpenLayers.Geometry.Point(lon, lat);
            var stopFeature = new OpenLayers.Feature.Vector(
                geometry,
                {type: 'stop',
                 id: stop.get('id'),
                 name: stop.get('name'),
                 route: stop.get('route')
                }
            );
            this.systemMapLayerStops.addFeatures([stopFeature]);
        }
        
        this.systemMapLayerRoutes.events.on({
            moveend: function(event) {
                if (!event.zoomChanged) return;
                var zoom = self.map.getMap().getZoom();
                var shouldShowStops = zoom >= 13;
                self.systemMapLayerStops.setVisibility(shouldShowStops);
                if (shouldShowStops && self.systemMapLayerRoutes.selectedFeatures.length > 0) {
                    // we can only select one route at a time
                    var feature = self.systemMapLayerRoutes.selectedFeatures[0];
                    // reselecting it will take care of adjusting stops appropriately
                    selectControlRoutes.select(feature);
                }
            }
        });
        
        // stops should be off by default
        this.systemMapLayerStops.setVisibility(false);

        //map.zoomToExtent(this.systemMapLayerRoutes.getDataExtent());
        map.setCenter(new OpenLayers.LonLat(-73.93639874024, 40.738839047863), 12);
    },
 
    onDataLoaded: function(data)
    {
        this.data = data;
        this.routeStore.loadData(data);
        this.stopStore.loadData(data);
        this.xmlLoaded = true;
        // because we want the system map to be the default panel
        // we just load the routes immediately on the map
        this.loadRoutes();
        // need to check if the current panel that's expanded is the systemmap
        // if so, need to load the map data
    },

    CLASS_NAME: "otp.systemmap.Systemmap"
};

otp.systemmap.Systemmap = new otp.Class(otp.systemmap.Systemmap);
