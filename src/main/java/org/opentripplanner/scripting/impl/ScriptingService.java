package org.opentripplanner.scripting.impl;

import java.io.File;

import org.opentripplanner.standalone.OTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ScriptingService {

    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(ScriptingService.class);

    private OTPServer otpServer;

    public boolean enableScriptingWebService;

    public ScriptingService(OTPServer otpServer) {
        this.otpServer = otpServer;
    }

    public Object runScript(File scriptFile) throws Exception {
        BSFOTPScript script = new BSFOTPScript(otpServer, scriptFile);
        return script.run();
    }

    public Object runScript(String filename, String scriptContent) throws Exception {
        BSFOTPScript script = new BSFOTPScript(otpServer, filename, scriptContent);
        return script.run();
    }

}
