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
import legion.tool.agent.data.stats.RunStats;
import legion.tool.cli.AgentInfo;
import legion.tool.cli.OrchEngine;
import legion.tool.cli.OrchTestInfo;

@Parameters(commandNames = "test-stats")
public class TestStats implements OrchEngine.Command {
    @Parameter(names = "-test", required = true)
    String testId;
    private OrchEngine orchEngine;

    public TestStats(OrchEngine orchEngine) {
        this.orchEngine = orchEngine;
    }

    @Override
    public void run() throws Exception {
        OrchTestInfo test = orchEngine.collectTests().get(testId);
        if (test == null) {
            System.out.println("Test is not found");
            return;
        }
        for (AgentInfo agent : test.agents) {
            RunStats stats = agent.client().stats(testId);
            System.out.printf("= %s (start: %s, duration: %s) ==\n", agent, stats.startDate, stats.duration);
            System.out.printf(
                    "  Queries: %s (success: %s, exceptions: %s)\n",
                    stats.queries.getCount(),
                    stats.success.getCount(),
                    stats.exceptions.getCount()
            );
        }
    }
}
