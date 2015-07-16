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

package legion.tool.agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;

public class AgentServer {
    private final HttpServer server;
    private final Thread thread = new RunThread();

    public AgentServer(String host, int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(host, port), -1);
        server.createContext("/", new RootHandler());
        server.createContext("/jars", new JarsHandler());
        server.createContext("/runs", new RunsHandler());
        server.createContext("/exit", new ExitHandler());
    }

    public void start() {
        thread.start();
    }

    public void waitForCompletion() throws InterruptedException {
        thread.join();
    }

    private static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(200, 0);
            OutputStreamWriter writer = new OutputStreamWriter(httpExchange.getResponseBody(), "UTF8");
            writer.write("0.0.1\n");
            writer.close();
            httpExchange.close();
        }
    }

    private class ExitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            thread.interrupt();
            httpExchange.sendResponseHeaders(204, -1);
            httpExchange.close();
        }
    }

    private class RunThread extends Thread {
        @Override
        public void run() {
            server.start();
            while (true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    server.stop(3);
                    return;
                }
            }
        }
    }
}

