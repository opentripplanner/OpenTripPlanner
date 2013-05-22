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

    initialize : function(webapp) {
        otp.modules.Module.prototype.initialize.apply(this, arguments);
    },

    activate : function() {
        if(this.activated) return;
        var this_ = this;
        
        otp.modules.Module.prototype.activate.apply(this);

        this.entitiesWidget = new otp.modules.alerts.EntitiesWidget('otp-'+this.moduleId+'-entitiesWidget', this);
        
    },
    
    mapBoundsChanged : function(event) {
        if(this.webapp.map.lmap.getZoom() >= 16) {
            this.webapp.transitIndex.loadStopsInRectangle("TriMet", this.webapp.map.lmap.getBounds(), this, function(data) {
                this.entitiesWidget.updateStops(data.stops);
            });
        }

    },
    


});
