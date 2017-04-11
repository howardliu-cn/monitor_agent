package com.wfj.monitor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class RequestCounter extends Counter {
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String counterName = "http.request";
    private Map<Integer, Counter> errorCounter = Collections.synchronizedMap(new HashMap<Integer, Counter>());

    protected RequestCounter(String name) {
        super(name);
    }
}
