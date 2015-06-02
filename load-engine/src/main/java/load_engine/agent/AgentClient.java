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

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import load_engine.agent.data.*;
import load_engine.agent.data.stats.RunStats;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class AgentClient {
    private static final Gson GSON = new Gson();

    private final String host;
    private final int port;

    public AgentClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public String version() throws IOException {
        URLConnection conn = connect("/");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        return reader.readLine();
    }

    public JarInfo uploadJar(File jar) throws IOException {
        HttpURLConnection conn = connect("/jars");
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Length", Long.toString(jar.length()));
        conn.setDoOutput(true);
        conn.connect();
        OutputStream out = conn.getOutputStream();
        InputStream in = new FileInputStream(jar);
        ByteStreams.copy(in, out);
        out.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        return GSON.fromJson(reader, JarInfo.class);
    }

    public List<JarInfo> listJars() throws IOException {
        HttpURLConnection conn = connect("/jars");
        conn.setRequestMethod("GET");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        return GSON.fromJson(reader, JarsList.class).jars;
    }

    public RunInfo run(String runId, String jarId, String testId, int durationLimit, int queriesLimit, int qpsLimit) throws IOException {
        RunRequest request = new RunRequest();
        request.runId = runId;
        request.jarId = jarId;
        request.testName = testId;
        request.durationLimit = durationLimit;
        request.queriesLimit = queriesLimit;
        request.qpsLimit = qpsLimit;
        byte[] requestBody = GSON.toJson(request).getBytes();

        HttpURLConnection conn = connect("/runs");
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", Long.toString(requestBody.length));
        conn.setDoOutput(true);
        conn.connect();
        OutputStream out = conn.getOutputStream();
        out.write(requestBody);
        out.close();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        RunInfo runInfo = GSON.fromJson(reader, RunInfo.class);
        reader.close();
        return runInfo;
    }

    public List<RunInfo> listRuns() throws IOException {
        HttpURLConnection conn = connect("/runs");
        conn.setRequestMethod("GET");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        return GSON.fromJson(reader, RunsList.class).runs;
    }

    public RunStats stats(String runId) throws IOException {
        HttpURLConnection conn = connect("/runs/" + runId);
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        RunStats stats = GSON.fromJson(reader, RunStats.class);
        reader.close();
        return stats;
    }

    public RunStats interrupt(String runId) throws IOException {
        HttpURLConnection conn = connect("/interrupt");
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", "0");
        conn.connect();
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        RunStats stats = GSON.fromJson(reader, RunStats.class);
        reader.close();
        return stats;
    }

    public void exit() throws IOException {
        HttpURLConnection conn = connect("/exit");
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Length", "0");
        conn.connect();
        long ignored = conn.getInputStream().skip(1000);
        conn.getInputStream().close();
    }

    private HttpURLConnection connect(String path) {
        try {
            URL url = new URL("http", host, port, path);
            return (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
