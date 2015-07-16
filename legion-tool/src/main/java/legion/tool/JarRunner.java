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

package legion.tool;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Maps;
import legion.LoadTest;
import legion.Metrics;
import legion.runner.LoadGenerator;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;
import java.util.Properties;

public class JarRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(JarRunner.class);
    private static final String NOT_STARTED_MESSAGE = "Test was not started";

    private final File jarFile;
    private final URL jar;
    private final MetricRegistry registry;
    private LoadGenerator loadGenerator;
    private String testName;

    public JarRunner(File jar, MetricRegistry registry) {
        this.jarFile = jar;
        this.registry = registry;
        try {
            this.jar = jar.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public MetricRegistry getRegistry() {
        return registry;
    }

    public void start(int maxDuration, int queriesLimit, int qpsLimit, int generatorThreadsLimit, String name, Properties properties) {
        if (loadGenerator != null) {
            return;
        }
        testName = name;
        Map<String, Class<? extends LoadTest>> tests = listTests();
        Class<? extends LoadTest> test = tests.get(name);
        if (test == null) {
            LOGGER.error("Test not found: {}", name);
            throw new RuntimeException("Can't find test: " + name);
        }
        try {
            LoadTest testInstance = test.newInstance();
            testInstance.init(properties, registry);
            loadGenerator = new LoadGenerator(maxDuration, queriesLimit, qpsLimit, generatorThreadsLimit, new Metrics(registry));
            loadGenerator.start(testInstance.getGenerators(), testInstance.getLoaders(), properties);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Can't instantiate test", e);
            throw new RuntimeException(e);
        }
    }

    public String getTestName() {
        if (testName == null) {
            throw new IllegalStateException(NOT_STARTED_MESSAGE);
        }
        return testName;
    }

    public File getJarFile() {
        return jarFile;
    }

    public int getMaxDuration() {
        if (loadGenerator == null) {
            throw new IllegalStateException(NOT_STARTED_MESSAGE);
        }
        return loadGenerator.getMaxDuration();
    }

    public int getQpsLimit() {
        if (loadGenerator == null) {
            throw new IllegalStateException(NOT_STARTED_MESSAGE);
        }
        return loadGenerator.getQpsLimit();
    }

    public Metrics getMetrics() {
        if (loadGenerator == null) {
            throw new IllegalStateException(NOT_STARTED_MESSAGE);
        }
        return loadGenerator.getMetrics();
    }

    public int getQueriesLimit() {
        if (loadGenerator == null) {
            throw new IllegalStateException(NOT_STARTED_MESSAGE);
        }
        return loadGenerator.getQueriesLimit();
    }

    public void join() throws InterruptedException {
        loadGenerator.join();
    }

    public boolean isRunning() {
        return loadGenerator.isRunning();
    }

    public void interrupt() {
        loadGenerator.interrupt();
    }

    public Map<String, Class<? extends LoadTest>> listTests() {
        URLClassLoader cl = new URLClassLoader(new URL[]{this.jar}, this.getClass().getClassLoader());
        Reflections reflections = new Reflections(
                new ConfigurationBuilder()
                        .addClassLoader(cl)
                        .addScanners(new SubTypesScanner())
                        .addUrls(jar)
        );
        Map<String, Class<? extends LoadTest>> tests = Maps.newHashMap();
        LOGGER.debug("About to load classes");
        reflections.getSubTypesOf(LoadTest.class).forEach(i -> tests.put(i.getName(), i));
        LOGGER.debug("Classes are loaded");
        return tests;
    }
}
