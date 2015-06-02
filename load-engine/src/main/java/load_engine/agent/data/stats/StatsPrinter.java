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

package load_engine.agent.data.stats;

import com.google.common.base.Joiner;

import java.io.PrintStream;
import java.util.Date;

public class StatsPrinter {
    private final static Joiner JOINER = Joiner.on('\t');

    private final PrintStream out;

    public StatsPrinter(PrintStream out) {
        this.out = out;
    }

    public void printStats(int iteration, RunStats stats) {
        printMetric(iteration, ".duration", "seconds", stats.duration);
        printTimerStats(iteration, ".generator", stats.generator);
        printTimerStats(iteration, ".queries", stats.queries);
        printMeterStats(iteration, ".success", stats.success);
        printMeterStats(iteration, ".exceptions", stats.exceptions);
        UserDefinedStats uds = stats.userDefined;
        uds.gauges.forEach((k, v) -> printMetric(iteration, k, "value", v));
        uds.counters.forEach((k, v) -> printMetric(iteration, k, "value", v));
        uds.meters.forEach((k, v) -> printMeterStats(iteration, k, v));
        uds.histograms.forEach((k, v) -> printHistogramStats(iteration, k, v));
        uds.timers.forEach((k, v) -> printTimerStats(iteration, k, v));
    }

    public void printMeterStats(int iteration, String name, MeterStats stats) {
        printMetric(iteration, name, "count", stats.getCount());
        printMetric(iteration, name, "meanRate", stats.getMeanRate());
        printMetric(iteration, name, "oneMinuteRate", stats.getOneMinuteRate());
        printMetric(iteration, name, "fiveMinutesRate", stats.getFiveMinutesRate());
        printMetric(iteration, name, "fifteenMinutesRate", stats.getFifteenMinutesRate());
    }

    public void printHistogramStats(int iteration, String name, HistogramStats stats) {
        printMetric(iteration, name, "min", stats.getMin());
        printMetric(iteration, name, "max", stats.getMax());
        printMetric(iteration, name, "mean", stats.getMean());
        printMetric(iteration, name, "median", stats.getMedian());
        printMetric(iteration, name, "stddev", stats.getStddev());
        printMetric(iteration, name, "percentile75", stats.getPercentile75());
        printMetric(iteration, name, "percentile95", stats.getPercentile95());
        printMetric(iteration, name, "percentile98", stats.getPercentile98());
        printMetric(iteration, name, "percentile99", stats.getPercentile99());
        printMetric(iteration, name, "percentile999", stats.getPercentile999());
    }

    public void printTimerStats(int iteration, String name, TimerStats stats) {
        printMeterStats(iteration, name, stats);
        printHistogramStats(iteration, name, stats);
    }

    public void printMetric(int iteration, String metric, String subMetric, Object value) {
        out.println(
                JOINER.join(
                        iteration,
                        new Date().toString(),
                        metric,
                        subMetric,
                        value
                )
        );
    }

}
