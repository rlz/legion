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

package legion.runner;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;
import legion.Generator;
import legion.Loader;
import legion.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public class LoadGenerator<Task> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadGenerator.class);
    private static final BooleanSupplier UNLIMITED_QUERIES = () -> true;

    private final int maxDuration;
    private final int queriesLimit;
    private final int qpsLimit;
    private final int generatorThreadsLimit;
    private final Metrics metrics;
    private MainThread mainThread;

    public LoadGenerator(
            int maxDuration,
            int queriesLimit,
            int qpsLimit,
            int generatorThreadsLimit,
            MetricRegistry registry
    ) {
        this(maxDuration, queriesLimit, qpsLimit, generatorThreadsLimit, new Metrics(registry));
    }

    public LoadGenerator(
            int maxDuration,
            int queriesLimit,
            int qpsLimit,
            int generatorThreadsLimit,
            Metrics metrics
    ) {
        this.maxDuration = maxDuration;
        this.queriesLimit = queriesLimit;
        this.qpsLimit = qpsLimit;
        this.generatorThreadsLimit = generatorThreadsLimit;
        this.metrics = metrics;
    }

    private static void waitForFinish(Iterable<? extends Thread> threads) {
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public int getQueriesLimit() {
        return queriesLimit;
    }

    public int getQpsLimit() {
        return qpsLimit;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void start(Collection<? extends Generator<Task>> generators, Collection<? extends Loader<Task>> loaders, Properties props) {
        if (mainThread != null) {
            throw new IllegalStateException("Load generator can't be executed twice");
        }
        mainThread = new MainThread(generators, loaders, props);
        mainThread.start();
    }

    public boolean isRunning() {
        return mainThread != null && mainThread.isAlive();
    }

    public void join() throws InterruptedException {
        if (!isRunning()) {
            throw new IllegalStateException("LoadGenerator is not running");
        }
        mainThread.join();
    }

    public void interrupt() {
        if (!isRunning()) {
            throw new IllegalStateException("LoadGenerator is not running");
        }
        mainThread.interrupt();
    }

    public void doTest(
            Collection<? extends Generator<Task>> generators,
            Collection<? extends Loader<Task>> loaders,
            Properties props
    ) throws InterruptedException {
        this.start(generators, loaders, props);
        this.join();
    }

    private class MainThread extends Thread {
        private final List<SchedulerThread<Task>> schedulers = new ArrayList<>();
        private final List<LoadThread<Task>> loadThreads = new ArrayList<>();
        private final BlockingQueue<ScheduledTask<Task>> queue = new ArrayBlockingQueue<>(10000);

        public MainThread(
                Collection<? extends Generator<Task>> generators,
                Collection<? extends Loader<Task>> loaders,
                Properties props
        ) {
            props = (Properties) props.clone();
            props.setProperty("generatorThreads", Integer.toString(Integer.min(generatorThreadsLimit, generators.size())));
            props.setProperty("generators", Integer.toString(generators.size()));
            props.setProperty("loaders", Integer.toString(loaders.size()));
            QpsScheduler scheduler = new QpsScheduler(qpsLimit);
            BooleanSupplier canSchedule = UNLIMITED_QUERIES;
            if (queriesLimit > 0) {
                canSchedule = new QueriesLimit(queriesLimit);
            }

            int loaderIndex = 0;
            for (Loader<Task> l : loaders) {
                Properties loaderProps = (Properties) props.clone();
                loaderProps.setProperty("loaderIndex", Integer.toString(loaderIndex++));
                l.init(loaderProps, metrics.registry);
                LoadThread<Task> thread = new LoadThread<>(queue, l, metrics);
                thread.setName("LoadThread-" + loaderIndex);
                loadThreads.add(thread);
            }

            LoadThreadsFinalizer<Task> loadThreadsFinalizer = new LoadThreadsFinalizer<>(generators.size(), queue);

            int generatorThreadIndex = 0;
            AtomicInteger generatorsCounter = new AtomicInteger();
            for (List<Generator<Task>> g : distGeneratorsByThreads(generators, generatorThreadsLimit)) {
                Properties generatorProps = (Properties) props.clone();
                generatorProps.setProperty("generatorThreadIndex", Integer.toString(generatorThreadIndex++));
                SchedulerThread<Task> thread = new SchedulerThread<>(
                        queue,
                        g,
                        scheduler,
                        canSchedule,
                        loadThreadsFinalizer,
                        generatorProps,
                        metrics,
                        generatorsCounter
                );
                thread.setName("GeneratorThread-" + generatorThreadIndex);
                schedulers.add(thread);
            }
        }

        private List<List<Generator<Task>>> distGeneratorsByThreads(Collection<? extends Generator> generators, int threads) {
            int partitions = Integer.min(threads, generators.size());
            List<List<Generator<Task>>> result = Lists.newArrayListWithCapacity(partitions);
            for (int i = 0; i < partitions; ++i) {
                result.add(Lists.newArrayList());
            }
            int index = 0;
            for (Generator<Task> g : generators) {
                result.get(index++).add(g);
                if (index == result.size()) {
                    index = 0;
                }
            }
            return result;
        }

        @Override
        public void run() {
            metrics.markStart();

            loadThreads.forEach(Thread::start);
            schedulers.forEach(Thread::start);

            ScheduledExecutorService executorService = null;
            if (maxDuration > 0) {
                executorService = new ScheduledThreadPoolExecutor(1);
                executorService.schedule(
                        this::interrupt,
                        maxDuration,
                        TimeUnit.SECONDS
                );
                executorService.shutdown();
            }

            waitForFinish(schedulers);
            LOGGER.trace("All scheduler exited");
            waitForFinish(loadThreads);
            LOGGER.trace("All loader exited");

            if (executorService != null) {
                executorService.shutdownNow();
            }

            metrics.markEnd();
        }

        @Override
        public void interrupt() {
            LOGGER.trace("About to interrupt schedulers");
            schedulers.forEach(Thread::interrupt);
            LOGGER.trace("Waiting schedulers to exit");
            waitForFinish(schedulers);
            LOGGER.trace("All schedulers exited - clear queue");
            queue.clear();
            ScheduledTask.addFinalizer(queue);
        }
    }
}
