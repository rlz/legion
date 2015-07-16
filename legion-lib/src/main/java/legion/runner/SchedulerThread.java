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

import com.codahale.metrics.Timer;
import legion.Generator;
import legion.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public class SchedulerThread<Task> extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerThread.class);

    private final BlockingQueue<ScheduledTask<Task>> queue;
    private final Properties threadProperties;
    private final QpsScheduler scheduler;
    private final BooleanSupplier canSchedule;
    private final Runnable doneNotifier;
    private final Metrics metrics;
    private final AtomicInteger generatorsCounter;

    private Iterator<Generator<Task>> generatorsIterator;
    private Generator<Task> currentGenerator;

    public SchedulerThread(
            BlockingQueue<ScheduledTask<Task>> queue,
            List<Generator<Task>> generators,
            QpsScheduler scheduler,
            BooleanSupplier canSchedule,
            Runnable doneNotifier,
            Properties threadProperties,
            Metrics metrics,
            AtomicInteger generatorsCounter
    ) {
        this.queue = queue;
        this.threadProperties = threadProperties;
        this.generatorsIterator = generators.iterator();
        this.scheduler = scheduler;
        this.canSchedule = canSchedule;
        this.doneNotifier = doneNotifier;
        this.metrics = metrics;
        this.generatorsCounter = generatorsCounter;
    }

    @Override
    public void run() {
        try {
            if (!canSchedule.getAsBoolean()) {
                return;
            }
            ScheduledTask<Task> task = genTask();
            while (task != null) {
                try {
                    queue.put(task);
                } catch (InterruptedException e) {
                    break;
                }
                if (!canSchedule.getAsBoolean()) {
                    break;
                }
                task = genTask();
            }
        } finally {
            doneNotifier.run();
        }
    }

    ScheduledTask<Task> genTask() {
        try {
            Task task;
            while (true) {
                if (currentGenerator == null) {
                    if (generatorsIterator.hasNext()) {
                        initGenerator();
                    } else {
                        return null;
                    }
                }
                Timer.Context timerCtx = metrics.generator.time();
                task = currentGenerator.generate();
                if (task != null) {
                    timerCtx.stop();
                    return new ScheduledTask<>(task, scheduler.next());
                }
                if (generatorsIterator.hasNext()) {
                    initGenerator();
                } else {
                    currentGenerator.close();
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Generator thread ends with error", e);
            return null;
        }
    }

    void initGenerator() {
        if (currentGenerator != null) {
            currentGenerator.close();
        }
        currentGenerator = generatorsIterator.next();
        Properties p = (Properties) threadProperties.clone();
        p.setProperty("generatorIndex", Integer.toString(generatorsCounter.getAndIncrement()));
        currentGenerator.init(p, metrics.registry);
    }
}
