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

otp.widgets.transit.TripViewerWidget = 
    otp.Class(otp.widgets.Widget, {

    module : null,

    agency_id : null,
    
    activeLeg : null,
    timeIndex : null,
    
    routeLookup : [], // for retrieving route obj from numerical index in <select> element
    
    lastSize : null,
    //variantIndexLookup : null, 
    
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        module.addWidget(this);
        
        this.module = module;
        
        this.$().addClass('otp-tripViewer');
        this.$().resizable();
        this.$().css('display','none');
        
        this.minimizable = true;
        this.addHeader("Trip Viewer");
        
        var this_ = this;


        var routeSelectDiv = $('<div class="otp-tripViewer-select notDraggable" />').appendTo(this.$());
        $('<div style="float: left;">Route:</div>').appendTo(routeSelectDiv);
        this.routeSelect = $('<select id="'+this.id+'-routeSelect" style="width:100%;"></select>')
        .appendTo($('<div style="margin-left:60px;">').appendTo(routeSelectDiv))
        .change(function() {
            this_.newRouteSelected();
        });

        _.each(module.webapp.transitIndex.routes, function(route, key) {
            var optionHtml = '<option>';
            if(route.routeData.routeShortName) optionHtml += '('+route.routeData.routeShortName+') ';
            if(route.routeData.routeLongName) optionHtml += route.routeData.routeLongName;
            optionHtml += '</option>';
            this_.routeSelect.append($(optionHtml));
            this_.routeLookup.push(route);
        });


        var variantSelectDiv = $('<div class="otp-tripViewer-select notDraggable" />').appendTo(this.$());
        $('<div style="float: left;">Variant:</div>').appendTo(variantSelectDiv);
        this.variantSelect = $('<select id="'+this.id+'-routeSelect" style="width:100%;"></select>')
        .appendTo($('<div style="margin-left:60px;">').appendTo(variantSelectDiv))
        .change(function() {
            this_.newVariantSelected();
        });

        this.stopList = $('<div class="otp-tripViewer-stopList notDraggable" />').appendTo(this.$());

        $('<div class="otp-tripViewer-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
        this.$().resizable({
            minWidth: 200,
            alsoResize: this.stopList,
        });
        
    },
    
    newRouteSelected : function() {
        this.agency_id = null;
        this.activeLeg = null;
        var route = this.routeLookup[this.routeSelect.prop("selectedIndex")]
        this.agency_id = route.routeData.id.agencyId + "_" + route.routeData.id.id;
        this.variantSelect.empty();
        this.stopList.empty();
        this.checkAndLoadVariants();     
    },
    
    newVariantSelected : function() {
        this.activeLeg = null;
        var variantName = this.variantSelect.val();
        //console.log("new variant selected: "+variantName);
        this.stopList.empty();
        this.setActiveVariant(this.module.webapp.transitIndex.routes[this.agency_id].variants[variantName]);
    },
    
    /*newStopSelected : function() {
        this.clearTimes();
        var stop = this.activeVariant.stops[this.stopSelect.prop("selectedIndex")];
        
        var this_ = this;
        this.module.webapp.transitIndex.runStopTimesQuery(stop.id, null, this.activeTime, this, function(data) {
            var stopTimes = [];
            for(var i=0; i<data.stopTimes.length; i++) {
                var st = data.stopTimes[i].StopTime || data.stopTimes[i];
                if(st.phase == 'departure')
                    stopTimes.push(st.time*1000);
            }
            this_.times = stopTimes;
            this_.updateTimes();
        });
    },*/
        
    /*clearTimes : function() {
        this.times = null;
        this.timeIndex = null;
        if(this.rightTime) this.rightTime.remove();
        if(this.centerTime) this.centerTime.remove();
        if(this.leftTime) this.leftTime.remove();    
    },*/
        
    update : function(leg) {
        //this.clearTimes();
        this.activeLeg = leg;
        this.activeTime = leg.startTime;
        //this.times = times;

        this.agency_id = leg.agencyId + "_" + leg.routeId;
        
        var tiRouteInfo = this.module.webapp.transitIndex.routes[this.agency_id];
        $('#'+this.id+'-routeSelect option:eq('+tiRouteInfo.index+')').prop('selected', true);
        
        this.checkAndLoadVariants();      
    },
        
    checkAndLoadVariants : function() {
        var tiRouteInfo = this.module.webapp.transitIndex.routes[this.agency_id];
        if(tiRouteInfo.variants != null) {
            //console.log("variants exist");
            this.updateVariants();
        }
        else {
            this.module.webapp.transitIndex.loadVariants(this.agency_id, this, this.updateVariants);
        }
    },

    updateVariants : function() {
        var this_ = this;
        var route = this.module.webapp.transitIndex.routes[this.agency_id];

        if(!route.variants) {
            console.log("ERROR: transitIndex.routes.["+this.agency_id+"].variants null in StopViewerWidget.updateVariants()");
            return;
        }
        
        if(this.activeLeg) {
            this.module.webapp.transitIndex.readVariantForTrip(this.activeLeg.agencyId, this.activeLeg.tripId, this, this.setActiveVariant);
        }

        this.variantSelect.empty();
        _.each(route.variants, function(variant) {
            $('<option>'+variant.name+'</option>').appendTo(this_.variantSelect);
        });
        
        if(!this.activeLeg) {
            this.newVariantSelected();
        }
    },
    
    setActiveVariant : function(variantData) {
        var this_ = this;
        var route = this.module.webapp.transitIndex.routes[this.agency_id];
        this.activeVariant = route.variants[variantData.name];
        $('#'+this.id+'-variantSelect option:eq('+(this.activeVariant.index)+')').prop('selected', true);
          
        this.stopList.empty();
        var selectedStopIndex = 0;
        for(var i=0; i<this.activeVariant.stops.length; i++) {
            var stop = this.activeVariant.stops[i];

            var row = $('<div class="otp-tripViewer-stopRow" />').appendTo(this.stopList);
            
            var stopIcon = $('<div style="width: 30px; height: 32px; overflow: hidden; float:left; margin-left: 2px;" />').appendTo(row);
            
            // use the appropriate line/stop graphic          
            var lineImg;
            if(i == 0) {
                lineImg = $('<img src="images/widget-trip-stop-first.png" />');
            }
            else if(i == this.activeVariant.stops.length - 1) {
                lineImg = $('<img src="images/widget-trip-stop-last.png" />');
            }
            else {
                lineImg = $('<img src="images/widget-trip-stop-middle.png" />');
            }

            // append the arrow for the board/alight stop, if applicable
            if(this.activeLeg && i == this.activeLeg.from.stopIndex) {
                $('<img src="images/mode/arrow.png" style="margin: 8px 2px;" />').appendTo(stopIcon);
            }
            else if(this.activeLeg && i == this.activeLeg.to.stopIndex) {
                $('<img src="images/mode/arrow-left.png" style="margin: 8px 2px;" />').appendTo(stopIcon);
            }
            else {
                lineImg.css({ marginLeft : 12 });
            }

            lineImg.appendTo(stopIcon);
            
            // set up the stop name and id/links content            
            var stopText = $('<div style="margin-left: 40px" />').appendTo(row);
            $('<div class="otp-tripViewer-stopRow-name"><b>'+(i+1)+'.</b> '+stop.name+'</div>').appendTo(stopText);
            var idLine = $('<div class="otp-tripViewer-stopRow-idLine" />').appendTo(stopText);
            var idHtml = '<span><i>';
            if(stop.url) idHtml += '<a href="'+stop.url+'" target="_blank">';
            idHtml += stop.id.agencyId+' #'+stop.id.id;
            if(stop.url) idHtml += '</a>';
            idHtml += '</i></span>'
            $(idHtml).appendTo(idLine);
            
            $('<span>&nbsp;[<a href="#">Recenter</a>]</span>').appendTo(idLine)
            .data("stop", stop)
            .click(function(evt) {
                var stop = $(this).data("stop");
                this_.module.webapp.map.lmap.panTo(new L.LatLng(stop.lat, stop.lon));
            });
            $('<span>&nbsp;[<a href="#">Viewer</a>]</span>').appendTo(idLine)
            .click(function(evt) {
                console.log("Stop Viewer");
            });
            
            // highlight the boarded stops
            if(this.activeLeg && i >= this.activeLeg.from.stopIndex && i <= this.activeLeg.to.stopIndex) {
                stopIcon.css({ background : 'cyan' });
            }
            
            // set up hover functionality (open popup over stop)
            row.data("stop", stop).hover(function(evt) {
                var stop = $(this).data("stop");
                var latLng = new L.LatLng(stop.lat, stop.lon);
                if(!this_.module.webapp.map.lmap.getBounds().contains(latLng)) return;
                var popup = L.popup()
                    .setLatLng(latLng)
                    .setContent(stop.name)
                    .openOn(this_.module.webapp.map.lmap);
            }, function(evt) {
                this_.module.webapp.map.lmap.closePopup();
            });
            
        }
        
        // scroll to the boarded segment, if applicable
        if(this.activeLeg) {
            var scrollY = this.stopList[0].scrollHeight * this.activeLeg.from.stopIndex / (this.activeVariant.stops.length - 1);
            this.stopList.scrollTop(scrollY);
        }
    },
    
});
