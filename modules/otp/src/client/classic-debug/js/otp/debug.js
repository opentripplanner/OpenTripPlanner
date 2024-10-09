otp.debug = {};

(function (self) {
    var enabled = false,
        debugWindow, debugEl;

    self.debugEl = debugEl;
    self.enable = function () {
        console.info('Debug mode enabled (persistent).');
        console.info('    To disable put debug=false in the url parameters');
        window.localStorage['otpDebug'] = 'true'; // Save in localstorage to make it persistant!
        otp.config.debug = enabled = true;

        if (enabled) {
            debugWindow = window.open('','OpenTripPlanner Debug','toolbar=yes, scrollbars=yes, height=500, width=800');
            debugWindow.document.write('<div class="debug-content"></div>');
            debugEl = $(debugWindow.document).find('.debug-content');
            debugEl.html('<div class="request-json"><h2>Request JSON</h2><pre></pre></div>');
            self.debugEl = debugEl;
        }
    };
    self.disable = function () {
        delete window.localStorage['otpDebug']; // Reset the debug value
    };

    self.processRequest = function (data) {
        console.warn('processRequest', enabled, data);
        if (enabled) {
            debugEl.find('.request-json pre').html(JSON.stringify(data, null, 4));
        }
    };
})(otp.debug);