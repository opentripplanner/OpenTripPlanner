/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.scripting.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.bsf.BSFException;
import org.apache.bsf.BSFManager;
import org.opentripplanner.scripting.api.OtpsEntryPoint;
import org.opentripplanner.standalone.OTPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class BSFOTPScript implements OTPScript {

    private static final Logger LOG = LoggerFactory.getLogger(BSFOTPScript.class);

    private OTPServer otpServer;

    private String scriptContent, scriptPath, scriptLang;

    public BSFOTPScript(OTPServer otpServer, File scriptFile) throws BSFException, IOException {
        this.otpServer = otpServer;
        this.scriptPath = scriptFile.getAbsolutePath();
        // Method below will throw a BSFException if the langage is not found
        this.scriptLang = BSFManager.getLangFromFilename(scriptFile.getAbsolutePath());
        // Read the script content
        byte[] encoded = Files.readAllBytes(Paths.get(scriptFile.getAbsolutePath()));
        scriptContent = new String(encoded, StandardCharsets.UTF_8);
    }

    public BSFOTPScript(OTPServer otpServer, String filename, String content) throws BSFException,
            IOException {
        this.otpServer = otpServer;
        this.scriptPath = filename;
        // Method below will throw a BSFException if the langage is not found
        this.scriptLang = BSFManager.getLangFromFilename(filename);
        this.scriptContent = content;
    }

    @Override
    public Object run() throws BSFException {
        LOG.info("Running script {}", scriptPath);

        BSFManager manager = new BSFManager();
        OtpsEntryPoint otpsEntryPoint = new OtpsEntryPoint(otpServer);

        manager.declareBean("otp", otpsEntryPoint, OtpsEntryPoint.class);
        manager.exec(scriptLang, scriptPath, 0, 0, scriptContent);

        LOG.info("Script {} done", scriptPath);
        return otpsEntryPoint.getRetval();
    }
}
