package legion.tool.agent.data.stats;

import com.google.gson.Gson;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RunStatsTest {
    @Test
    public void testGenerate() {
        var rs = new RunStats();
        var gson = new Gson().toJson(rs);
        assertEquals("{\"generator\":{\"count\":0,\"meanRate\":0.0,\"oneMinuteRate\":0.0," +
                "\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0,\"min\":0.0,\"max\":0.0,\"mean\":0.0," +
                "\"stddev\":0.0,\"median\":0.0,\"percentile75\":0.0,\"percentile95\":0.0,\"percentile98\":0.0," +
                "\"percentile99\":0.0,\"percentile999\":0.0},\"queries\":{\"count\":0,\"meanRate\":0.0," +
                "\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0,\"min\":0.0,\"max\":0.0," +
                "\"mean\":0.0,\"stddev\":0.0,\"median\":0.0,\"percentile75\":0.0,\"percentile95\":0.0," +
                "\"percentile98\":0.0,\"percentile99\":0.0,\"percentile999\":0.0},\"success\":" +
                "{\"count\":0,\"meanRate\":0.0,\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0," +
                "\"fifteenMinutesRate\":0.0},\"exceptions\":{\"count\":0,\"meanRate\":0.0," +
                "\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0}," +
                "\"userDefined\":{\"gauges\":{},\"counters\":{},\"meters\":{},\"histograms\":{}," +
                "\"timers\":{}},\"startDate\":0,\"duration\":0,\"durationLimit\":0," +
                "\"queriesLimit\":0,\"qpsLimit\":0,\"isRunning\":false}", gson);
    }

    @Test
    public void testParse() {
        new Gson().fromJson("{\"generator\":{\"count\":0,\"meanRate\":0.0,\"oneMinuteRate\":0.0," +
                "\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0,\"min\":0.0,\"max\":0.0,\"mean\":0.0," +
                "\"stddev\":0.0,\"median\":0.0,\"percentile75\":0.0,\"percentile95\":0.0,\"percentile98\":0.0," +
                "\"percentile99\":0.0,\"percentile999\":0.0},\"queries\":{\"count\":0,\"meanRate\":0.0," +
                "\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0,\"min\":0.0,\"max\":0.0," +
                "\"mean\":0.0,\"stddev\":0.0,\"median\":0.0,\"percentile75\":0.0,\"percentile95\":0.0," +
                "\"percentile98\":0.0,\"percentile99\":0.0,\"percentile999\":0.0},\"success\":" +
                "{\"count\":0,\"meanRate\":0.0,\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0," +
                "\"fifteenMinutesRate\":0.0},\"exceptions\":{\"count\":0,\"meanRate\":0.0," +
                "\"oneMinuteRate\":0.0,\"fiveMinutesRate\":0.0,\"fifteenMinutesRate\":0.0}," +
                "\"userDefined\":{\"gauges\":{},\"counters\":{},\"meters\":{},\"histograms\":{}," +
                "\"timers\":{}},\"startDate\":0,\"duration\":0,\"durationLimit\":0," +
                "\"queriesLimit\":0,\"qpsLimit\":0,\"isRunning\":false}\n", RunStats.class);
    }
}