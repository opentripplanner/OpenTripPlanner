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

otp.namespace("otp.core");

otp.core.Measure = {

    /** ptr to OpenLayers.map (not otp.core.Map controller) */
    map             : null,
    locale          : otp.locale.English,

    /**
     *  panel is the object that will be showing the results - default is a tooltip 
     *  NOTE: assumes these methods: show(), hide(), activate(), deactivate() 
     */
    outputPanel     : null,

    /** @string div name where the panel should float in (eg: for tooltip, this is the map div) */
    outputParentDiv : otp.util.OpenLayersUtils.MAP_PANEL,

    /** outputMethod is a function that will be called with meausre updates - scope is defined by outputPanel*/
    outputMethod    : console.log,

    m_control       : null,
    m_onOff         : false,

    /**
     * Property: units
     * {String} One of 'english', 'metric', or 'geographic'.  Default is 'english'.
     */
    units          : 'english',

    // style the sketch fancy
    sketchSymbolizers : {
        "Point": {
            pointRadius: 4,
            graphicName: "square",
            fillColor:   "white",
            fillOpacity: 1,
            strokeWidth: 1,
            strokeOpacity: 1,
            strokeColor: "#333333"
        },
        "Line": {
            strokeWidth:     2,
            strokeOpacity:   1,
            strokeColor:     otp.util.OpenLayersUtils.ORANGE,
            strokeDashstyle: "dash"
        },
        "Polygon": {
            strokeWidth: 2,
            strokeOpacity: 1,
            strokeColor: "#666666",
            fillColor: "white",
            fillOpacity: 0.3
        }
    },

    /**
     * @consturctor
     * @param {Object} config
     */
    initialize : function(config)
    {
        // step 1: configure the objec
        console.log("enter measure.Measure constructor");
        otp.configure(this, config);

        // step 2: create the control
        var style = new OpenLayers.Style();
        style.addRules([
            new OpenLayers.Rule({symbolizer: this.sketchSymbolizers})
        ]);
        var styleMap = new OpenLayers.StyleMap({"default": style});

        this.m_control = new OpenLayers.Control.Measure(
            OpenLayers.Handler.Path, {
                persist: true,
                handlerOptions: {
                    layerOptions: {styleMap: styleMap}
                },
                callbacks: {
                     modify: function(point, feature) {
                            this.measurePartial(point, feature.geometry);
                     }
                },
                persist: true
            }
        );
        this.m_control.events.on({
            "measure":        this.measureCB,
            "measurepartial": this.measureCB,
            "activate":       function() { this.map.div.style.cursor = "crosshair";},
            "deactivate":     function() { this.map.div.style.cursor = "default";  
                                           // BELOW: make doubly sure that this thing turns off (done mostly to get rid of tootip)
                                           this.off(); 
                                           var thisObj = this;
                                           window.setTimeout( function(){ thisObj.off() }, 500); },
            "scope":          this
            
        });

        // step 3: default make of an output panel (tool tip)
        if(this.outputPanel == null)
            this.makeExtToolTip();

        // step 4: add control to map
        try
        {
            this.map.addControl(this.m_control);
            this.m_control.displaySystem = this.units;
        }
        catch(e)
        {
        }

        console.log("exit measure.Measure constructor");
    },

    /** OL callback on mousemovement */
    measureCB : function(event)
    {
        var units    = event.units;
        var measure  = event.measure;

        var prec = 3;
        if(units == "in" || units == "ft")
            prec = 0;

        var out = "measure: " + measure.toFixed(prec) + " " + units;
        this.write(out);
    },

    /** big circle calculation (over the default planner calcuation) */
    geodesic : function(bool)
    {
        if(bool == true)
            ;
        else
            ;
    },

    /** turn the measure control on */
    on : function()
    {
        try
        {
            this.m_control.activate();
            this.toggleOutput(true);
            this.m_onOff = true;
            otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.MEASURE);
        }
        catch(e)
        {
        }
    },

    /** turn the measure control off */
    off : function()
    {
        try
        {
            this.m_control.deactivate();
            this.toggleOutput(false);
            this.m_onOff = false;
        }
        catch(e)
        {
        }
    },


    /** turn the measure control on & off */
    toggle : function()
    {
        if(this.m_onOff == false)
        {
            this.on();
            this.m_onOff = true;
        }
        else
        {
            this.off();
            this.m_onOff = false;
        }
    },

    /** UI: send measure result to output */
    write : function(msg)
    {
        try
        {
            this.outputMethod.call(this.outputPanel, msg);
            this.outputPanel.show();
        }
        catch(e)
        {
        }
    },

    /** UI: hide / show for the output item */
    makeButton : function(tbar)
    {
        var retVal = {};
        try 
        {
             // step 1: button config object
            retVal = {
                id:            'measure-button',
                iconCls:       'measure-button',
                text:          this.locale.buttons.measure,
                tooltip:       this.locale.buttons.measureTip,
                enableToggle:  true,
                scope:         this,
                handler:       this.toggle
            }

            // step 2: add button to tbar
            if(tbar != null)
            {
                tbar.addButton(retVal);
            }
        }
        catch(e)
        {
        }
        
        return retVal;
    },


    /** UI: hide / show for the output item */
    toggleOutput : function(bool)
    {
        try 
        {
            if(bool == false)
            {
                this.outputPanel.hide();
                this.outputPanel.disable();
            }
            else
            {
                this.outputPanel.enable();
                this.clearOutput();
                this.outputPanel.show();
            }
        }
        catch(e)
        {
        }
    },

    /** UI: default clear method of the panel */
    clearOutput  : function(bool)
    {
        this.write(this.locale.buttons.measureInfo);
    },

    /** UI default output panel -- a tooltip that follows the mouse around */
    makeExtToolTip : function()
    {
        try 
        {
            this.outputPanel = new Ext.ToolTip({
                target:     this.outputParentDiv,
                title:      this.locale.buttons.measureInfo,
                width:      175,
                dismissDelay: 0,
                showDelay:    0,
                disabled:   true,
                hidden:     true,
                trackMouse: true
            });
            this.outputMethod = this.outputPanel.setTitle;//.el.update;
        }
        catch(e)
        {
        }

        console.log("exit measure.Measure constructor");
    },

    CLASS_NAME : "otp.core.Measure"
};
otp.core.Measure =  new otp.Class(otp.core.Measure);
