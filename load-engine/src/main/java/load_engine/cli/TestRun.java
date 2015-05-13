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

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.base.Joiner;

import java.util.UUID;

@Parameters(commandNames = "test-run")
class TestRun implements OrchEngine.Command {
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

//        if (logs) {
//            new LogThread(runId, ImmutableList.copyOf(info.agents)).start();
//        }
    }

//    private static class LogThread extends Thread {
//        final String testId;
//        final List<AgentInfo> agents;
//        final PrintStream logWriter;
//        int iteration = 1;
//
//        private LogThread(String testId, List<AgentInfo> agents) throws IOException {
//            this.testId = testId;
//            this.agents = agents;
//            logWriter = new PrintStream(new File("log")); //todo: file name shold contain date, etc
//        }
//
//        @Override
//        public void run() {
//            boolean running = true;
//            int iteration = 1;
//
//            while (running) {
//                running = false;
//                for (AgentInfo agent: agents) {
//                    RunStats stats;
//                    try {
//                        stats = agent.client().stats(testId);
//                    } catch (IOException e) {
//                        // todo : log error
//                        continue;
//                    }
//                    if (stats.isRunning) {
//                        running = true;
//                    }
//                }
//            }
//        }
//
//        private void printf(AgentInfo agent, String format, Object... args) {
//            logWriter.printf("%s %s %s %s", iteration, new Date().toString(), agent, stats.duration, stats.generator.);
//        }
//    }
}
