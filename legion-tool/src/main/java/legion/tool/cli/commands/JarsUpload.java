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
import legion.tool.agent.AgentClient;
import legion.tool.agent.data.JarInfo;
import legion.tool.cli.AgentInfo;
import legion.tool.cli.OrchEngine;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.ArgumentCompleter;
import org.jline.reader.impl.completer.FileNameCompleter;
import org.jline.reader.impl.completer.StringsCompleter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

@Parameters(commandNames = "jar-upload")
public class JarsUpload implements OrchEngine.Command {
    @Parameter(names = "-jar")
    File jar;
    private OrchEngine orchEngine;

    public JarsUpload(OrchEngine orchEngine) {
        this.orchEngine = orchEngine;
    }

    @Override
    public void run() throws Exception {
        String jarId = null;
        if (orchEngine.getAgents().isEmpty()) {
            System.out.println("Can't upload JAR: no agent defined");
            return;
        }
        System.out.println("Upload:");
        for (AgentInfo agent : orchEngine.getAgents()) {
            JarInfo info = new AgentClient(agent.host, agent.port).uploadJar(jar);
            jarId = info.jarId;
            System.out.printf("  - %s\n", agent);
        }
        if (jarId != null) {
            System.out.printf("Jar ID: %s\n", jarId);
        }
    }

    public Completer completer() {
        return new ArgumentCompleter(
                new StringsCompleter("jar-upload"),
                new StringsCompleter("-jar"),
                new FileNameCompleter() {
                    @Override
                    protected boolean accept(Path path) {
                        return Files.isDirectory(path) || Files.isReadable(path) && path.toString().endsWith(".jar");
                    }
                }
        );
    }
}
