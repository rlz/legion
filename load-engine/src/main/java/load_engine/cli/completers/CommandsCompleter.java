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

package load_engine.cli.completers;

import com.beust.jcommander.internal.Lists;
import jline.console.completer.Completer;
import load_engine.cli.OrchEngine;

import java.util.List;

public class CommandsCompleter implements Completer {
    final List<String> commands = Lists.newArrayList();

    public CommandsCompleter(OrchEngine orchEngine) {
        orchEngine.commands().forEach(c -> commands.add(c.getName()));
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        int startIndex = 0;
        String commandBuffer = buffer;
        if (buffer.startsWith("help ")) {
            startIndex = 5;
            commandBuffer = buffer.substring(5);
        }
        if (buffer.length() > cursor) {
            return -1;
        }
        if (commandBuffer.contains(" ")) {
            return -1;
        }
        final String finalCommandBuffer = commandBuffer;
        commands.stream().filter(c -> c.startsWith(finalCommandBuffer)).forEach(i -> candidates.add(i + " "));
        return startIndex;
    }
}
