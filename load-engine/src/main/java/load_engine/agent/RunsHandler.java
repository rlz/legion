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

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import com.sun.net.httpserver.HttpExchange;
import load_engine.Metrics;
import load_engine.agent.data.RunInfo;
import load_engine.agent.data.RunRequest;
import load_engine.agent.data.RunStats;
import load_engine.agent.data.RunsList;
import load_engine.runner.JarRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

public class RunsHandler implements JsonHttpHandler {
    private static final Map<String, JarRunner> runs = Maps.newConcurrentMap();

    private static void fillRunInfo(String runId, JarRunner runner, RunInfo info) {
        info.runId = runId;
        info.jarId = runner.getJarFile().getName().substring(0, 32);
        info.testName = runner.getTestName();
        info.isRunning = runner.isRunning();
        info.durationLimit = runner.getMaxDuration();
        info.queriesLimit = runner.getQueriesLimit();
        info.qpsLimit = runner.getQpsLimit();
    }

    private static void fillRunStats(String runId, JarRunner runner, RunStats stats) {
        fillRunInfo(runId, runner, stats);
        Metrics metrics = runner.getMetrics();
        stats.duration = metrics.duration.getValue();
        stats.startDate = metrics.startDate.getValue();
        RunStats.addTimerValues(metrics.generator, stats.generator);
        RunStats.addTimerValues(metrics.queries, stats.queries);
        RunStats.addMeterValues(metrics.exceptions, stats.exceptions);
        RunStats.addMeterValues(metrics.success, stats.success);
    }

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        String path = httpExchange.getRequestURI().getPath();
        String method = httpExchange.getRequestMethod();

        try {
            if (path.equals("/runs")) {
                if (method.equals("GET")) {
                    listRuns(httpExchange);
                } else if (method.equals("POST")) {
                    runTest(httpExchange);
                }
            } else if (path.matches("/runs/[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
                if (method.equals("GET")) {
                    runInfo(httpExchange);
                } else if (method.equals("POST")) {
                    interruptRun(httpExchange);
                }
            } else {
                httpExchange.sendResponseHeaders(400, 0);
                httpExchange.getResponseBody().write("{\"error\":\"Wrong path or method\"}".getBytes());
            }
        } finally {
            httpExchange.close();
        }
    }

    private void listRuns(HttpExchange httpExchange) throws IOException {
        RunsList runsList = new RunsList();
        for (Map.Entry<String, JarRunner> run : runs.entrySet()) {
            RunInfo info = new RunInfo();
            fillRunInfo(run.getKey(), run.getValue(), info);
            runsList.runs.add(info);
        }
        writeObject(runsList, httpExchange);
    }

    private void runTest(HttpExchange httpExchange) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
        RunRequest request = GSON.fromJson(reader, RunRequest.class);
        if (runs.containsKey(request.runId)) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.getResponseBody().write("{\"error\":\"Run ID is not unique\"}".getBytes());
            return;
        }
        if (!JarsHandler.getJars().contains(request.jarId)) {
            httpExchange.sendResponseHeaders(400, 0);
            httpExchange.getResponseBody().write("{\"error\":\"Jar not found\"}".getBytes());
            return;
        }
        JarRunner jarRunner = new JarRunner(new File(request.jarId + ".jar"), new MetricRegistry());
        jarRunner.start(request.durationLimit, request.queriesLimit, request.qpsLimit, request.testName);
        runs.put(request.runId, jarRunner);
        RunInfo info = new RunInfo();
        fillRunInfo(request.runId, jarRunner, info);
        writeObject(info, httpExchange);
    }

    private void runInfo(HttpExchange httpExchange) throws IOException {
        String runId = httpExchange.getRequestURI().getPath().substring(6);
        JarRunner jarRunner = runs.get(runId);
        if (jarRunner == null) {
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.getResponseBody().write("{\"error\":\"Run not found\"}".getBytes());
            return;
        }
        RunStats stats = new RunStats();
        fillRunStats(runId, jarRunner, stats);
        writeObject(stats, httpExchange);
    }

    private void interruptRun(HttpExchange httpExchange) throws IOException {
        String runId = httpExchange.getRequestURI().getPath().substring(6, 37);
        JarRunner jarRunner = runs.get(runId);
        if (jarRunner == null) {
            httpExchange.sendResponseHeaders(404, 0);
            httpExchange.getResponseBody().write("{\"error\":\"Run not found\"}".getBytes());
            return;
        }
        jarRunner.interrupt();
        RunStats stats = new RunStats();
        fillRunStats(runId, jarRunner, stats);
        writeObject(stats, httpExchange);
    }
}
