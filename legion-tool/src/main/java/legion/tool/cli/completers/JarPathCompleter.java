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

package legion.tool.cli.completers;

import jline.console.completer.Completer;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarPathCompleter implements Completer {
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
