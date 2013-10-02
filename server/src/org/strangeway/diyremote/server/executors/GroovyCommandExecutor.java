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

import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.strangeway.diyremote.Action;
import org.strangeway.diyremote.Command;
import org.strangeway.diyremote.Result;
import org.strangeway.diyremote.server.CommandExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Yuriy Artamonov
 */
public class GroovyCommandExecutor implements CommandExecutor {

    private Map<Action, Closure<String>> actions = new HashMap<Action, Closure<String>>();

    private Closure<String> statusProvider;

    public GroovyCommandExecutor() {
        // load actions from app directory
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
}