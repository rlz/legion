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

package load_engine.agent;

import com.beust.jcommander.internal.Sets;
import com.sun.net.httpserver.HttpExchange;
import load_engine.agent.data.JarInfo;
import load_engine.agent.data.JarsList;
import load_engine.runner.JarRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

class JarsHandler implements JsonHttpHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarsHandler.class);
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
    private static final Set<String> jars = Sets.newHashSet();

    public static Set<String> getJars() {
        return jars;
    }

    private static String bytes2String(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_DIGITS[v >>> 4];
            hexChars[j * 2 + 1] = HEX_DIGITS[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static JarInfo jarInfo(String jarId) {
        JarInfo info = new JarInfo();
        info.jarId = jarId;
        info.tests.addAll(new JarRunner(new File(jarId + ".jar"), null).listTests().keySet());
        return info;
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String path = httpExchange.getRequestURI().getPath();
        String method = httpExchange.getRequestMethod();

        try {
            if (path.equals("/jars")) {
                if (method.equals("PUT")) {
                    saveJar(httpExchange);
                }

                if (method.equals("GET")) {
                    listJars(httpExchange);
                }
            } else if (path.matches("/jars/[0-9a-f]{32}")) {
                if (method.equals("GET")) {
                    jarInfo(httpExchange);
                }
            } else {
                httpExchange.sendResponseHeaders(400, 0);
                httpExchange.getResponseBody().write("{\"error\":\"Wrong path or method\"}".getBytes());
            }
        } catch (Exception e) {
            // todo: format exeptions as JSON
            LOGGER.error("Internal error", e);
        } finally {
            httpExchange.close();
        }
    }

    private void saveJar(HttpExchange exchange) throws IOException {
        File tempFile = File.createTempFile("load-agent-", ".jar");
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can't get MD5 digest", e);
            throw new RuntimeException(e);
        }
        InputStream jarBody = exchange.getRequestBody();
        OutputStream tempJar = new FileOutputStream(tempFile);
        byte[] buf = new byte[65536];
        int bytesRead;
        while ((bytesRead = jarBody.read(buf)) >= 0) {
            digest.update(buf, 0, bytesRead);
            tempJar.write(buf, 0, bytesRead);
        }
        tempJar.close();
        String jarId = bytes2String(digest.digest());
        File jarFile = new File(jarId + ".jar");
        Files.copy(tempFile.toPath(), jarFile.toPath());
        tempFile.delete();
        jarFile.deleteOnExit();
        jars.add(jarId);
        JarInfo info = jarInfo(jarId);
        writeObject(info, exchange);
    }

    private void listJars(HttpExchange httpExchange) throws IOException {
        JarsList jarsList = new JarsList();
        for (String jar : jars) {
            JarInfo info = jarInfo(jar);
            jarsList.jars.add(info);
        }
        writeObject(jarsList, httpExchange);
    }

    private void jarInfo(HttpExchange httpExchange) throws IOException {
        String jarId = httpExchange.getRequestURI().getPath().substring(6);
        if (!jars.contains(jarId)) {
            httpExchange.sendResponseHeaders(404, -1);
            return;
        }
        JarInfo info = jarInfo(jarId);
        writeObject(info, httpExchange);
    }


}

