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

otp.modules.alerts.EditAlertWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    routesLookup : null,
    stopsLookup : null,
    
    affectedRoutes : [],
    affectedStops : [],

    initialize : function(id, module, routes, stops) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Edit Alert',
            cssClass : 'otp-alerts-editAlertWidget',
            closeable: true
        });
        
        this.module = module;
        this.affectedRoutes = routes;
        this.affectedStops = stops;

        $('<div>Alert Text</div>').appendTo(this.mainDiv);
        $('<textarea />').addClass('otp-alerts-editAlert-text notDraggable').appendTo(this.mainDiv);

        $('<div>Affected Entities:</div>').appendTo(this.mainDiv);
        this.affectedEntitiesSelect = $('<select size=5 />').appendTo(this.mainDiv);
        
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
                        
        $('<button>Save</button>').button().appendTo(buttonRow).click(function() {
            //this.close();
        });
        
        this.refreshAffectedEntities();
    },
    
    refreshAffectedEntities : function() {
        this.affectedEntitiesSelect.empty();
        
        for(var i = 0; i < this.affectedRoutes.length; i++) {
            console.log(this.affectedRoutes[i]);
            $('<option>Route '+this.affectedRoutes[i].routeData.routeShortName+'</option>').appendTo(this.affectedEntitiesSelect);
        }

        for(var i = 0; i < this.affectedStops.length; i++) {
            console.log(this.affectedStops[i]);
            $('<option>Stop '+this.affectedStops[i].id.id+'</option>').appendTo(this.affectedEntitiesSelect);
        }
    },
    
});
