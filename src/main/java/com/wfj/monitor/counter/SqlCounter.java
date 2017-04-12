package com.wfj.monitor.counter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class SqlCounter extends Counter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String counterName = "sql";

    protected SqlCounter(String name) {
        super(name);
    }
}
