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

package load_engine.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import load_engine.agent.data.RunStats;
import load_engine.cli.AgentInfo;
import load_engine.cli.OrchEngine;
import load_engine.cli.OrchJarInfo;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    @Parameter(names = "-logs")
    boolean logs = false;

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
        System.out.printf("Run test on agents: %s\n", Joiner.on(", ").join(info.agents.stream().map(a -> a.host + ":" + a.port).iterator()));
        System.out.printf("Test ID: %s\n", runId);
        for (AgentInfo agent : info.agents) {
            agent.client().run(runId, jarId, testName, durationLimit, queriesLimit, qpsLimit);
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
            logWriter = new PrintStream(new File("log")); //todo: file name shold contain date, etc
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
                    printStats(agent, iteration, stats);
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

        private void printStats(AgentInfo agent, int iteration, RunStats stats) {
            printMetric(agent, iteration, "general", "duration", stats.duration);
            printTimerStats(agent, iteration, "generator", stats.generator);
            printTimerStats(agent, iteration, "queries", stats.queries);
            printMeterStats(agent, iteration, "success", stats.success);
            printMeterStats(agent, iteration, "exceptions", stats.exceptions);
        }

        private void printTimerStats(AgentInfo agent, int iteration, String name, RunStats.TimerStats stats) {
            printMetric(agent, iteration, name, "count", stats.count);
            printMetric(agent, iteration, name, "min", stats.min);
            printMetric(agent, iteration, name, "max", stats.max);
            printMetric(agent, iteration, name, "mean", stats.mean);
            printMetric(agent, iteration, name, "median", stats.median);
            printMetric(agent, iteration, name, "stddev", stats.stddev);
            printMetric(agent, iteration, name, "percentile75", stats.percentile75);
            printMetric(agent, iteration, name, "percentile95", stats.percentile95);
            printMetric(agent, iteration, name, "percentile98", stats.percentile98);
            printMetric(agent, iteration, name, "percentile99", stats.percentile99);
            printMetric(agent, iteration, name, "percentile999", stats.percentile999);
            printMetric(agent, iteration, name, "meanRate", stats.meanRate);
            printMetric(agent, iteration, name, "oneMinuteRate", stats.oneMinuteRate);
            printMetric(agent, iteration, name, "fiveMinutesRate", stats.fiveMinutesRate);
            printMetric(agent, iteration, name, "fifteenMinutesRate", stats.fifteenMinutesRate);
        }

        private void printMeterStats(AgentInfo agent, int iteration, String name, RunStats.MeterStats stats) {
            printMetric(agent, iteration, name, "count", stats.count);
            printMetric(agent, iteration, name, "meanRate", stats.meanRate);
            printMetric(agent, iteration, name, "oneMinuteRate", stats.oneMinuteRate);
            printMetric(agent, iteration, name, "fiveMinutesRate", stats.fiveMinutesRate);
            printMetric(agent, iteration, name, "fifteenMinutesRate", stats.fifteenMinutesRate);
        }

        private void printMetric(AgentInfo agent, int iteration, String metric, String subMetric, Object value) {
            logWriter.println(
                    JOINER.join(
                            iteration,
                            new Date().toString(),
                            agent.toString(),
                            metric,
                            subMetric,
                            value
                    )
            );
        }
    }
}
