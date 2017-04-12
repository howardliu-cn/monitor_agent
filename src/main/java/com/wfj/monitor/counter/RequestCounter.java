package com.wfj.monitor.counter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
            response.setHeader(HEADER_SERVER_TAG, THIS_TAG);
            RequestDataWrapper wrapper = new RequestDataWrapper();
            wrapper.setTid(tid);
            wrapper.setRequest(request);
            wrapper.setResponse(response);
            wrapper.setStartTime(startTime);
            REQUEST_COUNTER_MAP.put(tid, wrapper);
            SLACounter.addSumInboundRequestCounts();
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
            long duration = endTime - wrapper.getStartTime();
            int status = response.getStatus();
            SLACounter.addHttpStatus(status);
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwable cause = wrapper.getCause();
            if (cause == null) {
                SLACounter.addSumOutboundRequestCounts();
                if (status < SC_BAD_REQUEST || status == SC_UNAUTHORIZED) {
                    SLACounter.addSumDealRequestCounts();
                    SLACounter.setPeerDealRequestTime(duration);
                    SLACounter.addSumDealRequestTime(duration);
                } else {
                    SLACounter.addSumErrDealRequestCounts();
                    SLACounter.addSumErrDealRequestTime(duration);
                }
            } else {
                SLACounter.addSumErrDealRequestCounts();
                SLACounter.addSumErrDealRequestTime(duration);
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
