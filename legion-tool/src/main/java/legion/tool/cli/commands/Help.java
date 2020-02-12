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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import legion.tool.cli.OrchEngine;
import org.jline.reader.Completer;
import org.jline.reader.impl.completer.StringsCompleter;

import java.util.List;
import java.util.stream.Collectors;

@Parameters(commandNames = "help")
public class Help implements OrchEngine.Command {
    @Parameter
    List<String> command;

    private OrchEngine orchEngine;

    public Help(OrchEngine orchEngine) {
        this.orchEngine = orchEngine;
    }

    @Override
    public void run() {
        if (command == null) {
            System.out.println("Run `help <command>` for help.");
            System.out.println("\nPossible commands:\n");
            for (OrchEngine.Command cmd : orchEngine.commands()) {
                System.out.printf("\t* %s\n", cmd.getName());
            }
            System.out.println();
        } else {
            if (command.size() > 1) {
                System.out.println("Only one command can be specified.");
            }
            for (OrchEngine.Command cmd : orchEngine.commands()) {
                if (cmd.getName().equals(command.get(0))) {
                    JCommander jc = new JCommander(cmd);
                    jc.setProgramName(cmd.getName());
                    jc.usage();
                    return;
                }
            }
            System.out.println("Unknown command. Try to run `help`.");
        }
    }

    @Override
    public String getDisplayName() {
        return "help [command]";
    }

    @Override
    public String getDescr() {
        return "Display commands help";
    }

    public Completer completer() {
        var commands = orchEngine.commands().stream().map(OrchEngine.Command::getName).collect(Collectors.toUnmodifiableList());
        return new StringsCompleter(commands);
    }
}
