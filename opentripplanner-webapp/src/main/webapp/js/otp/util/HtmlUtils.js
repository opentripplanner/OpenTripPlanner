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

otp.namespace("otp.util");

/**
 * Utility routines for Analytics
 */
otp.util.HtmlUtils = {

    defaultLogo : 'images/ui/logoSmall.png',
    defaultAlt  : 'OpenTripPlanner',

    /** routines to fixup the HTML shell (e.g., localize strings, add logos, etc...) */
    fixHtml : function(config)
    {
        try
        {
            if(!this.hasLogoLinkImg())
                this.drawCustomLogo(config.logo);
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }
    },

    hideShowElement : function(elem)
    {
        var node = me.parentNode.parentNode.className;
        if(node.indexOf(' expanded') > 0)
            me.parentNode.parentNode.className = node.replace(' expanded', '');
        else
            me.parentNode.parentNode.className = node + ' expanded';
    
        return false;
    },

    /** create logo image, using a custom logo if specified */
    drawCustomLogo : function(logo, alt)
    {
        
        
        try
        {
            var logoPath = (typeof logo === 'string') ? logo : this.defaultLogo;
            var altStr   = (typeof logo === 'string') ? alt  : this.defaultAlt;

            // TODO: refactor w/out Extjs ???
            var logoAnchorWrapper = this.getLogoLink(); 
            Ext.DomHelper.append(logoAnchorWrapper, {tag: 'img',
                                                     alt: altStr,
                                                     src: logoPath
            });
        }
        catch(e)
        {
            console.log("GA EXCEPTION: AnalyticsUtils.importGoogleAnalytics threw exception " + e);
        }
    },
    
    /** */
    getLogoLink : function(path)
    {
        if(!path)
            path='a';
        return Ext.get('logo').query(path)[0];
    },

    /** check whether the logo link has an img tag */
    hasLogoLinkImg : function()
    {
        var retVal = false;
        try 
        {
            var x = this.getLogoLink('a/img');
            if(x)
                retVal = true;
        }
        catch(e)
        {}
        return retVal;
    },

    CLASS_NAME : "otp.util.HtmlUtils"
};