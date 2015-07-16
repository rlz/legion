package legion.examples.simple;

import legion.Loader;

public class SimpleLoader implements Loader<String> {
    @Override
    public void run(String task) throws Exception {
        System.out.println(task);
    }
}
