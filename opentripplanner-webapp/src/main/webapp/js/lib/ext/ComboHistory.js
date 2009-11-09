// create namespace for plugins
Ext.namespace('Ext.ux.plugins');
 
/**
 * Ext.ux.plugins.ComboHistory plugin for Ext.form.Combobox
 *
 * @author  Frank Purcell
 * @based on the Ext plugin example by Jozef Sakalos
 * @date    January 7, 2008
 *
 * @class Ext.ux.plugins.ComboHistory
 * @extends Ext.util.Observable
 */
Ext.ux.plugins.ComboHistory = function(config) {
    Ext.apply(this, config);
};
 
// plugin code
Ext.extend(Ext.ux.plugins.ComboHistory, Ext.util.Observable, 
{
    init:function(combo) 
    {

   var cp = new Ext.state.CookieProvider({
       expires: new Date(new Date().getTime()+(1000*60*60*24*30)) //30 days
   });
   Ext.state.Manager.setProvider(cp);

   var data = [
                        ['ZUS', 'ZUnited States', 'ux-flag-us'],
                        ['ZDE', 'ZGermany', 'ux-flag-de'],
                        ['ZFR', 'ZFrance', 'ux-flag-fr']
                    ];

    cp.set( "Z", data);
    combo.m_cp = cp;

        Ext.apply(combo, 
        {
            expand:combo.expand.createSequence(function() 
            {
                try
                {
                    var n = this.store.getCount();
 
                    var Z = this.m_cp.get("Z");
                    var z = new Ext.data.SimpleStore({
                        fields: ['countryCode', 'countryName', 'countryFlag'],
                        data:   Z
                    });
                    this.store.add(z.getRange(0,2));
                }
                catch(Ex)
                {
                }
            })
/*
            setValue:combo.setValue.createSequence(function(value) {
                var j = 3;
            })
*/
        });
        
        var i = 2;

    }
});
