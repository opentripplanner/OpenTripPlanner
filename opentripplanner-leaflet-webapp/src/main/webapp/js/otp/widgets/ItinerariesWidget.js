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

otp.widgets.ItinerariesWidget = 
    otp.Class(otp.widgets.Widget, {

    header : null,
    itinsAccord : null,
    
    initialize : function(id) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.$().addClass('otp-itinWidget');
        this.$().resizable();
        this.header = $("<div>X Itineraries Returned:</div>").appendTo(this.$());
    },
    
    updateItineraries : function(itins) {
        var this_ = this;

        this.header.html(itins.length+" Itineraries Returned:");
        
        if(this.itinsAccord !== null) {
            this.itinsAccord.remove();
        }
        var divId = this.moduleId+"-itinsAccord";
        var html = "<div id='"+divId+"' class='otp-itinsAccord'>";
        for(var i=0; i<itins.length; i++) {
            var itin = itins[i];
            var timeStr = otp.util.Time.msToHrMin(itin.endTime-itin.startTime);            
            html += "<h3><b>Itinerary "+(i+1)+"</b>: "+timeStr+"</h3>";
            html += "<div>";
            for(var l=0; l<itin.legs.length; l++) {
                var leg = itin.legs[l];
                html += (l+1)+". <b>"+leg.mode+"</b>";
                if(leg.agencyId !== null) html += ": "+leg.agencyId+", ("+leg.route+") "+leg.routeLongName;
                html += "<br>";
            }
            html += "</div>";                        
        }
        html += "</div>";
        
        this.itinsAccord = $(html).appendTo(this.$());
        this.itinsAccord.accordion({
            heightStyle: "fill"
        });
        
        this.$().resize(function(){
            this_.itinsAccord.accordion("resize");
        });

        this.$().draggable({ cancel: "#"+divId });        
    }
    
});

    
