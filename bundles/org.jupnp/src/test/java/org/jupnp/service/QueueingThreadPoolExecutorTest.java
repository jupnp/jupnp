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
package org.jupnp.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jupnp.QueueingThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test class for the {@link QueueingThreadPoolExecutor} class, abbreviated here
 * QueueingTPE.
 * 
 * @author Jochen Hiller - Initial contribution
 */
class QueueingThreadPoolExecutorTest {

    /**
     * we know that the QueuingTPE uses a core pool timeout of 10 seconds. Will
     * be needed to check if all threads are down after this timeout.
     */
    private static final int CORE_POOL_TIMEOUT = 10000;

    /**
     * We can enable logging for all test cases.
     */
    @BeforeEach
    void setUp() {
        // enable to see logging. See below how to include slf4j-simple
        // enableLogging();
        disableLogging();
    }

    /**
     * Creates QueueingTPE instances. By default there will be NO thread
     * created, check it.
     */
    @Test
    void testCreateInstance() {
        String poolName = "testCreateInstance";
        QueueingThreadPoolExecutor.createInstance(poolName, 1);
        QueueingThreadPoolExecutor.createInstance(poolName, 2);
        QueueingThreadPoolExecutor.createInstance(poolName, 5);
        QueueingThreadPoolExecutor.createInstance(poolName, 10);
        QueueingThreadPoolExecutor.createInstance(poolName, 1000);
        QueueingThreadPoolExecutor.createInstance(poolName, 10000);
        QueueingThreadPoolExecutor.createInstance(poolName, 100000);
        QueueingThreadPoolExecutor.createInstance(poolName, 1000000);
        // no threads created
        assertFalse(areThreadsFromPoolRunning(poolName));
    }

    /**
     * Tests what happens when poolName == null.
     */
    @Test
    void testCreateInstanceInvalidArgsPoolNameNull() {
        assertThrows(IllegalArgumentException.class, () -> QueueingThreadPoolExecutor.createInstance(null, 1));
    }

    @Test
    void testCreateInstanceInvalidArgsPoolSize0() {
        assertThrows(IllegalArgumentException.class, () -> QueueingThreadPoolExecutor.createInstance("test", 0));
    }

    @Test
    void testCreateInstanceInvalidArgsPoolSizeMinus1() {
        assertThrows(IllegalArgumentException.class, () -> QueueingThreadPoolExecutor.createInstance("test", -1));
    }

    /**
     * This test tests behavior of standard TPE for a pool of core=1, max=2 when
     * no tasks have been scheduled. Acts as reference test case.
     */
    @Test
    void testPlainTPEPoolSize2() throws InterruptedException {
        ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 2, 10L, TimeUnit.SECONDS, new SynchronousQueue<>());
        pool.allowCoreThreadTimeOut(true);

        assertEquals(0, pool.getActiveCount());
        assertTrue(pool.allowsCoreThreadTimeOut());
        assertEquals(0, pool.getCompletedTaskCount());
        assertEquals(1, pool.getCorePoolSize());
        assertEquals(2, pool.getMaximumPoolSize());
        assertEquals(0, pool.getLargestPoolSize());
        assertEquals(0, pool.getQueue().size());

        // there will be 1 core thread created. Will not check here

        // needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
        pool.shutdown();
        Thread.sleep(CORE_POOL_TIMEOUT + 1000);
    }

    /**
     * This test tests behavior of QueueingTPE for a pool of core=1, max=2 when
     * no tasks have been scheduled. Same assumptions as above.
     */
    @Test
    void testQueuingTPEPoolSize2() {
        String poolName = "testQueuingTPEPoolSize2";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

        assertEquals(0, pool.getActiveCount());
        assertTrue(pool.allowsCoreThreadTimeOut());
        assertEquals(0, pool.getCompletedTaskCount());
        assertEquals(1, pool.getCorePoolSize());
        assertEquals(2, pool.getMaximumPoolSize());
        assertEquals(0, pool.getLargestPoolSize());
        assertEquals(0, pool.getQueue().size());

        // now expect that no threads have been created
        assertFalse(areThreadsFromPoolRunning(poolName));

        // no need to wait after shutdown as no threads created
        pool.shutdown();
    }

    @Test
    void testPoolWithWellDefinedPoolName() throws InterruptedException {
        basicTestForPoolName("testPoolWithWellDefinedPoolName");
    }

    @Test
    void testPoolWithBlankPoolName() {
        assertThrows(IllegalArgumentException.class, () -> basicTestForPoolName(" "));
    }

    @Test
    void testPoolWithEmptyPoolName() {
        assertThrows(IllegalArgumentException.class, () -> basicTestForPoolName(""));
    }

    /**
     * Basic tests of a pool with a given name. Checks thread creation and
     * cleanup.
     */
    protected void basicTestForPoolName(String poolName) throws InterruptedException {
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        pool.execute(createRunnable100ms());
        pool.execute(createRunnable100ms());
        assertTrue(isPoolThreadActive(poolName, 1));
        assertTrue(isPoolThreadActive(poolName, 2));

        // no queue thread

        // needs to wait CORE_POOL_TIMEOUT + x until all threads are down again
        pool.shutdown();
        Thread.sleep(CORE_POOL_TIMEOUT + 1000);
        assertFalse(areThreadsFromPoolRunning(poolName));
    }

    @Test
    void testPoolSize1() throws InterruptedException {
        String poolName = "testPoolSize1";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 1);

        pool.execute(createRunnable100ms());
        assertEquals(1, pool.getActiveCount());

        pool.execute(createRunnable100ms());
        // queue thread must be active
        assertTrue(isQueueThreadActive(poolName));
        // all should be executed
        waitForAssert(() -> assertEquals(2, pool.getCompletedTaskCount()));
        // at the end queue thread is shutdown
        waitForAssert(() -> assertFalse(isQueueThreadActive(poolName)));

        pool.execute(createRunnable100ms());
        pool.execute(createRunnable100ms());
        assertTrue(isQueueThreadActive(poolName));
        // all should be executed
        waitForAssert(() -> assertEquals(4, pool.getCompletedTaskCount()));

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    @Test
    void testPoolSize2ThreadsFast() throws InterruptedException {
        String poolName = "testPoolSize2ThreadsFast";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        pool.execute(createRunnableFast());
        pool.execute(createRunnableFast());
        waitForAssert(() -> assertEquals(2, pool.getCompletedTaskCount()));

        // no queue thread

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    @Test
    void testPoolSize1000ThreadsFast() throws InterruptedException {
        AbstractRunnable.resetRuns();
        String poolName = "testPoolSize1000ThreadsFast";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 1000);
        for (int i = 0; i < 1000; i++) {
            pool.execute(createRunnableFast());
        }
        // no queue thread
        assertFalse(isQueueThreadActive(poolName));

        waitForAssert(() -> {
            assertEquals(1000, pool.getCompletedTaskCount(), "Completed tasks must match");
            assertEquals(1000, AbstractRunnable.getRuns(), "Number of executors runs must match");
        });

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    @Test
    void testPoolSize2ThreadsHeavyLoad() throws InterruptedException {
        String poolName = "testPoolSize2ThreadsHeavyLoad";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        pool.execute(createRunnableHeavyLoad1s());
        pool.execute(createRunnableHeavyLoad1s());
        pool.execute(createRunnableHeavyLoad1s());
        pool.execute(createRunnableHeavyLoad1s());

        assertTrue(isQueueThreadActive(poolName));
        waitForAssert(() -> assertEquals(4, pool.getCompletedTaskCount()));

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    @Test
    void testPoolSize2ThreadSettings() throws InterruptedException {
        String poolName = "testPoolSize2ThreadSettings";
        basicTestPoolSize2ThreadSettings(poolName);
    }

    /**
     * Test basic thread creation, including thread settings (name, prio,
     * daemon).
     */
    protected void basicTestPoolSize2ThreadSettings(String poolName) throws InterruptedException {
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

        // pool 2 tasks, threads must exist
        pool.execute(createRunnable10s());
        assertEquals(1, pool.getActiveCount(), "1 thread must be active");
        assertTrue(isPoolThreadActive(poolName, 1), "1 thread must be active in pool");
        Thread t1 = getThread(poolName + "-1");
        assertFalse(t1.isDaemon(), "Thread 1 MUST NOT be a daemon");
        // thread will be NORM prio or max prio of this thread group, which can
        // < than NORM
        int prio1 = Math.min(t1.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
        assertEquals(prio1, t1.getPriority());

        pool.execute(createRunnable10s());
        assertEquals(2, pool.getActiveCount(), "2 threads must be active");
        assertTrue(isPoolThreadActive(poolName, 2), "2 threads must be active in pool");
        Thread t2 = getThread(poolName + "-2");
        assertFalse(t2.isDaemon(), "Thread 2 MUST NOT be a daemon");
        // thread will be NORM prio or max prio of this thread group, which can
        // < than NORM
        int prio2 = Math.min(t2.getThreadGroup().getMaxPriority(), Thread.NORM_PRIORITY);
        assertEquals(prio2, t2.getPriority());

        // 2 more tasks, will be queued, no threads
        pool.execute(createRunnable1s());
        // as pool size is 2, no more active threads, will stay at 2
        assertEquals(2, pool.getActiveCount(), "2 threads must be active");
        assertFalse(isPoolThreadActive(poolName, 3), "There MUST NOT be a thread 3");
        assertEquals(1, pool.getQueue().size());

        pool.execute(createRunnable1s());
        assertEquals(2, pool.getActiveCount(), "2 threads must be active");
        assertFalse(isPoolThreadActive(poolName, 4), "There MUST NOT be a thread 4");
        assertEquals(2, pool.getQueue().size());

        // 0 are yet executed
        assertEquals(0, pool.getCompletedTaskCount());

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    /**
     * Test that no further tasks will be executed when TPE is terminating.
     * 
     * @TODO the check for "(threadPoolExecutor instanceof
     *       QueueingThreadPoolExecutor)" is not necessary. This can never
     *       happen
     */
    @Test
    void testShutdownNoEntriesIntoQueueAnymore() throws InterruptedException {
        String poolName = "testShutdownNoEntriesIntoQueueAnymore";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);

        pool.execute(createRunnable10s());
        pool.execute(createRunnable10s());
        pool.execute(createRunnable10s());
        assertEquals(1, pool.getQueue().size());
        pool.execute(createRunnable10s());
        assertEquals(2, pool.getQueue().size());

        // now shutdown, and check no more entries into pool
        pool.shutdown();
        // give chance to shutdown
        Thread.sleep(1000);
        assertTrue(pool.isShutdown());
        pool.execute(createRunnable10s());
        // must stay at 2
        assertEquals(2, pool.getQueue().size());

        // 0 are executed
        assertEquals(0, pool.getCompletedTaskCount());

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    /**
     * Tests what happens when wrong rejected execution handler will be used.
     */
    @Test
    void testSetInvalidRejectionHandler() {
        String poolName = "testShutdownNoEntriesIntoQueueAnymore";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        assertThrows(UnsupportedOperationException.class,
                () -> pool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy()));
    }

    /**
     * Test that interrupting queue thread does not loose tasks.
     */
    @Test
    void testQueueThreadInterrupt() throws InterruptedException {
        String poolName = "testQueueThreadInterrupt";
        final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 2);
        long tStart;
        long tEnd;

        // interrupt when queue thread is started from current thread
        for (int i = 0; i < 100; i++) {
            pool.execute(createRunnable100ms());
            Thread queueThread = getThread(poolName + "-queue");
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
        assertEquals(98, pool.getQueue().size());
        tStart = System.currentTimeMillis();
        tEnd = tStart;
        // wait for 10 sec
        while ((tEnd - tStart) < 10000) {
            // send interrupt very fast to jump into every piece of code
            for (int j = 0; j < 100; j++) {
                Thread queueThread = getThread(poolName + "-queue");
                if (queueThread != null) {
                    queueThread.interrupt();
                }
            }
            Thread.sleep(50); // chance for thread switch
            tEnd = System.currentTimeMillis();
        }
        // chance to finalize queue thread
        Thread.sleep(3000);
        assertFalse(isQueueThreadActive(poolName));
        // all should be executed
        assertEquals(100, pool.getCompletedTaskCount());

        // interrupt when queue thread is started from other threads
        for (int i = 0; i < 100; i++) {
            new Thread(() -> pool.execute(createRunnable100ms())).start();
            Thread queueThread = getThread(poolName + "-queue");
            if (queueThread != null) {
                queueThread.interrupt();
            }
        }
        tStart = System.currentTimeMillis();
        tEnd = tStart;
        // wait for 10 sec
        while ((tEnd - tStart) < 10000) {
            // send interrupt very fast to jump into every piece of code
            for (int j = 0; j < 100; j++) {
                Thread queueThread = getThread(poolName + "-queue");
                if (queueThread != null) {
                    queueThread.interrupt();
                }
            }
            Thread.sleep(50); // chance for thread switch
            tEnd = System.currentTimeMillis();
        }
        // all should be executed
        waitForAssert(() -> assertEquals(200, pool.getCompletedTaskCount()));

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    /**
     * This test checks if entries in queue will be fully processed.
     */
    @Test
    void testPoolSize4With1QueueEntry() throws InterruptedException {
        AbstractRunnable.resetRuns();
        String poolName = "testPoolSize4With1QueueEntry";
        final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 4);

        pool.execute(createRunnable100ms());
        pool.execute(createRunnable100ms());
        pool.execute(createRunnable100ms());
        pool.execute(createRunnable1s());
        pool.execute(createRunnable1s());
        assertEquals(4, pool.getActiveCount());
        assertTrue(isQueueThreadActive(poolName));
        assertEquals(1, pool.getQueue().size());

        // wait until all jobs have been processed
        Thread.sleep(2 * 1000 + 1000);

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));

        // all 5 jobs have to be executed
        assertEquals(5, AbstractRunnable.getRuns());
        assertTrue(isSequenceComplete(AbstractRunnable.getRunSequence(), 5));
    }

    /**
     * Tests large pool with long running tasks.
     */
    @Test
    void testPoolSize10ThreadsLongNewQueueThread() throws InterruptedException {
        String poolName = "testPoolSize10ThreadsLong";
        ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 10);

        for (int i = 0; i < 100; i++) {
            pool.execute(createRunnable100ms());
        }
        assertEquals(10, pool.getActiveCount());
        // queue thread must be active
        assertTrue(isQueueThreadActive(poolName));
        long queueThreadId1 = getThread(poolName + "-queue").getId();
        // all should be executed
        waitForAssert(() -> assertEquals(100, pool.getCompletedTaskCount()));
        // at the end queue thread is shutdown, wait for additional 3 sec
        waitForAssert(() -> assertFalse(isQueueThreadActive(poolName)));

        // now put again requests into queue. Queue thread should be created
        // again with different thread id
        for (int i = 0; i < 100; i++) {
            pool.execute(createRunnable100ms());
        }
        assertTrue(isQueueThreadActive(poolName));
        long queueThreadId2 = getThread(poolName + "-queue").getId();
        // as queue thread has been created again, has to have different thread id
        assertNotEquals(queueThreadId1, queueThreadId2);

        // all should be executed
        waitForAssert(() -> assertEquals(200, pool.getCompletedTaskCount()));
        // at the end queue thread is shutdown, wait for additional 3 sec
        waitForAssert(() -> assertFalse(isQueueThreadActive(poolName)));

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    /**
     * This tests checks addToQueue() when used from multiple threads.
     */
    @Test
    void testPoolSize10FillPoolParallel() throws InterruptedException {
        final String poolName = "testPoolSize10FillPoolParallel";
        final int poolSize = 10;
        final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, poolSize);

        final int N = 200;
        final int millisPerRunnable = 1000;
        final Thread[] fillThreads = new Thread[N];
        for (int i = 0; i < fillThreads.length; i++) {
            fillThreads[i] = new Thread(() -> pool.execute(createRunnable(millisPerRunnable)));
        }

        // now start all threads
        for (Thread fillThread : fillThreads) {
            fillThread.start();
        }
        waitForAssert(() -> assertEquals(10, pool.getActiveCount()));
        // queue thread must be active
        assertTrue(isQueueThreadActive(poolName));

        // after N * millisPerRunnable all created Runnables should be executed
        // since we have a pool of size "poolSize" the amount of Runnables running in parallel is "poolSize"
        // so the execution time is ideally: (N * millisPerRunnable) / poolSize
        // as there is some overhead we add some safety time for some tolerance
        final int safetyTime = N * 10; // millis
        Thread.sleep(((N * millisPerRunnable) / poolSize) + safetyTime);

        waitForAssert(() -> assertEquals(pool.getCompletedTaskCount(), N));
        // at the end queue thread is shutdown
        waitForAssert(() -> assertFalse(isQueueThreadActive(poolName)));

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    /**
     * Monkey test, just adding executors in parallel threads, and check if all
     * have been executed.
     */
    @Test
    void testMonkeyTest() throws InterruptedException {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        AbstractRunnable.resetRuns();
        String poolName = "testMonkeyTest";
        final ThreadPoolExecutor pool = QueueingThreadPoolExecutor.createInstance(poolName, 5);

        Thread[] fillThreads = new Thread[100];
        // fill first 5 threads with long running jobs to make sure threads are
        // blocked
        for (int i = 0; i < 5; i++) {
            fillThreads[i] = new Thread(() -> pool.execute(createRunnable10s()));
        }
        for (int i = 5; i < fillThreads.length; i++) {
            fillThreads[i] = new Thread(() -> {
                int action = new Random().nextInt(5);
                switch (action) {
                    case 0:
                        pool.execute(createRunnableFast());
                        break;
                    case 1:
                        pool.execute(createRunnable100ms());
                        break;
                    case 2:
                        pool.execute(createRunnable1s());
                        break;
                    case 3:
                        pool.execute(createRunnable10s());
                        break;
                    case 4:
                        pool.execute(createRunnableHeavyLoad1s());
                        break;
                }
            });
        }
        // now start all threads
        for (Thread fillThread : fillThreads) {
            fillThread.start();
        }

        waitForAssert(() -> assertEquals(5, pool.getActiveCount(), "All threads should be busy"));
        // queue thread must be active
        waitForAssert(() -> assertTrue(isQueueThreadActive(poolName)));

        // wait until processed
        while (pool.getCompletedTaskCount() < 100) {
            logger.info("getCompletedTaskCount: {}", pool.getCompletedTaskCount());
            Thread.sleep(1000);
        }
        logger.info("getCompletedTaskCount: {}", pool.getCompletedTaskCount());
        assertEquals(100, pool.getCompletedTaskCount(), "Completed tasks must match");
        // check runs too
        assertEquals(100, AbstractRunnable.getRuns(), "Number of executors runs must match");

        pool.shutdown();
        // after shutdown all threads are down again
        waitForAssert(() -> assertFalse(areThreadsFromPoolRunning(poolName)));
    }

    // helper methods

    private void enableLogging() {
        // add slf4j-simple-1.7.13.jar to classpath to activate logging during
        // development
        // e.g. from http://mvnrepository.com/artifact/org.slf4j/slf4j-simple
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
        System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
    }

    private void disableLogging() {
        // disable logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        System.clearProperty("org.slf4j.simpleLogger.logFile");
        System.clearProperty("org.slf4j.simpleLogger.showDateTime");
    }

    private boolean isQueueThreadActive(String poolName) {
        return getThread(poolName + "-queue") != null;
    }

    private boolean isPoolThreadActive(String poolName, int id) {
        return getThread(poolName + "-" + id) != null;
    }

    /**
     * Search for thread with given name.
     * 
     * @return found thread or null
     */
    private Thread getThread(String threadName) {
        // get top level thread group
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) {
            g = g.getParent();
        }
        // make buffer 10 entries bigger
        Thread[] l = new Thread[g.activeCount() + 10];
        int n = g.enumerate(l);
        for (int i = 0; i < n; i++) {
            // enable printout to see threads
            // System.out.println("getThread:" + l[i]);
            if (l[i].getName().equals(threadName)) {
                return l[i];
            }
        }
        return null;
    }

    private boolean areThreadsFromPoolRunning(String poolName) {
        // get top level thread group
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g.getParent() != null) {
            g = g.getParent();
        }
        boolean foundThreads = false;
        // make buffer 10 entries bigger
        Thread[] l = new Thread[g.activeCount() + 10];
        int n = g.enumerate(l);
        for (int i = 0; i < n; i++) {
            // we can only test if name is at least one character,
            // otherwise there will be threads found (handles poolName="")
            if (!poolName.isEmpty()) {
                if (l[i].getName().startsWith(poolName)) {
                    foundThreads = true;
                }
            }
        }
        return foundThreads;
    }

    /**
     * Just searches if all numbers from 1..max are included in sequence,
     * delimited by comma. Order does not matter.
     */
    private boolean isSequenceComplete(String sequence, int max) {
        sequence = "," + sequence;
        for (int i = 1; i <= max; i++) {
            if (!sequence.contains("," + i + ",")) {
                Logger logger = LoggerFactory.getLogger(this.getClass());
                logger.error("isSequenceComplete: missed {} in {}", i, sequence);
                return false;
            }
        }
        return true;
    }

    private boolean isSequenceOrdered(String sequence, int from, int to) {
        sequence = "," + sequence;
        int fromPos = sequence.indexOf(String.valueOf(from));
        StringTokenizer tokenizer = new StringTokenizer(sequence.substring(fromPos), ",");
        for (int i = from; i <= to; i++) {
            String val = tokenizer.nextToken();
            if (!val.equals(String.valueOf(i))) {
                Logger logger = LoggerFactory.getLogger(this.getClass());
                logger.error("isSequenceOrdered: missed {} in {}", i, sequence);
                return false;
            }
        }
        return true;
    }

    private static void waitForAssert(Runnable runnable) {
        final Instant start = Instant.now();
        while (Duration.between(start, Instant.now()).toSeconds() < 20) {
            try {
                runnable.run();
                return;
            } catch (Error | NullPointerException e1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e2) {
                    throw new IllegalStateException("Interrupted while sleeping", e2);
                }
            }
        }
        runnable.run();
    }

    // Runnables for testing

    private Runnable createRunnable(final int millis) {
        return new RunnableCustom(millis);
    }

    private Runnable createRunnableFast() {
        return new RunnableFast();
    }

    private Runnable createRunnable100ms() {
        return new Runnable100ms();
    }

    private Runnable createRunnable1s() {
        return new Runnable1s();
    }

    private Runnable createRunnable10s() {
        return new Runnable10s();
    }

    private Runnable createRunnableHeavyLoad1s() {
        return new RunnableHeavyLoad1s();
    }

    private abstract static class AbstractRunnable implements Runnable {
        private static AtomicInteger runs = new AtomicInteger(0);
        private static AtomicInteger lastUniqueId = new AtomicInteger(0);
        /**
         * We need a synchronized StringBuffer, StringBuilder is not sufficient
         * here.
         */
        private static StringBuffer runSequence = new StringBuffer();
        private final int uniqueId;

        public static void resetRuns() {
            LoggerFactory.getLogger(AbstractRunnable.class).info("resetRuns");
            runs = new AtomicInteger(0);
            runSequence = new StringBuffer();
            lastUniqueId = new AtomicInteger(0);
        }

        public static int getRuns() {
            return runs.get();
        }

        public static String getRunSequence() {
            return runSequence.toString();
        }

        protected Logger logger = LoggerFactory.getLogger(this.getClass());

        protected AbstractRunnable() {
            uniqueId = lastUniqueId.incrementAndGet();
        }

        protected void sleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                // ignore
                logger.info("interrupted");
            }
        }

        @Override
        public void run() {
            logger.info("run job {}", uniqueId);
            runs.incrementAndGet();
            runSequence.append(uniqueId);
            runSequence.append(',');
        }

        public String toString() {
            return "job " + uniqueId;
        }
    }

    private static class RunnableFast extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            // do nothing
        }
    }

    private static class RunnableHeavyLoad1s extends AbstractRunnable {
        /** preserve as static to avoid code optimization. */
        static double d = Math.PI;

        @Override
        public void run() {
            super.run();
            long tStart = System.currentTimeMillis();
            long tEnd = tStart;
            while ((tEnd - tStart) < 1000) {
                for (int i = 0; i < 10000; i++) {
                    d += Math.acos((double) tEnd);
                    d += Math.atan(Math.sqrt(Math.pow(d, 10)));
                }
                tEnd = System.currentTimeMillis();
            }
        }
    }

    private static class RunnableCustom extends AbstractRunnable {
        private final int millis;

        RunnableCustom(final int millis) {
            this.millis = millis;
        }

        @Override
        public void run() {
            super.run();
            sleep(millis);
        }
    }

    private static class Runnable100ms extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(100);
        }
    }

    private static class Runnable1s extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(1000);
        }
    }

    private static class Runnable10s extends AbstractRunnable {
        @Override
        public void run() {
            super.run();
            sleep(10 * 1000);
        }
    }
}
