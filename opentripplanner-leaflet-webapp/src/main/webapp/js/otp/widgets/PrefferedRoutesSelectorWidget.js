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

otp.widgets.PreferredRoutesSelectorWidget = 
    otp.Class(otp.widgets.Widget, {

    preferredRoutesControl : null,
    
    initialize : function(id, preferredRoutesControl) {
    
        otp.widgets.Widget.prototype.initialize.call(this, id, preferredRoutesControl.tripWidget.widgetManager);
        
        this.preferredRoutesControl = preferredRoutesControl;
        this.$().addClass('otp-preferredRoutesSelectorWidget');
        
        this.addHeader("Preferred Routes Selector");
    }
    
});
