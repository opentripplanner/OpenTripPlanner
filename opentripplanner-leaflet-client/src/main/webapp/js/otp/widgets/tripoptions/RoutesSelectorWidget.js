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

otp.namespace("otp.widgets");

otp.widgets.RoutesSelectorWidget = 
    otp.Class(otp.widgets.Widget, {

    routesControl : null,
    
    routeData : [],
    selectedRouteIndices : [],
    selectedRouteIds : null, // agencyAndId format
    
    restoredRouteIds : null, // agencyAndId format
    
    initializedRoutes : false,
    
    initialize : function(id, routesControl, name) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, routesControl.tripWidget.owner, {
            openInitially : false,
            title : name + ' Selector'
        });

        this.routesControl = routesControl;
        this.ti = this.routesControl.tripWidget.module.webapp.transitIndex;

        this.selectedRouteIds = [];
        
        var mainRowHtml = '<div>';

        mainRowHtml += '<div style="float:left">All Routes:<br><select id="'+this.id+'-routeList" size=6 class="otp-preferredRoutesSelectorWidget-list notDraggable"></select></div>';
        mainRowHtml += '<div style="float:right">' + name + ':<br><select id="'+this.id+'-selectedList" size=6 class="otp-preferredRoutesSelectorWidget-list notDraggable"></select></div>';

        mainRowHtml += '<div class="otp-preferredRoutesSelectorWidget-middle">';
        mainRowHtml += '<button id="'+this.id+'-addButton" style="margin-bottom: 5px;">&gt;</button><br>';
        mainRowHtml += '<button id="'+this.id+'-removeButton">&lt;</button><br>';
        mainRowHtml += '</div>';
        
        mainRowHtml += '<div style="clear:both;" /></div>'

        $(mainRowHtml).appendTo(this.$());
        
        $('#'+this.id+'-addButton').button().click(function() {
            this_.selectRoute($('#'+this_.id+'-routeList').val());
        });
        $('#'+this.id+'-removeButton').button().click(function() {
            var agencyAndId = $('#'+this_.id+'-selectedList').val();
            $('#'+this_.id+'-selectedList option[value="'+agencyAndId+'"]').remove();
            this_.selectedRouteIds.splice( $.inArray(agencyAndId, this_.selectedRouteIds), 1 );
        });

        
        var buttonRowHtml = '<div class="otp-preferredRoutesSelectorWidget-buttonRow">';
        buttonRowHtml += '<button id="'+this.id+'-saveButton">Save</button>&nbsp;';
        buttonRowHtml += '<button id="'+this.id+'-closeButton">Close</button>';
        buttonRowHtml += '</div>'
        
        $(buttonRowHtml).appendTo(this.$());

        $('#'+this.id+'-saveButton').button().click(function() {
            var paramStr = '', displayStr = '';
            //for(var i = 0; i < this_.selectedRouteIndices.length; i++) {
            for(var i = 0; i < this_.selectedRouteIds.length; i++) {
                var route = this_.ti.routes[this_.selectedRouteIds[i]].routeData;

                paramStr += route.id.agencyId+"__"+route.id.id + (i < this_.selectedRouteIds.length-1 ? ',' : '');
                displayStr += (route.routeShortName || route.routeLongName) + (i < this_.selectedRouteIds.length-1 ? ', ' : '');

            }
            this_.hide();
            
            this_.routesControl.setRoutes(paramStr, displayStr);
        });
        $('#'+this.id+'-closeButton').button().click(function() {
            this_.close();
        });
        
        this.center();
    },
    
    selectRoute : function(agencyAndId) {
        if(_.contains(this.selectedRouteIds, agencyAndId)) return;
        $('#'+this.id+'-selectedList').append('<option value="'+agencyAndId+'">'+otp.util.Itin.getRouteDisplayString(this.ti.routes[agencyAndId].routeData)+'</option>');                
        this.selectedRouteIds.push(agencyAndId);
    },
    
    updateRouteList : function() {
        if(this.initializedRoutes) return;
        var routesList = $('#'+this.id+'-routeList'), selectedList = $('#'+this.id+'-selectedList');
        var this_ = this;
        
        routesList.empty();
        selectedList.empty();

        this.ti.loadRoutes(this, function() {
            console.log("routes:");
            console.log(this.ti.routes);
            //var restoredRouteIdArr = (this_.restoredRouteIds != null) ? this_.restoredRouteIds.split(',') : []; 
            
            var i = 0;
            //console.log('restoredRouteIdArr');
            //console.log(restoredRouteIdArr);
            for(agencyAndId in this.ti.routes) {
                var route = this.ti.routes[agencyAndId].routeData;
                routesList.append('<option value="'+agencyAndId+'">'+otp.util.Itin.getRouteDisplayString(route)+'</option>');
                //var combinedId = route.id.agencyId+"_"+route.id.id;
                if(_.contains(this_.restoredRouteIds, agencyAndId)) {
                    this_.selectRoute(agencyAndId);
                }
                i++;
            }
            this_.initializedRoutes = true;
        });

    },
    

    
});
