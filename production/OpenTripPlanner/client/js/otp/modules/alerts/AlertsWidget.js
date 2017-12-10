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

otp.modules.alerts.AlertsWidget = 
    otp.Class(otp.widgets.Widget, {
    
    module : null,
    
    routesLookup : null,
    stopsLookup : null,
    
    affectedRoutes : [],
    affectedStops : [],

    initialize : function(id, module, routes, stops) {
        var this_ = this;
        this.module = module;
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Alerts',
            cssClass : 'otp-alerts-alertsWidget',
            closeable: true
        });
        
        this.alertsList = $(Mustache.render(otp.templates.div, {
            id : this.id+'-alertsList',
            cssClass : 'otp-alerts-alertsWidget-alertsList notDraggable'
        })).appendTo(this.mainDiv);
        
        // set up the 'new alert' button
        var buttonRow = $('<div>').addClass('otp-alerts-entitiesWidget-buttonRow').appendTo(this.mainDiv)
        
        $(Mustache.render(otp.templates.button, { text : "Create New Alert"}))
        .button().appendTo(buttonRow).click(function() {
            this_.module.newAlertWidget();
        });        
    },
    
    refreshAlerts : function(alerts) {
        var this_ = this;
        this.alertsList.empty();
        for(var i = 0; i < alerts.length; i++) {
            /*$(Mustache.render(otp.modules.alerts.alertRow, alerts.models[i].attributes))
            .appendTo(this.alertsList)
            .data('alertObj', alerts.at(i))
            .click(function() {
                var alertObj = $(this).data('alertObj');
                this_.module.editAlertWidget(alertObj);
            });*/
            
            //var context = _.clone(alerts.models[i].attributes);
            var context = this.module.prepareAlertTemplateContext(alerts.models[i]);

            var routeIdArr = [], stopIdArr = [];
            for(var e = 0; e < context.informedEntities.length; e++) {
                var entity = context.informedEntities[e];
                if(entity.routeId) routeIdArr.push(entity.routeReference);
                if(entity.stopId) stopIdArr.push(entity.stopId);
            }
            context['routeIds'] = routeIdArr.join(', ');
            context['stopIds'] = stopIdArr.join(', ');
            
            ich['otp-alerts-alertRow'](context).appendTo(this.alertsList)
            .data('alertObj', alerts.at(i))
            .click(function() {
                var alertObj = $(this).data('alertObj');
                this_.module.editAlertWidget(alertObj);
            });
        }
        
    },
});
