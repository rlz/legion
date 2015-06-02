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

public class HistogramStatsImpl implements HistogramStats {
    private double min;
    private double max;
    private double mean;
    private double stddev;
    private double median;
    private double percentile75;
    private double percentile95;
    private double percentile98;
    private double percentile99;
    private double percentile999;

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public void setMin(double min) {
        this.min = min;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public void setMax(double max) {
        this.max = max;
    }

    @Override
    public double getMean() {
        return mean;
    }

    @Override
    public void setMean(double mean) {
        this.mean = mean;
    }

    @Override
    public double getStddev() {
        return stddev;
    }

    @Override
    public void setStddev(double stddev) {
        this.stddev = stddev;
    }

    @Override
    public double getMedian() {
        return median;
    }

    @Override
    public void setMedian(double median) {
        this.median = median;
    }

    @Override
    public double getPercentile75() {
        return percentile75;
    }

    @Override
    public void setPercentile75(double percentile75) {
        this.percentile75 = percentile75;
    }

    @Override
    public double getPercentile95() {
        return percentile95;
    }

    @Override
    public void setPercentile95(double percentile95) {
        this.percentile95 = percentile95;
    }

    @Override
    public double getPercentile98() {
        return percentile98;
    }

    @Override
    public void setPercentile98(double percentile98) {
        this.percentile98 = percentile98;
    }

    @Override
    public double getPercentile99() {
        return percentile99;
    }

    @Override
    public void setPercentile99(double percentile99) {
        this.percentile99 = percentile99;
    }

    @Override
    public double getPercentile999() {
        return percentile999;
    }

    @Override
    public void setPercentile999(double percentile999) {
        this.percentile999 = percentile999;
    }
}
