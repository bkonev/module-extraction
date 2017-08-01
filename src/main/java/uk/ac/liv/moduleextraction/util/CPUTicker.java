package uk.ac.liv.moduleextraction.util;

import com.google.common.base.Ticker;
import java.lang.management.ThreadMXBean;
import java.lang.management.ManagementFactory;

public class CPUTicker extends Ticker {
    private final ThreadMXBean threadTimer;

    public CPUTicker() {
        threadTimer = ManagementFactory.getThreadMXBean();
    }

    public long read() {
        return threadTimer.getCurrentThreadCpuTime();
    }
}
