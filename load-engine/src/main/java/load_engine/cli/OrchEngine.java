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

package load_engine.cli;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.internal.Sets;
import com.google.common.base.Splitter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import load_engine.agent.data.JarInfo;
import load_engine.agent.data.RunInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OrchEngine {
    private final ConsoleReader reader;
    private final Set<AgentInfo> agents = Sets.newHashSet();


    public OrchEngine() throws IOException {
        reader = new ConsoleReader();
        reader.setPrompt("load-orch> ");
    }

    List<Command> commands() {
        return ImmutableList.of(
                new DelAgent(this),
                new ClearAgents(this),
                new AddAgent(this),
                new Agents(this),
                new JarsUpload(this),
                new Jars(this),
                new TestRun(this),
                new Tests(this),
                new TestStats(this)
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
        reader.addCompleter(new CommandsCompleter());
        reader.addCompleter(new JarPathCompleter());
        String line;
        while ((line = reader.readLine()) != null) {
            JCommander jc = new JCommander();
            List<Command> commands = commands();
            commands.forEach(jc::addCommand);
            try {
                jc.parse(Splitter.on(Pattern.compile("\\s+")).splitToList(line).toArray(new String[]{}));
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
                        continue;
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
            for (OrchTestInfo info: j.getValue()) {
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

        void run() throws Exception;
    }

    private static class JarPathCompleter implements Completer {
        Pattern pattern = Pattern.compile("^jar-upload\\s+-jar\\s+(.*)");

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            if (buffer.length() > cursor) {
                return -1;
            }

            Matcher matcher = pattern.matcher(buffer);

            if (!matcher.matches()) {
                return -1;
            }

            String prefix = matcher.group(1);
            if (prefix.equals("..")) {
                candidates.add("/");
                return cursor;
            }

            File dir;
            int index;
            String partialName;
            if (prefix.contains("/")) {
                dir = new File(prefix.substring(0, prefix.lastIndexOf('/')));
                index = buffer.lastIndexOf('/') + 1;
                partialName = prefix.substring(prefix.lastIndexOf('/') + 1, prefix.length());
            } else {
                dir = new File(".");
                index = buffer.length() - prefix.length();
                partialName = prefix;
            }
            if (!dir.isDirectory()) {
                return 0;
            }
            for (File f : dir.listFiles()) {
                if (!f.getName().startsWith(partialName)) {
                    continue;
                }
                if (f.isDirectory()) {
                    candidates.add(f.getName() + "/");
                } else {
                    candidates.add(f.getName());
                }
            }
            return index;
        }
    }

    private class CommandsCompleter implements Completer {
        final List<String> commands = Lists.newArrayList();

        public CommandsCompleter() {
            commands().forEach(c -> commands.add(c.getName()));
        }

        @Override
        public int complete(String buffer, int cursor, List<CharSequence> candidates) {
            if (buffer.length() > cursor) {
                return -1;
            }
            if (buffer.contains(" ")) {
                return -1;
            }
            commands.stream().filter(c -> c.startsWith(buffer)).forEach(candidates::add);
            return 0;
        }
    }
}
