/*
 * Copyright (c) 2015, IponWeb (http://www.iponweb.com)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package legion.tool.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import legion.tool.agent.data.JarInfo;
import legion.tool.agent.data.RunInfo;
import legion.tool.cli.commands.*;
import legion.tool.cli.completers.CommandsCompleter;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.completer.NullCompleter;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class OrchEngine {
    private final LineReader reader;
    private final Set<AgentInfo> agents = Sets.newHashSet();


    public OrchEngine() throws IOException {
        reader = LineReaderBuilder.builder()
                .appName("Legion Orch")
                .terminal(TerminalBuilder.terminal())
                .completer(new CommandsCompleter(this))
                .build();
        reader.setOpt(LineReader.Option.AUTO_FRESH_LINE);
    }

    public List<Command> commands() {
        return ImmutableList.of(
                new DelAgent(this),
                new ClearAgents(this),
                new AddAgent(this),
                new Agents(this),
                new JarsUpload(this),
                new Jars(this),
                new TestRun(this),
                new Tests(this),
                new TestStats(this),
                new Help(this)
        );
    }

    public Set<AgentInfo> getAgents() {
        return new HashSet<>(agents);
    }

    public void addAgent(AgentInfo agent) {
        agents.add(agent);
    }

    public void delAgent(AgentInfo agent) {
        agents.remove(agent);
    }

    public void clearAgents() {
        agents.clear();
    }

    public void run() throws IOException {
//        reader.addCompleter(new CommandsCompleter(this));
//        reader.addCompleter(new JarPathCompleter());
        String line = null;
        while (true) {
            try {
                line = reader.readLine("legion-orch> ");
            } catch (EndOfFileException e) {
                break;
            }

            if (line.isBlank()) {
                continue;
            }

            JCommander jc = new JCommander();
            List<Command> commands = commands();
            commands.forEach(jc::addCommand);
            try {
                jc.parse(Splitter.on(Pattern.compile("\\s+")).splitToList(line.trim()).toArray(new String[]{}));
            } catch (RuntimeException e) {
                e.printStackTrace();
                continue;
            }
            String cmdName = jc.getParsedCommand();
            for (Command cmd : commands) {
                if (cmd.getName().equals(cmdName)) {
                    try {
                        cmd.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                        // continue
                    }
                }
            }
        }
    }

    public Map<String, OrchJarInfo> collectJars() throws IOException {
        SetMultimap<JarInfo, AgentInfo> jars = HashMultimap.create();
        for (AgentInfo agent : agents) {
            for (JarInfo info : agent.client().listJars()) {
                jars.put(info, agent);
            }
        }
        Map<String, OrchJarInfo> result = Maps.newHashMap();
        for (Map.Entry<JarInfo, Collection<AgentInfo>> j : jars.asMap().entrySet()) {
            result.put(j.getKey().jarId, new OrchJarInfo(j.getKey(), ImmutableList.copyOf(j.getValue())));
        }
        return result;
    }

    public Map<String, OrchTestInfo> collectTests() throws IOException {
        SetMultimap<RunInfo, OrchTestInfo> runs = HashMultimap.create();
        for (AgentInfo agent : agents) {
            for (RunInfo info : agent.client().listRuns()) {
                runs.put(info, new OrchTestInfo(info, false, ImmutableList.of(agent)));
            }
        }
        Map<String, OrchTestInfo> result = Maps.newHashMap();
        for (Map.Entry<RunInfo, Collection<OrchTestInfo>> j : runs.asMap().entrySet()) {
            boolean isRunning = false;
            List<AgentInfo> agents = Lists.newArrayList();
            for (OrchTestInfo info : j.getValue()) {
                if (info.runInfo.isRunning) {
                    isRunning = true;
                }
                agents.add(info.agents.get(0));
            }
            result.put(j.getKey().runId, new OrchTestInfo(j.getKey(), isRunning, agents));
        }
        return result;
    }

    public interface Command {
        default String getName() {
            return this.getClass().getAnnotation(Parameters.class).commandNames()[0];
        }

        default String getDisplayName() {
            return getName();
        }

        default String getDescr() {
            return null;
        }

        void run() throws Exception;

        default Completer completer() {
            return new NullCompleter();
        }
    }
}
