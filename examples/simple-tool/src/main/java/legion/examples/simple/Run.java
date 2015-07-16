package legion.examples.simple;

import com.google.common.collect.ImmutableList;
import legion.Metrics;
import legion.runner.LoadGenerator;

import java.util.Properties;

public class Run {
    public static void main(String[] args) throws InterruptedException {
        LoadGenerator<String> generator = new LoadGenerator<>(-1, 10, 2, 1, new Metrics());
        generator.doTest(ImmutableList.of(new SimpleGenerator()), ImmutableList.of(new SimpleLoader()), new Properties());
    }
}
