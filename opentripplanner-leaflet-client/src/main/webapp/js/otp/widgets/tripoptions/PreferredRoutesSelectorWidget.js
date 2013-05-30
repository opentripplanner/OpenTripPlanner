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

otp.widgets.PreferredRoutesSelectorWidget = 
    otp.Class(otp.widgets.Widget, {

    preferredRoutesControl : null,
    
    routeData : [],
    selectedRouteIndices : [],
    
    restoredRouteIds : null,
    
    initializedRoutes : false,
    
    initialize : function(id, preferredRoutesControl) {
        var this_ = this;
        otp.widgets.Widget.prototype.initialize.call(this, id, preferredRoutesControl.tripWidget.owner, {
            openInitially : false,
            title : 'Preferred Routes Selector'
        });

        this.preferredRoutesControl = preferredRoutesControl;
        
        var mainRowHtml = '<div>';

        mainRowHtml += '<div style="float:left">All Routes:<br><select id="'+this.id+'-routeList" size=6 class="otp-preferredRoutesSelectorWidget-list notDraggable"></select></div>';
        mainRowHtml += '<div style="float:right">Preferred Routes:<br><select id="'+this.id+'-selectedList" size=6 class="otp-preferredRoutesSelectorWidget-list notDraggable"></select></div>';

        mainRowHtml += '<div class="otp-preferredRoutesSelectorWidget-middle">';
        mainRowHtml += '<button id="'+this.id+'-addButton" style="margin-bottom: 5px;">&gt;</button><br>';
        mainRowHtml += '<button id="'+this.id+'-removeButton">&lt;</button><br>';
        mainRowHtml += '</div>';
        
        mainRowHtml += '<div style="clear:both;" /></div>'

        $(mainRowHtml).appendTo(this.$());
        
        $('#'+this.id+'-addButton').button().click(function() {
            var index = document.getElementById(this_.id+'-routeList').selectedIndex;
            if(index >= 0 && this_.routeData && !_.contains(this_.selectedRouteIndices, index)) {
                this_.selectRoute(index);
            }
        });
        $('#'+this.id+'-removeButton').button().click(function() {
            var selList = document.getElementById(this_.id+'-selectedList');
            var index = selList.selectedIndex;
            if(index >= 0) {
                this_.selectedRouteIndices.splice(index, 1);
                selList.remove(index);
            }
        });

        
        var buttonRowHtml = '<div class="otp-preferredRoutesSelectorWidget-buttonRow">';
        buttonRowHtml += '<button id="'+this.id+'-saveButton">Save</button>&nbsp;';
        buttonRowHtml += '<button id="'+this.id+'-closeButton">Close</button>';
        buttonRowHtml += '</div>'
        
        $(buttonRowHtml).appendTo(this.$());

        $('#'+this.id+'-saveButton').button().click(function() {
            var paramStr = '', displayStr = '';
            for(var i = 0; i < this_.selectedRouteIndices.length; i++) {
                var route = this_.routeData[this_.selectedRouteIndices[i]];

                paramStr += route.id.agencyId+"__"+route.id.id + (i < this_.selectedRouteIndices.length-1 ? ',' : '');
                displayStr += (route.routeShortName || route.routeLongName) + (i < this_.selectedRouteIndices.length-1 ? ', ' : '');

            }
            this_.hide();
            
            this_.preferredRoutesControl.setRoutes(paramStr, displayStr);
        });
        $('#'+this.id+'-closeButton').button().click(function() {
            this_.close();
        });
        
        this.center();
    },
    
    selectRoute : function(index) {
        $('#'+this.id+'-selectedList').append('<option>'+otp.util.Itin.getRouteDisplayString(this.routeData[index])+'</option>');                
        this.selectedRouteIndices.push(index);
    },
    
    updateRouteList : function() {
        if(this.initializedRoutes) return;
        var routesList = $('#'+this.id+'-routeList'), selectedList = $('#'+this.id+'-selectedList');
        var this_ = this;
        
        var url = otp.config.hostname + '/opentripplanner-api-webapp/ws/transit/routes';
        this.currentRequest = $.ajax(url, {
            dataType:   'jsonp',
                
            success: function(data) {
                this_.routeData = data.routes;
                this_.routeData.sort(function(a,b) {
                    a = a.routeShortName || a.routeLongName;
                    b = b.routeShortName || b.routeLongName;
                    if(otp.util.Text.isNumber(a) && otp.util.Text.isNumber(a)) {
                        if(parseFloat(a) < parseFloat(b)) return -1;
                        if(parseFloat(a) > parseFloat(b)) return 1;
                        return 0;
                    }
                    if(a < b) return -1;
                    if(a > b) return 1;
                    return 0;
                });
                
                var restoredRouteIdArr = (this_.restoredRouteIds != null) ? this_.restoredRouteIds.split(',') : []; 
                
                for(var i = 0; i < data.routes.length; i++) {
                    var route = data.routes[i];
                    routesList.append('<option>'+otp.util.Itin.getRouteDisplayString(route)+'</option>');
                    var combinedId = route.id.agencyId+"_"+route.id.id;
                    if(_.contains(restoredRouteIdArr, combinedId)) {
                        this_.selectRoute(i);
                    }
                }
                this_.initializedRoutes = true;
            }
        });
    },
    

    
});
