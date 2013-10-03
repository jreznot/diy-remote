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

package org.strangeway.diyremote.server.executors;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.apache.commons.lang3.StringUtils;
import org.strangeway.diyremote.Action;
import org.strangeway.diyremote.Command;
import org.strangeway.diyremote.Result;
import org.strangeway.diyremote.server.CommandExecutor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Yuriy Artamonov
 */
public class GroovyCommandExecutor implements CommandExecutor {

    public static final Binding EMPTY_BINDING = new Binding();

    private Map<Action, Closure<String>> actions = new LinkedHashMap<Action, Closure<String>>();
    private Closure<String> statusProvider;

    public GroovyCommandExecutor() {
        // load actions from app/scripts directory
        File appDir = new File(System.getProperty("user.home"), ".diy-remote");
        File scriptsDir = new File(appDir, "scripts");
        if (scriptsDir.exists() && scriptsDir.isDirectory()) {
            File[] files = scriptsDir.listFiles();
            if (files != null) {
                loadActions(scriptsDir, files);
            }
        }
    }

    private void loadActions(File scriptsDir, File[] files) {
        GroovyScriptEngine scriptEngine;
        try {
            scriptEngine = new GroovyScriptEngine(scriptsDir.getAbsolutePath());
        } catch (IOException e) {
            System.out.print("Unable to initialize groovy script engine\n" + e.getMessage());
            e.printStackTrace(System.out);
            return;
        }

        List<ActionContainer> actionsList = new ArrayList<ActionContainer>();
        for (File file : files) {
            if ("status.groovy".equals(file.getName())) {
                ActionContainer ac = loadAction(scriptEngine, file);
                if (ac != null) {
                    statusProvider = ac.closure;
                }
            } else if (file.getName().endsWith(".groovy")) {
                ActionContainer ac = loadAction(scriptEngine, file);
                if (ac != null) {
                    actionsList.add(ac);
                }
            }
        }

        Collections.sort(actionsList, new Comparator<ActionContainer>() {
            @Override
            public int compare(ActionContainer o1, ActionContainer o2) {
                if (o1.order == o2.order)
                    return 0;
                if (o1.order > o2.order)
                    return 1;
                return -1;
            }
        });

        for (ActionContainer ac : actionsList) {
            actions.put(ac.action, ac.closure);
        }
    }

    private ActionContainer loadAction(GroovyScriptEngine scriptEngine, File file) {
        // load action
        try {
            Object actionDefinition = scriptEngine.run(file.getName(), EMPTY_BINDING);
            if (actionDefinition instanceof Map) {
                //noinspection unchecked
                Map<String, Object> definitionMap = (Map<String, Object>) actionDefinition;
                ActionContainer ac = new ActionContainer();

                Action a = new Action();
                a.name = (String) definitionMap.get("name");
                a.description = (String) definitionMap.get("description");
                a.icon = (String) definitionMap.get("icon");

                ac.action = a;

                ac.order = (Integer) definitionMap.get("order");
                //noinspection unchecked
                ac.closure = (Closure<String>) definitionMap.get("action");

                return ac;
            } else {
                System.out.print("Incorrect action definition: " + file.getName());
            }
        } catch (ResourceException e) {
            System.out.print("Unable to load action " + file.getName());
            e.printStackTrace(System.out);
        } catch (ScriptException e) {
            System.out.print("Unable to load action " + file.getName());
            e.printStackTrace(System.out);
        }
        return null;
    }

    @Override
    public Result getStatus() {
        Result result = new Result();
        result.message = statusProvider.call();
        return result;
    }

    @Override
    public List<Action> getActions() {
        return new ArrayList<Action>(actions.keySet());
    }

    @Override
    public Result execute(Command command) {
        for (Action a : actions.keySet()) {
            if (StringUtils.equals(a.name, command.actionId)) {
                Result r = new Result();
                r.message = actions.get(a).call();
                return r;
            }
        }

        return new Result();
    }

    private static class ActionContainer {

        public Action action;

        public int order = 0;

        public Closure<String> closure;
    }
}