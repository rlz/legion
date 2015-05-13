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

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import java.util.concurrent.atomic.AtomicLong;

public class Metrics {
    public final MetricRegistry registry;
    public final Timer queries;
    public final Timer generator;
    public final Meter success;
    public final Meter exceptions;
    public final Gauge<Long> startDate;
    public final Gauge<Long> duration;

    private final AtomicLong startTime = new AtomicLong(-1);
    private final AtomicLong startNanoTime = new AtomicLong(-1);
    private final AtomicLong endNanoTime = new AtomicLong(-1);

    public Metrics() {
        this(new MetricRegistry());
    }

    public Metrics(MetricRegistry registry) {
        this(registry, "generator", "queries", "success", "exception", "startDate", "duration");
    }

    public Metrics(
            MetricRegistry registry,
            String generatorName,
            String queriesName,
            String successName,
            String exceptionName,
            String startDateName,
            String durationName
    ) {
        this.registry = registry;

        generator = registry.timer(generatorName);
        queries = registry.timer(queriesName);
        success = registry.meter(successName);
        exceptions = registry.meter(exceptionName);

        this.startDate = startTime::get;
        registry.register(startDateName, this.startDate);

        duration = () -> {
            long start = startNanoTime.get();
            if (start < 0) {
                return -1l;
            }

            long end = endNanoTime.get();

            if (end < 0) {
                return (System.nanoTime() - start) / 1000000;
            }
            return endNanoTime.get() - startNanoTime.get();
        };
        registry.register(durationName, duration);
    }

    public void markStart() {
        startTime.set(System.currentTimeMillis() / 1000);
        startNanoTime.set(System.nanoTime());
    }

    public void markEnd() {
        endNanoTime.set(System.nanoTime());
    }
}
