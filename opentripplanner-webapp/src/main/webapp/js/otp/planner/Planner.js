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

    locale        : null,

    // pointer to the map / components
    map           : null,
    planner       : null,
    controller    : null,
    ui            : null,

    // configuration
    url                     : null,
    poi                     : null,
    fromToOverride          : null,
    linkTemplates           : null,
    geocoder                : null,
    useOptionDependencies   : null,
    templates               : null,

    // new tab (itineraries tabs) management
    m_tabs        : null,
    m_tabPanel    : null,
    m_mainPanel   : null,
    m_activeTabID : 0,
    m_numTabs     : 0,
    m_tabCount    : 0,
    m_forms       : null,
    m_renderer    : null,
    m_topoRenderer : null,

    /** */
    initialize : function(config)
    {
        this.planner = this;
        otp.configure(this, config);

        if(this.templates == null)
            this.templates = new otp.planner.Templates({locale : this.locale});

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
            items:      [this.m_tabPanel],
            listeners:  {'expand': {fn: this.panelExpanded, scope: this}}
        });

        // step 3: create the render and form (and add the form to the tab panel
        this.m_renderer = new otp.planner.Renderer(this);
        this.m_topoRenderer = new otp.planner.TopoRenderer({panel:this.ui.innerSouth});
        this.m_forms    = new otp.planner.Forms(this);
        this.addFormPanel(this.m_forms.getPanel());

        // step 4: override the Form's submit method to something we own here in planner
        var thisObj = this;
        this.m_forms.submitSuccess = function(){ thisObj.formSuccessCB(); };
        this.m_forms.submitFailure = function(){ thisObj.formFailureCB(); };

        m = this.map.getMap();
        m.events.register('click', m, function (e) {
        });
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
    clearForms : function() {
        this.m_forms.clear();
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
    clear : function() {
        this.showFormTab();
        this.m_forms.clear();
        this.m_renderer.clear();
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
    getTripInfo : function(mapURL, txtURL) {
        var retVal = {
            url : "",
            txt : "",
            txtUrl : "",
            valid : false
        };
        if (mapURL == null) {
            mapURL = "http://plan.opentripplanner.org";
        }

        if (txtURL == null) {
            txtURL = "http://text.opentripplanner.org";
        }

        var info   = this.getForms().getFormData(mapURL);
        retVal.txt = this.templates.tripFeedbackDetails.applyTemplate(info);
        retVal.url = this.templates.tripPrintTemplate.applyTemplate(info);
        info.url   = txtURL;
        retVal.txtUrl = this.templates.txtPlannerURL.applyTemplate(info);
        retVal.valid  = (
                ((info.from != null && info.from.length > 0) || (info.fromPlace != null && info.fromPlace.length > 0)) 
                && ((info.to != null && info.to.length > 0) || (info.toPlace != null && info.toPlace.length > 0)));

        return retVal;
    },

    /** */
    getActiveItinerary : function() {
        var retVal = null;

        var tt = this.m_tabs[this.m_activeTabID];
        if (tt) {
            retVal = tt.getActiveItinerary();
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
/** seems to be fixed in Extjs 3.0 ... at least on the Mac

        if(Ext.isOpera)
        {
            Ext.Msg.alert('Opera Browser', "The Opera Browser <b>will not</b> show your trip planning details.  I suggest that you use another browser (eg: Firefox, Safari, Chrome or IE) when trip planning with this map.");
        }
*/

        try 
        {
            var trip = new otp.planner.TripTab({planner:this, ui:this.ui, xml:xml, templates:this.templates, linkTemplates:this.linkTemplates, id:++this.m_tabCount, renderer:this.m_renderer, topoRenderer:this.m_topoRenderer, locale:this.locale, request:request}); 
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
        
        return true;
    },

    /**
     *  changing tabs (both forms / trip plans) triggers events to clear the map, etc...   
     */
    tabChange : function(tabPanel, activeTab) 
    {
        this.m_renderer.clear();
        var newTab = this.m_tabs[activeTab.id];
        if (newTab != null) {
            this.m_activeTabID = activeTab.id;
            newTab.draw();
        } else {
            this.m_activeTabID = 0;
            this.controller.deactivate(this.CLASS_NAME);

            // hide the topo map
            if (this.ui.innerSouth.isVisible()) {
                this.ui.innerSouth.hide();
                this.ui.viewport.doLayout();
            }
            this.m_forms.panelActivated();
        }
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

    /*
     * called when this particular panel has been given focus
     */
    panelExpanded: function()
    {
        var activeTab = this.m_tabPanel.getActiveTab();
        this.tabChange(this.m_tabPanel, activeTab);
    },
    
    CLASS_NAME: "otp.planner.Planner"
};

otp.planner.Planner = new otp.Class(otp.planner.Planner);
