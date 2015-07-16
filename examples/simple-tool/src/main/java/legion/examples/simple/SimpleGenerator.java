package legion.examples.simple;

import com.google.common.collect.Iterators;
import legion.Generator;

import java.util.Iterator;

public class SimpleGenerator implements Generator<String> {
    private final Iterator<String> it = Iterators.cycle("one", "two", "three");

    public String generate() throws Exception {
        return it.next();
    }
}
