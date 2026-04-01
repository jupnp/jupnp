/*
 * Copyright (C) 2011-2026 4th Line GmbH, Switzerland and others
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License Version 1 or later
 * ("CDDL") (collectively, the "License"). You may not use this file
 * except in compliance with the License. See LICENSE.txt for more
 * information.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * SPDX-License-Identifier: CDDL-1.0
 */
package org.jupnp.tool.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import org.jupnp.QueueingThreadPoolExecutor;
import org.jupnp.util.Exceptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends the {@link QueueingThreadPoolExecutor} about monitoring of
 * tasks executed.
 */
public class MonitoredQueueingThreadPoolExecutor extends QueueingThreadPoolExecutor {

    static boolean DEBUG_STATISTICS = false;

    /** Statistical data collected. */
    private MonitoredQueueingThreadPoolExecutor.Statistics stats;

    static final Logger LOGGER = LoggerFactory.getLogger(MonitoredQueueingThreadPoolExecutor.class);
    static final Logger STATS_LOGGER = LoggerFactory.getLogger("org.jupnp.tool.cli.stats");

    public MonitoredQueueingThreadPoolExecutor(String poolName, int threadPoolSize) {
        super(poolName, threadPoolSize);
        LOGGER.debug("Created MonitoredQueueingThreadPoolExecutor with poolName={} and poolSize={}", poolName,
                threadPoolSize);
        if (DEBUG_STATISTICS) {
            stats = new Statistics(poolName);
        }
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        if (DEBUG_STATISTICS) {
            stats.addCurrentPoolSize(this);
            stats.addExcecutor(r);
        }
        // TODO why so much executors?
        // if (getQueue().size() > 100) {
        // System.out.println("ALERT");
        // }
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable throwable) {
        super.afterExecute(runnable, throwable);
        if (throwable != null) {
            Throwable cause = Exceptions.unwrap(throwable);
            if (cause instanceof InterruptedException && isTerminating()) {
                // Ignore this, might happen when we shutdownNow() the
                // executor. We can't
                // log at this point as the logging system might be stopped
                // already (e.g. if it's a CDI component).
                return;
            }
            // Log only
            LOGGER.warn("Thread terminated {} abruptly with exception", runnable, throwable);
        }
    }

    @Override
    public void shutdown() {
        LOGGER.info("shutdown");
        super.shutdown();
        if (DEBUG_STATISTICS) {
            stats.dumpPoolStats();
            stats.dumpExecutorsStats();
            stats.release();
        }
        LOGGER.info("shutdown done");
    }

    @Override
    public List<Runnable> shutdownNow() {
        LOGGER.info("shutdownNow");
        List<Runnable> res = super.shutdownNow();
        if (DEBUG_STATISTICS) {
            stats.dumpPoolStats();
            stats.dumpExecutorsStats();
            stats.release();
        }
        LOGGER.info("shutdownNow done");
        return res;
    }

    // inner classes for statistics

    static class Statistics {

        static class PoolStatPoint {
            public long timestamp, completedTasks;
            public int corePoolSize, poolSize, maxPoolSize, activeCounts, queueSize;
        }

        /** Thread safe collection for points. */
        private List<Statistics.PoolStatPoint> points = new CopyOnWriteArrayList<>();

        /** Thread safe collection for executors. */
        private ConcurrentHashMap<String, AtomicInteger> executors = new ConcurrentHashMap<>();

        private final String poolName;

        Statistics(String name) {
            poolName = name;
        }

        /**
         * Add info about current pool status.
         */
        private void addCurrentPoolSize(ThreadPoolExecutor pool) {
            Statistics.PoolStatPoint p = new PoolStatPoint();
            p.timestamp = System.currentTimeMillis();
            p.corePoolSize = pool.getCorePoolSize();
            p.poolSize = pool.getPoolSize();
            p.maxPoolSize = pool.getMaximumPoolSize();
            p.activeCounts = pool.getActiveCount();
            p.queueSize = pool.getQueue().size();
            p.completedTasks = pool.getCompletedTaskCount();
            points.add(p);
        }

        /**
         * Increase number of calls to this runnable (by class name).
         */
        public void addExcecutor(Runnable r) {
            executors.putIfAbsent(r.getClass().getName(), new AtomicInteger(0));
            executors.get(r.getClass().getName()).incrementAndGet();
        }

        public void release() {
            points = null;
            executors = null;
        }

        public void dumpPoolStats() {
            STATS_LOGGER.info("Dump Pool Statistics for poolName: {}", poolName);
            STATS_LOGGER.info("[timestamp,corePoolSize,poolSize,maxPoolSize,activeThreads,queueSize,completedTasks]");
            for (PoolStatPoint p : points) {
                STATS_LOGGER.info("{},{},{},{},{},{},{}", p.timestamp, p.corePoolSize, p.poolSize, p.maxPoolSize,
                        p.activeCounts, p.queueSize, p.completedTasks);
            }
            STATS_LOGGER.info(" ");
        }

        public void dumpExecutorsStats() {
            STATS_LOGGER.info("Dump Pool Executors for poolName: {}", poolName);

            List<ConcurrentHashMap.Entry<String, AtomicInteger>> entries = new ArrayList<>(executors.entrySet());
            // sort the entries by number of calls
            entries.sort((a, b) -> Integer.compare(b.getValue().get(), a.getValue().get()));

            STATS_LOGGER.info("[executorClassName,numberOfExecutes]");
            for (ConcurrentHashMap.Entry<String, AtomicInteger> e : entries) {
                STATS_LOGGER.info("{},{}", e.getKey(), e.getValue().get());
            }
            STATS_LOGGER.info(" ");
        }
    }
}
