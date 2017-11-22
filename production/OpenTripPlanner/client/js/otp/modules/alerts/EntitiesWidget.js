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

otp.modules.alerts.EntitiesWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    routesLookup : null,
    stopsLookup : null,

    initialize : function(id, module) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Transit Entities',
            cssClass : 'otp-alerts-entitiesWidget'
        });
        
        this.module = module;
            
        var tabPanel = $('<div />').addClass('notDraggable').appendTo(this.mainDiv);
        
        $('<ul />').appendTo(tabPanel)
        .append('<li><a href="#'+id+'-routesTab">Routes</a></li>')
        .append('<li><a href="#'+id+'-stopsTab">Stops</a></li>');
        
        this.routesDiv = $('<div id="'+id+'-routesTab" />').addClass('otp-alerts-entitiesWidget-tabPanel').appendTo(tabPanel)
        //this.routesSelect = $('<select multiple />').addClass('otp-alerts-entitiesWidget-select').appendTo(this.routesDiv);

        this.stopsDiv = $('<div id="'+id+'-stopsTab" />').addClass('otp-alerts-entitiesWidget-tabPanel').appendTo(tabPanel);
        //this.stopsSelect = $('<select multiple />').addClass('otp-alerts-entitiesWidget-select').appendTo(this.stopsDiv);
        
        tabPanel.tabs();
        
        /*var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $('<button>Create Alert</button>').button().appendTo(buttonRow).click(function() {
            this_.createAlert();
        });*/
        
        this.refreshRoutes();
        
    },
    
    refreshRoutes : function() {
        var this_ = this;
        var ti = this.module.webapp.transitIndex
        this.routesLookup = [];
        ti.loadRoutes(this, function() {
            this_.routesDiv.empty();
            var i = 0;
            for(var routeId in ti.routes) {
                var route = ti.routes[routeId];
                //this_.routesSelect.append('<option value="'+i+'">'+otp.util.Itin.getRouteDisplayString(route.routeData)+'</option>');
                ich['otp-alerts-routeRow'](route.routeData).appendTo(this_.routesDiv)
                .data('routeId', route.routeData.id)
                .draggable({
                    helper: 'clone',
                    revert: 'invalid',
                    drag: function(event,ui){ 
                        this_.bringToFront();
                        $(ui.helper).css("border", '2px solid gray');
                    }                    
                })
                .hover(function(evt) {
                    var routeId = $(this).data('routeId');
                    this_.module.highlightRoute(routeId.agencyId+"_"+routeId.id);
                }, function(evt) {
                    this_.module.clearHighlights();
                });
                this.routesLookup[i] = route;
                i++;
            }
        });

        /*ti.loadRoutes(this, function() {
            this_.routesSelect.empty();
            var i = 0;
            for(var routeId in ti.routes) {
                var route = ti.routes[routeId];
                this_.routesSelect.append('<option value="'+i+'">'+otp.util.Itin.getRouteDisplayString(route.routeData)+'</option>');
                this.routesLookup[i] = route;
                i++;
            }
        });*/
    },

    updateStops : function(stopArray) {
        var this_ = this;
        /*if(!jQuery.contains(this.stopsDiv, this.stopsSelect)) {
            this.stopsDiv.empty();
            this.stopsDiv.append(this.stopsSelect);    
        }*/
        //this.stopsSelect.empty();
        this.stopsDiv.empty();
        for(var i = 0; i < stopArray.length; i++) {
            var stop = stopArray[i];
            ich['otp-alerts-stopRow'](stop).appendTo(this_.stopsDiv)
            .data('stop', stop)
            .data('stopId', stop.id)
            .draggable({
                helper: 'clone',
                revert: 'invalid',
                drag: function(event,ui){ 
                    this_.bringToFront();
                    $(ui.helper).css("border", '2px solid gray');
                }                    
            })
            .hover(function(evt) {
                this_.module.highlightStop($(this).data('stop'));
            }, function(evt) {
                this_.module.clearHighlights();
            });

            //this_.stopsSelect.append('<option value="'+i+'">('+stop.id.agencyId+'_'+stop.id.id+') '+stop.stopName+'</option>');                       
        }
        this.stopsLookup = stopArray;
    },
    
    stopsText : function(text) {
        this.stopsDiv.empty().html(text);
    },
    
    /*createAlert : function() {

        var routes = [];
        var routeIndices = this.routesSelect.val();
        if(routeIndices != null) {
            for(var i = 0; i < routeIndices.length; i++) {
                var index = parseInt(routeIndices[i]);
                routes.push(this.routesLookup[index]);
            }
        }
                
        var stops = [];
        var stopIndices = this.stopsSelect.val();
        if(stopIndices != null) {
            for(var i = 0; i < stopIndices.length; i++) {
                var index = parseInt(stopIndices[i]);
                stops.push(this.stopsLookup[index]);
            }
        }
                
        this.module.newAlertWidget(routes, stops);
    },*/
});
