package com.wfj.monitor.counter;

import com.wfj.monitor.handler.factory.SLACountManager;
import com.wfj.monitor.handler.wrapper.RequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.wfj.monitor.common.Constant.HEADER_SERVER_TAG;
import static com.wfj.monitor.common.Constant.THIS_TAG;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * <br>created at 17-4-11
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class RequestCounter extends Counter {
    private static Logger logger = LoggerFactory.getLogger(RequestCounter.class);
    protected final String counterName = "http.request";

    protected RequestCounter(String name) {
        super(name);
    }

    public static class ThreadRequest {
        private static final Map<Long, RequestDataWrapper> REQUEST_COUNTER_MAP =
                Collections.synchronizedMap(new HashMap<Long, RequestDataWrapper>());

        public static void begin(long tid, HttpServletRequest request, HttpServletResponse response) {
            assert request != null;
            assert response != null;
            long startTime = System.currentTimeMillis();
            long startThreadCupTime = ManagementFactory.getThreadMXBean().getThreadCpuTime(tid);
            response.setHeader(HEADER_SERVER_TAG, THIS_TAG);
            RequestDataWrapper wrapper = new RequestDataWrapper();
            wrapper.setTid(tid);
            wrapper.setRequest(request);
            wrapper.setResponse(response);
            wrapper.setStartTime(startTime);
            wrapper.setStartThreadCupTime(startThreadCupTime);
            REQUEST_COUNTER_MAP.put(tid, wrapper);
            SLACounter.addSumInboundRequestCounts();

            // counter
            SLACountManager.instance().getSumInboundRequestCounts().incrementAndGet();
        }

        public static void catchBlock(long tid, HttpServletRequest request, HttpServletResponse response,
                Throwable cause) {
            assert request != null;
            assert response != null;
            assert cause != null;
            RequestDataWrapper wrapper = REQUEST_COUNTER_MAP.get(tid);
            wrapper.setCause(cause);
        }

        public static void end(long tid, HttpServletRequest request, HttpServletResponse response) {
            assert request != null;
            assert response != null;
            long endTime = System.currentTimeMillis();
            RequestDataWrapper wrapper = REQUEST_COUNTER_MAP.get(tid);
            long startTime = wrapper.getStartTime();
            long startThreadCupTime = wrapper.getStartThreadCupTime();
            long duration = endTime - startTime;
            int status = response.getStatus();
            SLACounter.addHttpStatus(status);
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwable cause = wrapper.getCause();
            if (cause == null) {
                if (status < SC_BAD_REQUEST || status == SC_UNAUTHORIZED) {
                    SLACounter.addSumDealRequestCounts();
                    SLACounter.setPeerDealRequestTime(duration);
                    SLACounter.addSumDealRequestTime(duration);

                    // counter
                    SLACountManager.instance().getSumDealRequestCounts().incrementAndGet();
                    SLACountManager.instance().setPeerDealRequestTime(new AtomicLong(duration));
                    long sumDealTime = SLACountManager.instance().getSumDealRequestTime().get() + duration;
                    SLACountManager.instance().setSumDealRequestTime(new AtomicLong(sumDealTime));
                } else {
                    SLACounter.addSumErrDealRequestCounts();
                    SLACounter.addSumErrDealRequestTime(duration);

                    // counter
                    long sumDealTime = SLACountManager.instance().getSumErrDealRequestTime().get() + duration;
                    SLACountManager.instance().getSumErrDealRequestCounts().incrementAndGet();
                    SLACountManager.instance().setSumErrDealRequestTime(new AtomicLong(sumDealTime));
                }
                SLACounter.addSumOutboundRequestCounts();

                // counter
                SLACountManager.instance().getSumOutboundRequestCounts().incrementAndGet();
                RequestWrapper.SINGLETON.doExecute(request, response, startThreadCupTime, startTime);
            } else {
                SLACounter.addSumErrDealRequestCounts();
                SLACounter.addSumErrDealRequestTime(duration);

                long sumDealTime = SLACountManager.instance().getSumErrDealRequestTime().get() + duration;
                SLACountManager.instance().getSumErrDealRequestCounts().incrementAndGet();
                SLACountManager.instance().setSumErrDealRequestTime(new AtomicLong(sumDealTime));

                RequestWrapper.SINGLETON.doError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, startThreadCupTime,
                        startTime);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("the request counter is : " + SLACounter.instance());
            }
            REQUEST_COUNTER_MAP.remove(tid);
        }
    }

    public static class RequestDataWrapper {
        private long tid;
        private HttpServletRequest request;
        private HttpServletResponse response;
        private long startTime;
        private long startThreadCupTime;
        private Throwable cause;

        public long getTid() {
            return tid;
        }

        public void setTid(long tid) {
            this.tid = tid;
        }

        public HttpServletRequest getRequest() {
            return request;
        }

        public void setRequest(HttpServletRequest request) {
            this.request = request;
        }

        public HttpServletResponse getResponse() {
            return response;
        }

        public void setResponse(HttpServletResponse response) {
            this.response = response;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getStartThreadCupTime() {
            return startThreadCupTime;
        }

        public void setStartThreadCupTime(long startThreadCupTime) {
            this.startThreadCupTime = startThreadCupTime;
        }

        public Throwable getCause() {
            return cause;
        }

        public void setCause(Throwable cause) {
            this.cause = cause;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RequestDataWrapper that = (RequestDataWrapper) o;
            return tid == that.tid;
        }

        @Override
        public int hashCode() {
            return (int) (tid ^ (tid >>> 32));
        }
    }
}
