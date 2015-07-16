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

import com.beust.jcommander.Parameters;
import legion.tool.cli.AgentInfo;
import legion.tool.cli.OrchEngine;
import legion.tool.cli.OrchJarInfo;

import java.util.Map;

@Parameters(commandNames = "jars")
public class Jars implements OrchEngine.Command {
    private OrchEngine orchEngine;

    public Jars(OrchEngine orchEngine) {
        this.orchEngine = orchEngine;
    }

    @Override
    public void run() throws Exception {
        Map<String, OrchJarInfo> jars = orchEngine.collectJars();
        for (OrchJarInfo info : jars.values()) {
            System.out.printf("= %s ==\n", info.info.jarId);
            System.out.println("  Tests:");
            for (String test : info.info.tests) {
                System.out.printf("    %s\n", test);
            }
            System.out.println("  Agents:");
            for (AgentInfo agent : info.agents) {
                System.out.printf("    %s:%s\n", agent.host, agent.port);
            }
        }
    }
}
