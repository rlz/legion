.. highlight:: java

Legion Quick-Start
==================

First, we need to declare generator class::

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

Then we need to declare loader class::

    package legion.examples.simple;

    import legion.Loader;

    public class SimpleLoader implements Loader<String> {
        @Override
        public void run(String task) throws Exception {
            System.out.println(task);
        }
    }

Cool :)