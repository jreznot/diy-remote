/*
 * Copyright (c) 2013, DIY-Remote Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the StrangeWay.org nor the names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.strangeway.diyremote.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.strangeway.diyremote.server.sys.ClasspathWebAppContext;
import org.strangeway.diyremote.server.sys.ClasspathWebXmlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Yuriy Artamonov
 */
public class RemoteServer {

    private static final List<String> publishedResources = Arrays.asList(
            "/web.xml",
            "/diy-remote-spring.xml",
            "/diy-remote-web-spring.xml"
    );

    public static void main(String[] args) {
        File appDir = new File(System.getProperty("user.home"), ".diy-remote");
        if (!appDir.exists()) {
            boolean success = appDir.mkdirs();
            if (!success) {
                System.out.print("Could not create .diy-remote directory");
                return;
            }
        }

        // todo move port to CLI parameters
        Server server = new Server(8080);

        WebAppContext context = new ClasspathWebAppContext(new HashSet<String>(publishedResources));

        context.setConfigurations(new Configuration[]{new ClasspathWebXmlConfiguration()});
        context.setDescriptor("web.xml");
        context.setResourceBase(".");
        context.setContextPath("/");
        context.setParentLoaderPriority(true);
        context.setClassLoader(Thread.currentThread().getContextClassLoader());

        server.setHandler(context);

        try {
            server.start();
            server.join();
        } catch (Exception e) {
            if (!(e instanceof InterruptedException)) {
                System.out.print("Server stopped\n" + e.getMessage());
                e.printStackTrace(System.out);
            }
        }
    }
}