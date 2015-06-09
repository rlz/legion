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

import com.codahale.metrics.Timer;
import load_engine.Loader;
import load_engine.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class LoadThread<Task> extends Thread {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadThread.class);

    private final BlockingQueue<ScheduledTask<Task>> queue;
    private final Loader<Task> loader;
    private final Metrics metrics;

    public LoadThread(BlockingQueue<ScheduledTask<Task>> queue, Loader<Task> loader, Metrics metrics) {
        this.queue = queue;
        this.loader = loader;
        this.metrics = metrics;
    }

    @Override
    public void run() {
        while (true) {
            try {
                ScheduledTask<Task> task = queue.take();
                if (ScheduledTask.isFinalizer(task)) {
                    LOGGER.trace("Got finalizer - stop processing");
                    ScheduledTask.addFinalizer(queue);
                    break;
                }
                processTask(task);
            } catch (InterruptedException e) {
                LOGGER.info("interrupted");
                throw new RuntimeException(e);
            }
        }
        loader.close();
    }

    private void processTask(ScheduledTask<Task> task) throws InterruptedException {
        long now = System.nanoTime();
        while (((task.startTime - now) / 1000000) > 0) {
            Thread.sleep((task.startTime - now) / 1000000);
            now = System.nanoTime();
        }

        boolean success = true;
        try (Timer.Context ignored = metrics.queries.time()) {
            loader.run(task.task);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.info("Exception in loader", e);
            metrics.exceptions.mark();
            success = false;
        }
        if (success) {
            metrics.success.mark();
        }
    }
}
