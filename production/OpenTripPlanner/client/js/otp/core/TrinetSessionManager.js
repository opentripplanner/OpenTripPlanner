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

otp.core.TrinetSessionManager = otp.Class({

    sessionId : null,
    username : null,
    
    checkSessionSuccessCallback : null,
    
    initialize : function(webapp, verifyLoginUrl, redirectUrl, checkSessionSuccessCallback) {
    
        // initialize the session
        if(_.has(webapp.urlParams, 'sessionId')) {
            console.log("received session id: " + webapp.urlParams['sessionId']);
            this.checkSession(webapp.urlParams['sessionId'], checkSessionSuccessCallback);
        }
        else { // no sessionId passed in; must request one from server
            console.log("creating new session..");
            this.newSession(verifyLoginUrl, redirectUrl);
        }
    },
        
    newSession : function(verifyLoginUrl, redirectUrl) {
        var this_ = this;
        var url = otp.config.datastoreUrl+'/auth/newSession';
        $.ajax(url, {
            type: 'GET',
            dataType: 'json',
            
            success: function(data) {
                console.log("newSession success: "+data.sessionId);
                var windowUrl = verifyLoginUrl + "?session=" + data.sessionId + "&redirect=" + redirectUrl;
                console.log("redirecting to: "+windowUrl);
                window.location = windowUrl;
            },
            
            error: function(data) {
                console.log("newSession error");
                console.log(data);
            }
        });
    },
    
    checkSession : function(sessionId, checkSessionSuccessCallback) {
        var this_ = this;
        var url = otp.config.datastoreUrl+'/auth/checkSession';
        $.ajax(url, {
            type: 'GET',
            data: {
                sessionId : sessionId,
            },
            dataType: 'json',
            
            success: function(data) {
                if(data.username) {
                    this_.sessionId = sessionId;
                    this_.username = data.username;
                    this_.role = data.role;
                    checkSessionSuccessCallback.call(this);
                    //console.log('logged in as '+data.username);
                    //console.log(data);
                }
                else {
                    console.log("bad session id: " + sessionId);
                }
            },
            
            error: function(data) {
                console.log("checkSession error");
                console.log(data);
            }
        });
    },
    
});



