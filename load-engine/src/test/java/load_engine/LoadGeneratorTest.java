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

package load_engine;

import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import load_engine.runner.LoadGenerator;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoadGeneratorTest {
    private MetricRegistry metricRegistry;

    private static void sleep(int millis) {
        boolean interrupted = false;
        long startTime = System.nanoTime();
        long sleepTime = (startTime + millis * 1000000 - System.nanoTime()) / 1000000;
        while (sleepTime > 0) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                interrupted = true;
            }
            sleepTime = (startTime + millis * 1000000 - System.nanoTime()) / 1000000;
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Before
    public void initMetricRegistry() {
        metricRegistry = new MetricRegistry();
    }

    @Test
    public void testLimitRequests() throws InterruptedException {
        LoadGenerator<Object> loadGenerator = new LoadGenerator<>(-1, 100, -1, new Metrics(metricRegistry));
        AtomicInteger requests = new AtomicInteger();
        Object task = new Object();
        Generator<Object> generator = () -> task;
        Loader<Object> loader = t -> requests.incrementAndGet();
        loadGenerator.doTest(
                ImmutableList.of(generator),
                ImmutableList.of(loader),
                new Properties()
        );
        assertEquals(100, requests.get());
    }

    @Test
    public void testLimitTime() throws InterruptedException {
        LoadGenerator<Object> loadGenerator = new LoadGenerator<>(2, -1, -1, new Metrics(metricRegistry));
        Object task = new Object();
        Generator<Object> generator = () -> task;
        Loader<Object> loader = t -> {
        };
        long startTime = System.nanoTime();
        loadGenerator.doTest(
                ImmutableList.of(generator),
                ImmutableList.of(loader),
                new Properties()
        );
        long duration = System.nanoTime() - startTime;
        assertTrue(duration < 2200l * 1000000 && duration > 1990 * 1000000);
    }

    @Test
    public void testLimitQps() throws InterruptedException {
        LoadGenerator<Object> loadGenerator = new LoadGenerator<>(-1, 20, 10, new Metrics(metricRegistry));
        Object task = new Object();
        Generator<Object> generator = () -> task;
        Loader<Object> loader = t -> {
        };
        long startTime = System.nanoTime();
        loadGenerator.doTest(
                ImmutableList.of(generator),
                ImmutableList.of(loader),
                new Properties()
        );
        long duration = System.nanoTime() - startTime;
        assertTrue(duration < 2200l * 1000000 && duration > 1800 * 1000000);
    }

    @Test
    public void testSupplierMultithreading() throws InterruptedException {
        LoadGenerator<Object> loadGenerator = new LoadGenerator<>(-1, 200, -1, new Metrics(metricRegistry));
        Object task = new Object();
        Generator<Object> generator = () -> {
            sleep(40);
            return task;
        };
        AtomicInteger requests = new AtomicInteger();
        Loader<Object> loader = t -> requests.incrementAndGet();
        long startTime = System.nanoTime();
        loadGenerator.doTest(
                ImmutableList.of(generator, generator, generator, generator),
                ImmutableList.of(loader),
                new Properties()
        );
        long duration = System.nanoTime() - startTime;
        assertTrue(
                String.format("Unexpected test duration: %sms", duration/1000000),
                duration < 2100l * 1000000 && duration > 1900 * 1000000
        );
    }

    @Test
    public void testConsumerMultithreading() throws InterruptedException {
        LoadGenerator<Object> loadGenerator = new LoadGenerator<>(-1, 200, -1, new Metrics(metricRegistry));
        Object task = new Object();
        Generator<Object> generator = () -> task;
        AtomicInteger requests = new AtomicInteger();
        Loader<Object> loader = t -> {
            //System.out.printf("start %s: %s\n", Thread.currentThread().getName(), (System.nanoTime() - startTime) / 1000000.);
            sleep(40);
            requests.incrementAndGet();
            //System.out.printf("end %s: %s\n", Thread.currentThread().getName(), (System.nanoTime() - startTime) / 1000000.);
        };
        long startTime = System.nanoTime();
        loadGenerator.doTest(
                ImmutableList.of(generator),
                ImmutableList.of(loader, loader, loader, loader),
                new Properties()
        );
        long duration = System.nanoTime() - startTime;
        assertTrue(
                String.format("Unexpected test duration: %sms", duration/1000000),
                duration < 2100l * 1000000 && duration > 1900 * 1000000
        );
    }
}