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

    initialize : function(id, module) {
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
        this.routesSelect = $('<select size=10 />').addClass('otp-alerts-entitiesWidget-select').appendTo(this.routesDiv);

        this.stopsDiv = $('<div id="'+id+'-stopsTab" />').addClass('otp-alerts-entitiesWidget-tabPanel').appendTo(tabPanel);
        this.stopsSelect = $('<select size=10 />').addClass('otp-alerts-entitiesWidget-select').appendTo(this.stopsDiv);
        
        tabPanel.tabs();
        
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $('<button>Create Alert</button>').button().appendTo(buttonRow).click(function() {
            console.log("create alert");
        });
        
        this.refreshRoutes();
        
    },
    
    refreshRoutes : function() {
        var this_ = this;
        var ti = this.module.webapp.transitIndex
        ti.loadRoutes(this, function() {
            this_.routesSelect.empty();
            for(var routeId in ti.routes) {
                var route = ti.routes[routeId];
                this_.routesSelect.append('<option>'+otp.util.Itin.getRouteDisplayString(route.routeData)+'</option>');
            }
        });
    },

    updateStops : function(stopArray) {
        var this_ = this;
        this.stopsSelect.empty();
        for(var i = 0; i < stopArray.length; i++) {
            var stop = stopArray[i];
            this_.stopsSelect.append('<option>('+stop.id.agencyId+'_'+stop.id.id+') '+stop.stopName+'</option>');                       
        }
    }, 
});
