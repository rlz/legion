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

package load_engine.runner;

import com.codahale.metrics.MetricRegistry;
import load_engine.Metrics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LoadGenerator<Task> {
    private static final BooleanSupplier UNLIMITED_QUERIES = () -> true;

    private final int maxDuration;
    private final int queriesLimit;
    private final int qpsLimit;
    private final Metrics metrics;
    private MainThread mainThread;

    public LoadGenerator(int maxDuration, int queriesLimit, int qpsLimit, MetricRegistry registry) {
        this(maxDuration, queriesLimit, qpsLimit, new Metrics(registry));
    }

    public LoadGenerator(int maxDuration, int queriesLimit, int qpsLimit, Metrics metrics) {
        this.maxDuration = maxDuration;
        this.queriesLimit = queriesLimit;
        this.qpsLimit = qpsLimit;
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

    public void start(Collection<Supplier<Task>> suppliers, Collection<Consumer<Task>> consumers) {
        if (mainThread != null) {
            throw new IllegalStateException("Load generator can't be executed twice");
        }
        mainThread = new MainThread(suppliers, consumers);
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

    public void doTest(Collection<Supplier<Task>> suppliers, Collection<Consumer<Task>> consumers) throws InterruptedException {
        this.start(suppliers, consumers);
        this.join();
    }

    private class MainThread extends Thread {
        private final List<SchedulerThread<Task>> schedulers = new ArrayList<>();
        private final List<LoadThread<Task>> loadThreads = new ArrayList<>();
        private final BlockingQueue<ScheduledTask<Task>> queue = new ArrayBlockingQueue<>(10000);

        public MainThread(Collection<Supplier<Task>> suppliers, Collection<Consumer<Task>> consumers) {
            QpsScheduler scheduler = new QpsScheduler(qpsLimit);
            BooleanSupplier canSchedule = UNLIMITED_QUERIES;
            if (queriesLimit > 0) {
                canSchedule = new QueriesLimit(queriesLimit);
            }

            for (Consumer<Task> c : consumers) {
                LoadThread<Task> thread = new LoadThread<>(queue, c, metrics);
                loadThreads.add(thread);
            }

            LoadThreadsFinalizer<Task> loadThreadsFinalizer = new LoadThreadsFinalizer<>(suppliers.size(), loadThreads);

            for (Supplier<Task> s : suppliers) {
                SchedulerThread<Task> thread = new SchedulerThread<>(
                        queue,
                        s,
                        scheduler,
                        canSchedule,
                        loadThreadsFinalizer,
                        metrics
                );
                schedulers.add(thread);
            }
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
            waitForFinish(loadThreads);

            if (executorService != null) {
                executorService.shutdownNow();
            }

            metrics.markEnd();
        }

        @Override
        public void interrupt() {
            schedulers.forEach(Thread::interrupt);
            waitForFinish(schedulers);
            queue.clear();
        }
    }
}
