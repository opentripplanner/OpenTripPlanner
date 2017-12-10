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

otp.namespace("otp.modules.alerts");


otp.modules.alerts.AlertsModule = 
    otp.Class(otp.modules.Module, {
    
    moduleName  : "Alerts Manager",

    minimumZoomForStops : 15,
    
    openEditAlertWidgets : { }, // maps the alert id to the widget object
    
    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);
    },

    activate : function() {
        if(this.activated) return;
        var this_ = this;
        
        $.get(otp.config.resourcePath + 'js/otp/modules/alerts/alerts-templates.html')
        .success(function(data) {
            $('<div style="display:none;" />').appendTo($("body")).html(data);
            ich.grabTemplates();
            
            this_.webapp.transitIndex.loadRoutes(this_, function() {
            
                this_.alertsWidget = new otp.modules.alerts.AlertsWidget('otp-'+this_.id+'-alertsWidget', this_);
                this_.fetchAlerts();

                this_.entitiesWidget = new otp.modules.alerts.EntitiesWidget('otp-'+this_.id+'-entitiesWidget', this_);
            });
        });
            
        otp.modules.Module.prototype.activate.apply(this);

        this.stopHighlightLayer = new L.LayerGroup();
        this.routeHighlightLayer = new L.LayerGroup();
    
        this.addLayer("Route Highlights", this.routeHighlightLayer);
        this.addLayer("Stop Highlights", this.stopHighlightLayer);

    },
    
    mapBoundsChanged : function(event) {
        if(this.webapp.map.lmap.getZoom() >= this.minimumZoomForStops) {
            this.webapp.transitIndex.loadStopsInRectangle(null, this.webapp.map.lmap.getBounds(), this, function(data) {
                this.entitiesWidget.updateStops(data.stops);
            });
        }
        else {
            var diff = this.minimumZoomForStops - this.webapp.map.lmap.getZoom();
            this.entitiesWidget.stopsText("<i>Please zoom an additional " + diff + " zoom level" + (diff > 1 ? "s" : "") + " to see stops.</i>");
        }

    },
    
    alertWidgetCount : 0,
    
    newAlertWidget : function(affectedRoutes, affectedStops) {
        var alertObj = new otp.modules.alerts.Alert({
            timeRanges : [],
            informedEntities : []
        });
        
        if(affectedRoutes) {
            for(var i = 0; i < affectedRoutes.length; i++) {
                console.log(affectedRoutes[i]);
                var entity = {
                    agencyId: affectedRoutes[i].routeData.id.agencyId,
                    routeId: affectedRoutes[i].routeData.id.id
                };
                alertObj.attributes.informedEntities.push(entity);
            }
        }
        
        if(affectedStops) {
            for(var i = 0; i < affectedStops.length; i++) {
                var entity = {
                    agencyId: affectedStops[i].id.agencyId,
                    stopId: affectedStops[i].id.id
                };
                alertObj.attributes.informedEntities.push(entity);
            }
        }
                        
        var widget = new otp.modules.alerts.EditAlertWidget('otp-'+this.id+'-editAlertWidget-'+this.alertWidgetCount, this, alertObj);
        widget.bringToFront();
        this.alertWidgetCount++;
    },
    
    editAlertWidget : function(alertObj) {
        if(_.has(this.openEditAlertWidgets, alertObj.get('id'))) {
            var widget = this.openEditAlertWidgets[alertObj.get('id')];
            if(widget.minimized) widget.unminimize();
        }
        else {
            var widget = new otp.modules.alerts.EditAlertWidget('otp-'+this.id+'-editAlertWidget-'+this.alertWidgetCount, this, alertObj);
            this.openEditAlertWidgets[alertObj.get('id')] = widget;
            this.alertWidgetCount++;
        }
        widget.bringToFront();
    },    
    
    fetchAlerts : function() {
        var this_ = this;
        // fetch data from server
        this.alerts = new otp.modules.alerts.Alerts();
        this.alerts.fetch({success: function(collection, response, options) {
            this_.alertsWidget.refreshAlerts(this_.alerts);
        }});
    },
            
    saveAlert : function(alertObj) {
        var this_ = this;
        alertObj.save({}, {
            success : function() {
                console.log("saved!");
                this_.fetchAlerts();
            }
        });
    },
    
    deleteAlert : function(alertObj) {
        var this_ = this;
        alertObj.destroy({
            dataType: "text", // success is not triggered unless we do this
            success : function() {
                console.log("deleted!");
                this_.fetchAlerts();
            },
            error: function(model, response) {
                console.log("Error deleting");
                console.log(response);
            }            
        });
    },

    highlightRoute : function(agencyAndId) {
        this.webapp.transitIndex.loadVariants(agencyAndId, this, function(variants) {
            for(variantName in variants) {
                var polyline = new L.Polyline(otp.util.Geo.decodePolyline(variants[variantName].geometry.points));
                polyline.setStyle({ color : "blue", weight: 6, opacity: 0.4 });
                this.routeHighlightLayer.addLayer(polyline);            
            }
        });
    },
    
    highlightStop : function(stopObj) {
        L.marker([stopObj.stopLat, stopObj.stopLon]).addTo(this.stopHighlightLayer);
    },

    
    clearHighlights : function() {
        this.stopHighlightLayer.clearLayers(); 
        this.routeHighlightLayer.clearLayers(); 
    },
    
    prepareAlertTemplateContext : function(alertObj) {

        var context = _.clone(alertObj.attributes);

        var informedEntitiesCopy = [];
        for(var i=0; i < context.informedEntities.length; i++) {
            var informedEntityCopy = _.clone(context.informedEntities[i]);
            if(informedEntityCopy.routeId) {
                var agencyAndId = informedEntityCopy.agencyId + '_' + informedEntityCopy.routeId; 
                var routeData = this.webapp.transitIndex.routes[agencyAndId].routeData;
                informedEntityCopy.routeReference = otp.util.Itin.getRouteShortReference(routeData);
            }
            informedEntitiesCopy.push(informedEntityCopy);
        }
        context.informedEntities = informedEntitiesCopy;
        
        return context;
    }
});
