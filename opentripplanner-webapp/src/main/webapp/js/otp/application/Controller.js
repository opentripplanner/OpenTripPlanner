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

otp.namespace("otp.application");

/**
 * responsible for building and wiring together the trip planner appliction
 * will also parse any command line parameters, and execute any commands therein
 */
otp.application.Controller = {

    // config
    extent               : null,
    url                  : null,

    // custom icons will be used for these agency ids
    useCustomIconsForAgencies: [],

    // whether to add the systemmap to the accordion
    hasSystemMap : false,

    plannerContextMenu   : true,
    mapContextMenu       : false,

    // creation
    map        : null,
    ui         : null,
    poi        : null,
    cm         : null,
    planner    : null,
    params     : null,
    
    /** */
    initialize : function(config)
    {
        console.log("enter application.Controller constructor");
        otp.configure(this, config);

        this.params  = new otp.utils.ParseUrlParams();
        this.map  = new otp.core.Map({
                defaultExtent  : this.extent, 
                cm             : this.cm, 
                attribution    : otp.util.ExtUtils.MAP_ATTRIBUTION
        }); 
        this.ui   = new otp.core.UI({map:this.map});

        // initialize utilities
        otp.util.imagePathManager.addCustomAgencies(this.useCustomIconsForAgencies);
        
        ////////// trip planner ///////////
        this.poi     = new otp.planner.poi.Control({map:this.map.getMap()});
        this.planner = new otp.planner.Planner({url:this.url, map:this.map, poi:this.poi});
        this.makeContextMenu();
        this.ui.accordion.add(this.planner.getPanel());

        if (this.hasSystemMap)
        {
        	// XXX how to set the url? this.url seems to be null?
        	this.sm      = new otp.systemmap.Systemmap({map: this.map, url: '/opentripplanner-api-extended/ws/routes', popupUrl: '/opentripplanner-api-extended/ws/departures'});
        	this.ui.accordion.add(this.sm.getPanel());
        	// we want the system map to be the default panel now
        	this.ui.accordion.layout.setActiveItem(1);
        }
        
        this.ui.doLayout();

        this.load();

        console.log("exit application.Controller constructor");
    },

   /**
    * will call map & trip planner form routines in order to populate based on URL params
    */
    load : function()
    {
        try
        {
            // do the POI and the openTool stuff (and if a POI exists, suspend the pan on the tool)
//            var p = this.params.getPoi(this.poi, this.map);

            // trip planner forms
            var forms  = this.planner.getForms();
            forms.populate(this.params.m_params);
            if(this.params.hasSubmit())
            {
                forms.submit();
            }
        }
        catch(e)
        {
            console.log("ParseUrlParams highlight " + e)
        }
    },

   /**
    * create a right-click context menu
    */
    makeContextMenu : function()
    {
        var  cmConfig = {};
        if(this.plannerContextMenu)
            cmConfig.forms = this.planner.getForms();
        if(this.mapContextMenu)
            cmConfig.map = this.map;

        this.cm = new otp.planner.ContextMenu(cmConfig);
        this.cm.map = this.map;  // NOTE: must add the map back to the cm (in case map no mapping params)
    },

    CLASS_NAME : "otp.application.Controller"
};

otp.application.Controller = new otp.Class(otp.application.Controller);
