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
            title : name
        });

        this.routesControl = routesControl;
        this.indexApi = this.routesControl.tripWidget.module.webapp.indexApi;

        this.selectedRouteIds = [];

        ich['otp-tripOptions-routesSelector']({
            widgetId : this.id,
            name : this.name,
            //TRANSLATORS: All public transport routes. Shown in
            //Preffered/Banned routes widget
            allRoutes : _tr("All Routes"),
            //TRANSLATORS: save preffered/banned public transport routes
            save : _tr("Save"),
            //TRANSLATORS: Close preffered/banned public transport routes
            //widget
            close : _tr("Close")
        }).appendTo(this.$());

        this.selectedList = $('#'+this_.id+'-selectedList');
        this.routeList = $('#'+this_.id+'-routeList');

        $('#'+this.id+'-addButton').button().click(function() {
            this_.selectRoute(this_.routeList.val());
        });

        $('#'+this.id+'-removeButton').button().click(function() {
            var agencyAndId = this_.selectedList.val();
            $('#'+this_.id+'-selectedList option[value="'+agencyAndId+'"]').remove();
            this_.selectedRouteIds.splice( $.inArray(agencyAndId, this_.selectedRouteIds), 1 );
        });

        $('#'+this.id+'-saveButton').button().click(function() {
            var paramStr = '', displayStr = '';
            for(var i = 0; i < this_.selectedRouteIds.length; i++) {
                var route = this_.indexApi.routes[this_.selectedRouteIds[i]].routeData;
                //format expected: agency_routename or agency__routeid, so, in our case, two underscores 
                paramStr += route.id.replace(":", "__") + (i < this_.selectedRouteIds.length-1 ? ',' : '');
                displayStr += (route.shortName || route.longName) + (i < this_.selectedRouteIds.length-1 ? ', ' : '');
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
        if(!agencyAndId || _.contains(this.selectedRouteIds, agencyAndId)) return;
        this.selectedList.append('<option value="'+agencyAndId+'">'+otp.util.Itin.getRouteDisplayString(this.indexApi.routes[agencyAndId].routeData)+'</option>');
        this.selectedRouteIds.push(agencyAndId);
    },

    updateRouteList : function() {
        if(this.initializedRoutes) return;
        var this_ = this;

        this.routeList.empty();

        this.indexApi.loadRoutes(this, function() {
            this_.restoreSelected();
            this_.initializedRoutes = true;
        });

    },

    restoreSelected : function() {
        this.clearSelected();
        var i = 0;
        for(agencyAndId in this.indexApi.routes) {
            var route = this.indexApi.routes[agencyAndId].routeData;
            this.routeList.append('<option value="'+agencyAndId+'">'+otp.util.Itin.getRouteDisplayString(route)+'</option>');
            if(_.contains(this.restoredRouteIds, agencyAndId)) {
                this.selectRoute(agencyAndId);
            }
            i++;
        }
    },

    clearSelected : function() {
        this.selectedList.empty();
        this.selectedRouteIds = [];
    }

});
