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

    module : null,
    
    header : null,
    itinsAccord : null,
    
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.apply(this, arguments);
        this.module = module;
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
        var html = "<div id='"+divId+"' class='otp-itinsAccord'></div>";
        this.itinsAccord = $(html).appendTo(this.$());

        for(var i=0; i<itins.length; i++) {
            var itin = itins[i];
            var timeStr = otp.util.Time.msToHrMin(itin.endTime-itin.startTime);            
            $("<h3><b>Itinerary "+(i+1)+"</b>: "+timeStr+"</h3>").appendTo(this.itinsAccord).click(function(evt) {
                var arr = evt.target.id.split('-');
                var index = parseInt(arr[arr.length-1]);
                this_.module.drawItinerary(itins[index]);
            });
            this.renderItinerary(itin, i).appendTo(this.itinsAccord);
        }
        
        this.itinsAccord.accordion({
            heightStyle: "fill"
        });
        
        this.$().resize(function(){
            this_.itinsAccord.accordion("resize");
        });

        this.$().draggable({ cancel: "#"+divId });
        
    },
    
    renderItinerary : function(itin, i) {
        var this_ = this;
        
        var divId = this.moduleId+"-itinAccord-"+i;
        var html = "<div id='"+divId+"' class='otp-itinAccord'></div>";
        var itinAccord = $(html);

        for(var l=0; l<itin.legs.length; l++) {
            var leg = itin.legs[l];
            var headerHtml = "<b>"+leg.mode+"</b>";
            if(leg.mode === "WALK" || leg.mode === "BICYCLE") headerHtml += " to "+leg.to.name;
            else if(leg.agencyId !== null) headerHtml += ": "+leg.agencyId+", ("+leg.route+") "+leg.routeLongName;
            $("<h3>"+headerHtml+"</h3>").appendTo(itinAccord).hover(function(evt) {
                var arr = evt.target.id.split('-');
                var index = parseInt(arr[arr.length-1]);
                this_.module.highlightLeg(itin.legs[index]);
            }, function(evt) {
                this_.module.clearHighlights();
            });
            $("<div>Leg details go here</div>").appendTo(itinAccord);
        }
        itinAccord.accordion({
            active: false,
            heightStyle: "content",
            collapsible: true
        });
        return itinAccord;
    }
    
});

    
