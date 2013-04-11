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

otp.namespace("otp.modules.multimodal");

otp.modules.multimodal.StopViewerWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,
    
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        
        this.module = module;
        
        this.$().addClass('otp-stopViewer');
        this.$().css('display','none');
        
        this.minimizable = true;
        this.addHeader("Stop Viewer");
        
        /*this.routeLabel = $('<div></div>').appendTo(this.$());
        this.stopLabel = $('<div></div>').appendTo(this.$());
        
        this.timesDiv = $('<div class="otp-stopTimes-list notDraggable"></div>').appendTo(this.$());
        
        this.$().resizable({
            alsoResize: this.timesDiv,
        });*/

        var this_ = this;

        var html = "<div class='notDraggable'>Route: ";
        html += '<select id="'+this.id+'-routeSelect">';
        _.each(module.webapp.transitIndex.routes, function(route, key) {
            html += '<option>('+route.routeData.routeShortName+') '+route.routeData.routeLongName+'</option>';            
        });
        html += '</select>';
        html += "</div>";
        
        $(html).appendTo(this.$());
        

        $('<div class="otp-stopTimes-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
    },
    
    update : function(leg) {
        /*this.routeLabel.html('<b>Route</b>: '+routeName);
        this.stopLabel.html('<b>Stop</b>: '+stopID);

        this.timesDiv.empty();
        var highlightIndex = 0;
        for(var i=0; i<stopTimes.length; i++) {
            var html = '<div class="otp-stopTimes-list-item">';
            if(stopTimes[i] == highlightTime) html += "<b>";
            html += otp.util.Time.formatItinTime(stopTimes[i], "h:mma")
            html += '</div>';
            if(stopTimes[i] == highlightTime) html += "</b>";
            $(html).appendTo(this.timesDiv);
            if(stopTimes[i] == highlightTime) highlightIndex = i;
        }

        var scrollY = this.timesDiv[0].scrollHeight*highlightIndex/stopTimes.length
        if(highlightIndex>0) scrollY = scrollY - this.timesDiv.height()/2 + 10;
        this.timesDiv.scrollTop(scrollY);*/
    }
    
});
