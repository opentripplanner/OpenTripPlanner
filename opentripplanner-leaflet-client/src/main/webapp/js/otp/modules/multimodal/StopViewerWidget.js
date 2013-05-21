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

    agency_id : null,
    
    activeLeg : null,
    timeIndex : null,
    
    routeLookup : [], // for retrieving route obj from numerical index in <select> element
    
    //variantIndexLookup : null, 
    
    initialize : function(id, module) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, module.webapp.widgetManager);
        
        this.module = module;
        
        this.$().addClass('otp-stopViewer');
        this.$().css('display','none');
        
        this.minimizable = true;
        this.addHeader("Stop Viewer");
        
        var this_ = this;


        var routeSelectDiv = $('<div class="otp-stopViewer-select notDraggable">Route: </div>').appendTo(this.$());
        this.routeSelect = $('<select id="'+this.id+'-routeSelect" style="width:240px;"></select>').appendTo(routeSelectDiv)
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


        var variantSelectDiv = $('<div class="otp-stopViewer-select notDraggable">Variant: </div>').appendTo(this.$());
        this.variantSelect = $('<select id="'+this.id+'-variantSelect" style="width:240px;"></select>').appendTo(variantSelectDiv)
        .change(function() {
            this_.newVariantSelected();
        });

        var stopSelectDiv = $('<div class="otp-stopViewer-select notDraggable">Stop: </div>').appendTo(this.$());
        this.stopSelect = $('<select id="'+this.id+'-stopSelect" style="width:240px;"></select>').appendTo(stopSelectDiv)
        .change(function() {
            this_.newStopSelected();
        });

        this.timesDiv = $("<div class='otp-stopViewer-stopTimes notDraggable'></div>");
        this.timesDiv.appendTo(this.$());        

        $('<div class="otp-stopViewer-stopTimes-advancer" style="left:0px;">&laquo;</div>')
        .appendTo(this.timesDiv)
        .click(function(evt) {
            this_.updateTimes(-1);
        });

        $('<div class="otp-stopViewer-stopTimes-advancer" style="right:0px;">&raquo;</div>')
        .appendTo(this.timesDiv)
        .click(function(evt) {
            this_.updateTimes(1);
        });


        $('<div class="otp-stopTimes-close">[<a href="#">CLOSE</a>]</div>')
        .appendTo(this.$())
        .click(function() {
            this_.$().hide();
        });
        
    },
    
    newRouteSelected : function() {
        this.agency_id = null;
        this.activeLeg = null;
        var route = this.routeLookup[this.routeSelect.prop("selectedIndex")]
        this.agency_id = route.routeData.id.agencyId + "_" + route.routeData.id.id;
        this.variantSelect.empty();
        this.stopSelect.empty();
        this.checkAndLoadVariants();     
    },
    
    newVariantSelected : function() {
        this.activeLeg = null;
        var variantName = this.variantSelect.val();
        //console.log("new variant selected: "+variantName);
        this.stopSelect.empty();
        this.setActiveVariant(this.module.webapp.transitIndex.routes[this.agency_id].variants[variantName]);
    },
    
    newStopSelected : function() {
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
    },
        
    clearTimes : function() {
        this.times = null;
        this.timeIndex = null;
        if(this.rightTime) this.rightTime.remove();
        if(this.centerTime) this.centerTime.remove();
        if(this.leftTime) this.leftTime.remove();    
    },
        
    update : function(leg, times) {
        this.clearTimes();
        this.activeLeg = leg;
        this.activeTime = leg.startTime;
        this.times = times;

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
        var route = this.module.webapp.transitIndex.routes[this.agency_id];
        this.activeVariant = route.variants[variantData.name];
        $('#'+this.id+'-variantSelect option:eq('+(this.activeVariant.index)+')').prop('selected', true);

        //console.log(variant.stops);
        var stopSelect = $('#'+this.id+'-stopSelect');
        stopSelect.empty();
        var selectedStopIndex = 0;
        for(var i=0; i<this.activeVariant.stops.length; i++) {
            var stop = this.activeVariant.stops[i];
            $('<option>'+stop.name+' ('+stop.id.id+')</option>').appendTo(stopSelect);
            if(this.activeLeg && this.activeLeg.from.stopId.agencyId == stop.id.agencyId && this.activeLeg.from.stopId.id == stop.id.id) {
                selectedStopIndex = i;
                //this.selectedStop = stop;
                this.updateTimes();
            }
            $('#'+this.id+'-stopSelect option:eq('+selectedStopIndex+')').prop('selected', true);
            
        }
        if(!this.activeLeg) {
            this.newStopSelected();
        }
    },
    
    updateTimes : function(delta) {
        //console.log("uT delta="+delta);
        if(!this.timeIndex) {
            var bestTimeDiff = 1000000000;
            for(var i=0; i<this.times.length; i++) {
                var timeDiff = Math.abs(this.activeTime - this.times[i]);
                if(timeDiff < bestTimeDiff) {
                    this.timeIndex = i;
                    bestTimeDiff = timeDiff;
                }
            }
        }
         
        if(delta && delta == 1) {
            this.timeIndex++;
            this.leftTime.remove();
            this.centerTime.animate({
                left: 40,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            });
            this.leftTime = this.centerTime;
            
            this.rightTime.animate({
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            });
            this.centerTime = this.rightTime;

            this.rightTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex+1) + '<div>')
            .css({
                left: 300,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv)
            .animate({
                left: 200
            });
        }
        else if(delta && delta == -1) {
            this.timeIndex--;
            this.rightTime.remove();
            this.centerTime.animate({
                left: 200,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            });
            this.rightTime = this.centerTime;
            
            this.leftTime.animate({
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            });
            this.centerTime = this.leftTime;

            this.leftTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex-1) + '<div>')
            .css({
                left: -60,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv)
            .animate({
                left: 40
            });
        }
        else {
            this.leftTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex-1) + '<div>')
            .css({            
                left: 40,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv);
           
            this.centerTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex) + '<div>')
            .css({            
                left: 110,
                width: 80,
                'font-size': 20,
                'padding-top': 10
            })
            .appendTo(this.timesDiv);
     
            this.rightTime = $('<div class="otp-stopViewer-stopTimes-timeBox">' + this.getTime(this.timeIndex+1) + '<div>')
            .css({
                left: 200,
                width: 60,
                'font-size': 14,
                'padding-top': 13
            })
            .appendTo(this.timesDiv);
        }
    },
    
    getTime : function(index) {
        if(index < 0 || index >= this.times.length) return "";
        return otp.util.Time.formatItinTime(this.times[index], "h:mma");
    }
    
});
