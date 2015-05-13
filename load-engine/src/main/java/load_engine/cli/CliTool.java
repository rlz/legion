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
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.common.collect.ImmutableList;
import load_engine.agent.AgentServer;
import load_engine.runner.JarRunner;

import java.io.File;
import java.io.IOException;

public class CliTool {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.getProperties().setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        Common common = new Common();
        JCommander jc = new JCommander(common);
        AgentCommand agent = new AgentCommand();
        ListTestsCommand listTests = new ListTestsCommand();
        OrchCommand orch = new OrchCommand();
        ImmutableList.of(agent, listTests, orch).forEach(jc::addCommand);
        jc.parse(args);
        if (common.help) {
            jc.usage();
            System.exit(0);
        }
        String cmd = jc.getParsedCommand();
        switch (cmd) {
            case "agent": {
                if (agent.debug) {
                    System.getProperties().setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                } else {
                    System.getProperties().setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
                }
                AgentServer server = new AgentServer(agent.host, agent.port);
                server.start();
                server.waitForCompletion();
                break;
            }
            case "list-tests": {
                JarRunner runner = new JarRunner(new File(listTests.jar), null);
                runner.listTests().keySet().forEach(System.out::println);
                break;
            }
            case "orch": {
                new OrchEngine().run();
                break;
            }
        }
    }

    private static class Common {
        @Parameter(names = {"-h", "-help", "--help"}, help = true)
        boolean help = false;
    }

    @Parameters(commandNames = "agent")
    private static class AgentCommand {
        @Parameter(names = "-host")
        String host = "0.0.0.0";

        @Parameter(names = "-port")
        int port = 3500;

        @Parameter(names = "-debug")
        boolean debug = false;
    }

    @Parameters(commandNames = "list-tests")
    private static class ListTestsCommand {
        @Parameter(names = "-jar", required = true)
        String jar;
    }

    @Parameters(commandNames = "orch")
    private static class OrchCommand {

    }
}
