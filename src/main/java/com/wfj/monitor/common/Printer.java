package com.wfj.monitor.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

/**
 * <br>created at 17-4-10
 *
 * @author liuxh
 * @version 1.0.0
 * @since 1.0.0
 */
public class Printer {
    public static final Map<String, String> STATEMENT_HASHCODE_SQL_MAP = Collections
            .synchronizedMap(new HashMap<String, String>());
    public static final Map<Long, Throwable> THROWABLE_COLLECTOR = Collections.synchronizedMap(
            new HashMap<Long, Throwable>());

    public static String remove(int hashCode) {
        return STATEMENT_HASHCODE_SQL_MAP.remove(hashCode + "");
    }

    public static String add(int hashCode, String sql) {
        return STATEMENT_HASHCODE_SQL_MAP.put(hashCode + "", sql);
    }

    public static String get(int hashCode) {
        return STATEMENT_HASHCODE_SQL_MAP.get(hashCode + "");
    }

    public static Throwable add(long threadId, Throwable cause) {
        return THROWABLE_COLLECTOR.put(threadId, cause);
    }

    public static Throwable getAndRemove(long threadId) {
        return THROWABLE_COLLECTOR.remove(threadId);
    }

    public static Throwable get(long threadId) {
        return THROWABLE_COLLECTOR.get(threadId);
    }

    public static Throwable remove(long threadId) {
        return THROWABLE_COLLECTOR.remove(threadId);
    }

    public static void counter(HttpServletRequest request, HttpServletResponse response, long duration,
            Throwable cause) {
        SLACounter.addSumInboundRequestCounts();
        int status = response.getStatus();
        SLACounter.addHttpStatus(status);
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
        System.out.println(SLACounter.instance());
    }

    public static void printBeforeRequest(HttpServletRequest request) {
        System.err.println(request.getRequestURI());
    }

    public static void printAfterCreatePreparedStatement(Object connection, String sql, Object stmt) {
        System.err.println("this object's hashcode is " + System.identityHashCode(connection));
        System.err.println("the sql is: [" + sql + "]");
        System.err.println("the result is: [" + stmt + "]");
        int resultHashCode = System.identityHashCode(stmt);
        System.err.println("the result's hashcode is: [" + resultHashCode + "]");
        Printer.add(resultHashCode, sql);
        System.out.println(STATEMENT_HASHCODE_SQL_MAP);
    }

    public static void printAfterExecuteInPreparedStatement(Object stmt) {
        int stmtHashCode = System.identityHashCode(stmt);
        System.err.println("this object's hashcode is " + stmtHashCode);
        System.err.println("the sql is: [" + Printer.get(stmtHashCode) + "]");
        System.out.println(STATEMENT_HASHCODE_SQL_MAP);
    }

    public static void printAfterPreparedStatementClose(Object stmt) {
        Printer.remove(System.identityHashCode(stmt));
        System.out.println(STATEMENT_HASHCODE_SQL_MAP);
    }
}
