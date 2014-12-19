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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    private File scriptFile;

    public BSFOTPScript(OTPServer otpServer, File scriptFile) {
        this.otpServer = otpServer;
        this.scriptFile = scriptFile;
    }

    @Override
    public void run() throws Exception {
        LOG.info("Running script {}", scriptFile);

        BSFManager manager = new BSFManager();
        // Method below will throw a BSFException if the langage is not found
        String lang = BSFManager.getLangFromFilename(scriptFile.getAbsolutePath());

        // TODO Create entry point
        manager.declareBean("otp", new OtpsEntryPoint(otpServer), OtpsEntryPoint.class);

        byte[] encoded = Files.readAllBytes(Paths.get(scriptFile.getAbsolutePath()));
        String scriptContent = new String(encoded, StandardCharsets.UTF_8);

        manager.exec(lang, scriptFile.getAbsolutePath(), 0, 0, scriptContent);

        LOG.info("Script {} done", scriptFile);
    }
}
