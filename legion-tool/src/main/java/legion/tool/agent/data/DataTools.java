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

package legion.tool.agent.data;

import com.codahale.metrics.*;
import com.google.common.collect.Maps;
import legion.Metrics;
import legion.tool.agent.data.stats.*;

public class DataTools {
    public static void addHistogramValues(Sampling src, HistogramStats object) {
        Snapshot snapshot = src.getSnapshot();
        object.setMin(snapshot.getMin());
        object.setMax(snapshot.getMax());
        object.setMean(snapshot.getMean());
        object.setStddev(snapshot.getStdDev());
        object.setMedian(snapshot.getMedian());
        object.setPercentile75(snapshot.get75thPercentile());
        object.setPercentile95(snapshot.get95thPercentile());
        object.setPercentile98(snapshot.get98thPercentile());
        object.setPercentile99(snapshot.get99thPercentile());
        object.setPercentile999(snapshot.get999thPercentile());
    }

    public static void addMeterValues(Metered src, MeterStats stats) {
        stats.setCount(src.getCount());
        stats.setMeanRate(src.getMeanRate());
        stats.setOneMinuteRate(src.getOneMinuteRate());
        stats.setFiveMinutesRate(src.getFiveMinuteRate());
        stats.setFifteenMinutesRate(src.getFifteenMinuteRate());
    }

    public static void addTimerValues(Timer src, TimerStats stats) {
        addMeterValues(src, stats);
        addHistogramValues(src, stats);
    }

    public static void fillRunStats(Metrics metrics, RunStats stats) {
        stats.duration = metrics.duration.getValue();
        stats.startDate = metrics.startDate.getValue();
        addTimerValues(metrics.generator, stats.generator);
        addTimerValues(metrics.queries, stats.queries);
        addMeterValues(metrics.exceptions, stats.exceptions);
        addMeterValues(metrics.success, stats.success);

        MetricRegistry registry = metrics.registry;
        UserDefinedStats uds = stats.userDefined;
        MetricFilter metricFilter = (name, metric) -> !name.startsWith(".");
        uds.gauges.putAll(Maps.transformValues(registry.getGauges(metricFilter), g -> g.getValue().toString()));
        uds.counters.putAll(Maps.transformValues(registry.getCounters(metricFilter), Counter::getCount));
        uds.meters.putAll(
                Maps.transformValues(registry.getMeters(metricFilter), m -> {
                            MeterStatsImpl s = new MeterStatsImpl();
                            addMeterValues(m, s);
                            return s;
                        }
                )
        );
        uds.histograms.putAll(
                Maps.transformValues(registry.getHistograms(metricFilter), h -> {
                            HistogramStatsImpl s = new HistogramStatsImpl();
                            addHistogramValues(h, s);
                            return s;
                        }
                )
        );
        uds.timers.putAll(
                Maps.transformValues(registry.getTimers(metricFilter), t -> {
                            TimerStats s = new TimerStats();
                            addTimerValues(t, s);
                            return s;
                        }
                )
        );
    }
}
