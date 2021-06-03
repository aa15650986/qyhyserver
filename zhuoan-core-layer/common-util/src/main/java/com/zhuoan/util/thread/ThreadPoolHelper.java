
package com.zhuoan.util.thread;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;

public class ThreadPoolHelper {
    public static ExecutorService executorService;

    public ThreadPoolHelper() {
    }

    static {
        executorService = new ThreadPoolExecutor(8, 2147483647, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue(2), (new ThreadFactoryBuilder()).setNameFormat("call-back-handle-pool-%d").build(), new AbortPolicy());
    }
}
