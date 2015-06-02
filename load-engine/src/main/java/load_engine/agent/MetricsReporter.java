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

import com.codahale.metrics.*;
import load_engine.Metrics;

import java.io.OutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

public class MetricsReporter {
    private final PrintStream out;
    private final long rateFactor;
    private final double durationFactor;

    public MetricsReporter(OutputStream out, TimeUnit rateUnit, TimeUnit durationUnit) {
        this.out = new PrintStream(out);
        this.rateFactor = rateUnit.toSeconds(1);
        this.durationFactor = 1.0 / durationUnit.toNanos(1);
    }

    public void report(MetricRegistry registry) {
        SortedMap<String, Gauge> gauges = registry.getGauges();
        SortedMap<String, Counter> counters = registry.getCounters();
        SortedMap<String, Histogram> histograms = registry.getHistograms();
        SortedMap<String, Meter> meters = registry.getMeters();
        SortedMap<String, Timer> timers = registry.getTimers();

        Duration duration = Duration.of((long) gauges.get(Metrics.DURATION_METRIC_NAME).getValue(), ChronoUnit.MILLIS);
        Date startDate = new Date((long) gauges.get(Metrics.START_DATE_METRIC_NAME).getValue() * 1000);
        out.printf("= %s (started: %s)=====\n", duration.toString().substring(2), startDate);
        for (Map.Entry<String, Gauge> k : gauges.entrySet()) {
            if (!k.getKey().equals(Metrics.DURATION_METRIC_NAME) && !k.getKey().equals(Metrics.START_DATE_METRIC_NAME)) {
                out.printf("%s: %s\n", k.getKey(), k.getValue().getValue());
            }
        }
        for (Map.Entry<String, Counter> k : counters.entrySet()) {
            out.printf("%s: %s\n", k.getKey(), k.getValue().getCount());
        }
        for (Map.Entry<String, Histogram> k : histograms.entrySet()) {
            Snapshot s = k.getValue().getSnapshot();
            out.printf(
                    "%s: %s, max: %s, min: %s, mean: %s, median: %s, 75%%: %s, 95%%: %s, 98%%: %s, 99%%: %s, 99.9%% %s\n",
                    k.getKey(),
                    k.getValue().getCount(),
                    s.getMax(),
                    s.getMin(),
                    s.getMean(),
                    s.getMedian(),
                    s.get75thPercentile(),
                    s.get95thPercentile(),
                    s.get98thPercentile(),
                    s.get99thPercentile(),
                    s.get999thPercentile()
            );
        }
        for (Map.Entry<String, Meter> k : meters.entrySet()) {
            Meter m = k.getValue();
            out.printf(
                    "%s: %s (%s %s %s) %s\n",
                    k.getKey(),
                    m.getCount(),
                    convertRate(m.getOneMinuteRate()),
                    convertRate(m.getFiveMinuteRate()),
                    convertRate(m.getFifteenMinuteRate()),
                    convertRate(m.getMeanRate())
            );
        }
        for (Map.Entry<String, Timer> e : timers.entrySet()) {
            Timer t = e.getValue();
            Snapshot s = t.getSnapshot();
            out.printf(
                    "%s: %s (%s %s %s) %s\n",
                    e.getKey(),
                    t.getCount(),
                    convertRate(t.getOneMinuteRate()),
                    convertRate(t.getFiveMinuteRate()),
                    convertRate(t.getFifteenMinuteRate()),
                    convertRate(t.getMeanRate())
            );
            out.printf(
                    "\tmax: %s, min: %s, mean: %s, median: %s\n\t75%%: %s, 95%%: %s, 98%%: %s, 99%%: %s, 99.9%% %s\n",
                    convertDuration(s.getMax()),
                    convertDuration(s.getMin()),
                    convertDuration(s.getMean()),
                    convertDuration(s.getMedian()),
                    convertDuration(s.get75thPercentile()),
                    convertDuration(s.get95thPercentile()),
                    convertDuration(s.get98thPercentile()),
                    convertDuration(s.get99thPercentile()),
                    convertDuration(s.get999thPercentile())
            );
        }
        out.println();
    }

    public void close() {
        out.close();
    }

    private double convertRate(double rate) {
        return rate * rateFactor;
    }

    private double convertDuration(double duration) {
        return duration * durationFactor;
    }
}
