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

otp.namespace("otp.widgets.transit");

otp.widgets.transit.StopFinderWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    agency_id : null,
    
    timeIndex : null,
    
        
    initialize : function(id, module, stopViewer) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module, {
            title : 'Stop Finder',
            cssClass : 'otp-stopFinder',
            closeable : true,
            resizable : true,
            openInitially : false,
            persistOnClose : true,
        });
        
        this.module = module;
        this.stopViewer = stopViewer;
        
        var this_ = this;

        this.activeTime = moment();
          
        ich['otp-stopFinder']({}).appendTo(this.mainDiv);

        this.agencySelect = this.mainDiv.find('.otp-stopFinder-agencySelect');
        this.module.webapp.transitIndex.loadAgencies(this, function() {
            for(var agencyId in this.module.webapp.transitIndex.agencies) {
                $("<option />").html(agencyId).appendTo(this_.agencySelect);
            }
        });

        this.stopList = this.mainDiv.find('.otp-stopFinder-stopList');

        this.mainDiv.find('.otp-stopFinder-idButton').click(function() {
            var agencyId = this_.agencySelect.val();
            var id = this_.mainDiv.find('.otp-stopFinder-idField').val();
            if(!id || id.length === 0) return;
            this_.module.webapp.transitIndex.loadStopsById(agencyId, id, this, function(data) {
                this_.updateStops(data.stops);
            });
        });

        this.mainDiv.find('.otp-stopFinder-nameButton').click(function() {
            var agencyId = this_.agencySelect.val();
            var name = this_.mainDiv.find('.otp-stopFinder-nameField').val();
            if(!name || name.length === 0) return;
            this_.module.webapp.transitIndex.loadStopsByName(agencyId, name, this, function(data) {
                this_.updateStops(data.stops);
            });
        });

        this.center();
    },    

    updateStops : function(stops) {
        this.stopList.empty();

        var this_ = this;

        if(!stops || stops.length === 0) {
            this.stopList.html("No Stops Found");
            return;
        }

        for(var i = 0; i < stops.length; i++) {
            var stop = stops[i];
            $('<div />')
                .addClass('otp-stopFinder-stopRow')
                .html(stop.stopName + " (#" + stop.id.id + ")")
                .appendTo(this.stopList)
                .data('stop', stop)
                .click(function() {
                    var s = $(this).data('stop');
                    this_.stopViewer.show();
                    this_.stopViewer.setStop(s.id.agencyId, s.id.id, s.stopName);
                    this_.stopViewer.bringToFront();
                });
        }
    },
});
