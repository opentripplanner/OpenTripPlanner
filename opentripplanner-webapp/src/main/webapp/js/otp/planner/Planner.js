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

otp.namespace("otp.planner");

/**
  * Web TripPlanner
  * 
  * otp.planner.Planner's purpose is to act as the main controller for the trip planner.  
  *
  * Coordinates the rendering of the trip planner form, the parsing of any trip planner response, and
  * display of the resulting trip plans.
  */
otp.planner.Planner = {

    locale        : otp.locale.English,

    // pointer to the map / components
    map           : null,
    planner       : null,
    controller    : null,

    // configuration
    url                     : null,
    poi                     : null,
    useGenericRouteIcons    : true,  // needed here...sent down to Renderer constructor

    // new tab (itineraries tabs) management
    m_tabs        : null,
    m_tabPanel    : null,
    m_mainPanel   : null,
    m_activeTabID : 0,
    m_numTabs     : 0,
    m_tabCount    : 0,
    m_forms       : null,
    m_renderer    : null,

    /** */
    initialize : function(config)
    {
        console.log("enter planner.Planner constructor");
        this.planner = this;
        otp.configure(this, config);

        // step 0: a bit of config (fixes up the controller, if it's missing anything)
        this.controller = otp.util.ObjUtils.defaultController(this.controller);

        // step 1: create the tabs panel
        this.m_tabs     = new Array();
        this.m_tabPanel = new Ext.TabPanel({
            id:                'tripplanner-panel',
            title:             this.locale.tripPlanner.labels.panelTitle,
            activeTab:         0,
            layoutOnTabChange: true,
            resizeTabs:        true,
            enableTabScroll:   true,
            minTabWidth:       75,
            autoScroll:        false,
            plugins:           new Ext.ux.TabCloseMenu()
        });
        this.m_tabPanel.on('tabchange', this.tabChange, this);

        // step 2: create the panel that holds the tabs
        this.m_mainPanel = new Ext.Panel({
            layout:     'fit',
            id:         'tp-accordion',
            title:      this.locale.tripPlanner.labels.panelTitle,
            iconCls:    'planner-panel',
            autoShow:   true,
            border:     false,
            items:      [this.m_tabPanel]
        });

        // step 3: create the render and form (and add the form to the tab panel
        this.m_renderer = new otp.planner.Renderer(this);
        this.m_forms    = new otp.planner.Forms(this);
        this.addFormPanel(this.m_forms.getPanel());

        // step 4: override the Form's submit method to something we own here in planner
        var thisObj = this;
        this.m_forms.submitSuccess = function(){ thisObj.formSuccessCB() };
        this.m_forms.submitFailure = function(){ thisObj.formFailureCB() };

        m = this.map.getMap();
        m.events.register('click', m, function (e) {
        });

        console.log("exit planner.Planner constructor");
    },

    /** enlarge the main view panel of this object */ 
    expand : function(c)
    {
        if(c == null) c = this;
        if(c && c.m_mainPanel)
        {
            c.m_mainPanel.bubble(c.m_mainPanel.expand);
        }
    },

    /** */
    getPanel : function() 
    {
        return this.m_mainPanel;
    },

    /** */
    getMainPanel : function() 
    {
        return this.m_mainPanel;
    },

    /** */
    getForms : function()
    {
        return this.m_forms;
    },

    /** */
    clearForms : function()
    {
        try
        {
            this.m_forms.clear();
        }
        catch(e)
        {
            console.log("exception Planner.clearForms " + e);
        }
    },

    /** */
    showFormTab : function(btn) 
    {
        this.m_activeTabID = 0;
        this.m_tabPanel.setActiveTab(0);
        this.controller.deactivate(this.CLASS_NAME);
    },

    /** */
    populateFormTab : function(data) 
    {
        this.m_forms.populate(data);
        this.showFormTab();
    },

    /** */
    getTabPanel : function() 
    {
        return this.m_tabPanel;
    },

    /** */
    clear : function()
    {
        try
        {
            this.showFormTab();
            this.m_forms.clear();
            this.m_renderer.clear();
        }
        catch(e)
        {
            console.log("exception Planner.clear " + e);
        }
    },

    /** */
    focus : function()
    {
        try
        {
            this.showFormTab();
            this.m_mainPanel.bubble(this.m_mainPanel.expand);
        }
        catch(e) 
        {
            console.log("exception in Planner.focus (might be nothing if you're not using the trip planner & extjs): " + e);
        }
    },

    /** */
    getTripInfo : function(mapURL, txtURL)
    {
        var retVal = {url:"", txt:"", txtUrl:"", valid:false};
        try
        {
            if(mapURL == null)
                mapURL = "http://plan.opentripplanner.org";

            if(txtURL == null)
                txtURL = "http://text.opentripplanner.org";

            var info      = this.getForms().getFormData(mapURL);
            retVal.txt    = otp.planner.Templates.tripFeedbackDetails.applyTemplate(info);
            retVal.url    = otp.planner.Templates.tripPrintTemplate.applyTemplate(info);
            info.url      = txtURL;
            retVal.txtUrl = otp.planner.Templates.txtPlannerURL.applyTemplate(info);
            retVal.valid  = (
                            ((info.from      != null && info.from.length      > 0) ||
                             (info.fromPlace != null && info.fromPlace.length > 0) ) 
                            &&
                            ((info.to        != null && info.to.length        > 0) ||
                             (info.toPlace   != null && info.toPlace.length   > 0))
            );
        }
        catch(e)
        {
            console.log("exception Planner.getTripInfo " + e);
        }

        return retVal;
    },

    /** */
    getActiveItinerary : function()
    {
        var retVal = null;

        try
        {
            var tt = this.m_tabs[this.m_activeTabID];
            if(tt)
            {
                retVal = tt.getActiveItinerary();
            }
        }
        catch(e)
        {
            console.log("exception Planner.getActiveItinerary " + e);
        }

        return retVal;
    },


    /** */
    getSelectedRoutes : function()
    {
        var retVal = [];

        var ai = this.getActiveItinerary(); 
        if(ai)
        {
            retVal = ai.getRoutes();
        }

        return retVal;
    },

    /**
     *  newTripPlan is called when the trip planner is successful in planning a trip
     *  
     *  @see form submit for this panel
     */
    newTripPlan : function(xml, request)
    {
        console.log("enter Planner.newTripPlan");

/** seems to be fixed in Extjs 3.0 ... at least on the Mac

        if(Ext.isOpera)
        {
            Ext.Msg.alert('Opera Browser', "The Opera Browser <b>will not</b> show your trip planning details.  I suggest that you use another browser (eg: Firefox, Safari, Chrome or IE) when trip planning with this map.");
        }
*/

        try 
        {
            console.log("Planner.newTripPlan: create new trip tab");
            var trip = new otp.planner.TripTab({planner:this, xml:xml, id:++this.m_tabCount, renderer:this.m_renderer, locale:this.locale, request:request}); 
            var newTab = trip.getPanel();
            if(newTab && trip.isValid())
            {
                this.m_activeTabID = newTab.getId();
                this.m_tabs[this.m_activeTabID] = trip;
                this.m_tabPanel.add(newTab);
                this.m_tabPanel.setActiveTab(newTab);
            }
            else if (!newTab)
            {
                // this path is taken when there are no itineraries found in the server response
                // the tab does not get created in this case
                return false;
            }
        } 
        catch(e) 
        {
            this.m_activeTabID = 0;
            console.log("exception Planner.newTripPlan: exception " + e);
        }
        
        console.log("exit Planner.newTripPlan");
        return true;
    },

    /**
     *  changing tabs (both forms / trip plans) triggers events to clear the map, etc...   
     */
    tabChange : function(tabPanel, activeTab) 
    {
        console.log("enter Planner.tabChange");
        try
        {
            this.m_renderer.clear();
            var newTab = this.m_tabs[activeTab.id];
            if(newTab != null)
            {
                this.m_activeTabID = activeTab.id;
                newTab.draw();
            }
            else
            {
                this.m_activeTabID = 0;
                this.controller.deactivate(this.CLASS_NAME);
            }
        }
        catch(e)
        {
            console.log("exception Planner.tabChange " + e);
        }
        console.log("exit Planner.tabChange");
    },

    /**
     * adds a form panel to a new tab in the trip planner tab'd panel.
     * NOTE: this routine will only be called once by Forms, to add the form tab to the layout. 
     */
    addFormPanel : function(fp) 
    {
        // add panel
        this.m_tabPanel.add(fp);
        this.m_tabPanel.activate(0);
        this.m_tabPanel.doLayout();
    },

    CLASS_NAME: "otp.planner.Planner"
}

otp.planner.Planner = new otp.Class(otp.planner.Planner);
