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

package legion.tool.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.internal.Lists;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import legion.tool.agent.data.stats.RunStats;
import legion.tool.agent.data.stats.StatsPrinter;
import legion.tool.cli.AgentInfo;
import legion.tool.cli.OrchEngine;
import legion.tool.cli.OrchJarInfo;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.AggregateCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Parameters(commandNames = "test-run")
public class TestRun implements OrchEngine.Command {
    @Parameter(names = "-jar")
    String jarId;

    @Parameter(names = "-test")
    String testName;

    @Parameter(names = "-duration-limit")
    int durationLimit = -1;

    @Parameter(names = "-queries-limit")
    int queriesLimit = -1;

    @Parameter(names = "-qps-limit")
    int qpsLimit = -1;

    @Parameter(names = "-gen-threads-limit")
    int generatorThreadsLimit = 5;

    @Parameter(names = "-logs")
    boolean logs = false;

    @Parameter(names = "-p", variableArity = true)
    List<String> properties = Lists.newArrayList();

    private OrchEngine orchEngine;

    public TestRun(OrchEngine orchEngine) {
        this.orchEngine = orchEngine;
    }

    @Override
    public void run() throws Exception {
        String runId = UUID.randomUUID().toString();
        OrchJarInfo info = orchEngine.collectJars().get(jarId);
        if (info == null) {
            System.out.println("Jar not found");
            return;
        }

        Properties props = new Properties();
        for (String p : properties) {
            int eqIndex = p.indexOf('=');
            if (eqIndex == -1) {
                System.out.printf("Property must be formatted as <name>=<value>. Can't parse `%s`\n", p);
                return;
            }
            String key = p.substring(0, eqIndex);
            String value = p.substring(eqIndex + 1, p.length());
            props.setProperty(key, value);
        }
        System.out.printf("Run test on agents: %s\n", Joiner.on(", ").join(info.agents.stream().map(a -> a.host + ":" + a.port).iterator()));
        System.out.printf("Test ID: %s\n", runId);

        props.setProperty("agents", Integer.toString(info.agents.size()));
        int agentIndex = 0;
        for (AgentInfo agent : info.agents) {
            Properties agentProperties = new Properties(props);
            agentProperties.setProperty("agent-index", Integer.toString(agentIndex++));
            agent.client().run(runId, jarId, testName, durationLimit, queriesLimit, qpsLimit, generatorThreadsLimit, agentProperties);
        }

        if (logs) {
            new LogThread(runId, ImmutableList.copyOf(info.agents)).start();
        }
    }

    private static class LogThread extends Thread {
        final String testId;
        final List<AgentInfo> agents;
        final PrintStream logWriter;
        final Joiner JOINER = Joiner.on('\t');

        private LogThread(String testId, List<AgentInfo> agents) throws IOException {
            this.testId = testId;
            this.agents = agents;
            logWriter = new PrintStream(new File("log")); //todo: file name should contain date, etc
        }

        @Override
        public void run() {
            boolean running = true;
            int iteration = 1;

            while (running) {
                running = false;
                for (AgentInfo agent : agents) {
                    RunStats stats;
                    try {
                        stats = agent.client().stats(testId);
                    } catch (IOException e) {
                        // todo : log error
                        continue;
                    }
                    new StatsPrinter(logWriter, TimeUnit.SECONDS, TimeUnit.MILLISECONDS) {
                        @Override
                        public void printMetric(int iteration, String metric, String subMetric, Object value) {
                            logWriter.println(
                                    JOINER.join(
                                            iteration,
                                            new Date().toString(),
                                            agent,
                                            metric,
                                            subMetric,
                                            value
                                    )
                            );
                        }
                    }.printStats(iteration, stats);
                    if (stats.isRunning) {
                        running = true;
                    }
                }
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    // ignore
                }
                iteration++;
            }
        }
    }

    public Completer completer() {
        return new AggregateCompleter(
                new StringsCompleter("-jar"),
                new StringsCompleter("-test"),
                new StringsCompleter("-duration-limit"),
                new StringsCompleter("-queries-limit"),
                new StringsCompleter("-qps-limit"),
                new StringsCompleter("-gen-threads-limit"),
                new StringsCompleter("-logs"),
                new StringsCompleter("-p")
        );
    }
}
