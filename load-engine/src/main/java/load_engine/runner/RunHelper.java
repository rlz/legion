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

package load_engine.runner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class RunHelper<Task> {
    private final LoadGenerator<Task> loadGenerator;
    private final List<Supplier<Task>> suppliers = new ArrayList<>();
    private final List<Consumer<Task>> consumers = new ArrayList<>();

    public RunHelper(LoadGenerator<Task> loadGenerator) {
        this.loadGenerator = loadGenerator;
    }

    public RunHelper<Task> withSuppliers(List<? extends Supplier<Task>> suppliers) {
        this.suppliers.clear();
        this.suppliers.addAll(suppliers);
        return this;
    }

    public RunHelper<Task> withConstSupplier(Task task) {
        suppliers.clear();
        suppliers.add(() -> task);
        return this;
    }

    public RunHelper<Task> withSuppliers(Supplier<Task> supplier, int number) {
        suppliers.clear();
        for (int i = 0; i < number; ++i) {
            suppliers.add(supplier);
        }
        return this;
    }

    public RunHelper<Task> withConsumers(Collection<Consumer<Task>> consumers) {
        this.consumers.clear();
        this.consumers.addAll(consumers);
        return this;
    }

    public RunHelper<Task> withConsumers(Consumer<Task> consumer, int number) {
        consumers.clear();
        for (int i = 0; i < number; ++i) {
            consumers.add(consumer);
        }
        return this;
    }

    public void start() {
        loadGenerator.start(suppliers, consumers);
    }
}
