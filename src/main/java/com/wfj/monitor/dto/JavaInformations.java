/*
 * Copyright 2008-2016 by Emeric Vernat
 *
 *     This file is part of Java Melody.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.wfj.monitor.dto;

import com.wfj.monitor.conf.EnvPropertyConfig;
import com.wfj.monitor.conf.Parameters;
import com.wfj.monitor.handler.wrapper.JdbcWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

/**
 * Informations systèmes sur le serveur, sans code html de présentation. L'état
 * d'une instance est initialisé à son instanciation et non mutable; il est donc
 * de fait thread-safe. Cet état est celui d'une instance de JVM java, de ses
 * threads et du système à un instant t. Les instances sont sérialisables pour
 * pouvoir être transmises au serveur de collecte.
 *
 * @author Emeric Vernat
 */
public class JavaInformations implements Serializable {

    private static Logger log = LoggerFactory.getLogger(JavaInformations.class);

    /**
     * 一下计算 CUP 占用率使用
     */
    private static final int CPUTIME = 30;
    private static final int PERCENT = 100;
    private static final int FAULTLENGTH = 10;

    public static final double HIGH_USAGE_THRESHOLD_IN_PERCENTS = 95d;
    private static final long serialVersionUID = 3281861236369720876L;
    private static final Date START_DATE = new Date();
    private static final boolean SYSTEM_CPU_LOAD_ENABLED = "1.7".compareTo(Parameters.JAVA_VERSION) < 0;
    private static boolean localWebXmlExists = true; // true par défaut
    private static boolean localPomXmlExists = true; // true par défaut

    private MemoryInformations memoryInformations;
    @SuppressWarnings("all")
    private List<TomcatInformations> tomcatInformationsList;
    private int sessionCount;
    private long sessionAgeSum;
    private int activeThreadCount;
    private int usedConnectionCount;
    private int maxConnectionCount;
    private int activeConnectionCount;
    private long transactionCount;
    private long processCpuTimeMillis;

    private double systemLoadAverage;
    private double systemCpuLoad;

    private double beforeCpuTime = 0;
    private double beforeCpuUpTime = 0;
    private double processCpuLoad;

    private long unixOpenFileDescriptorCount;
    private long unixMaxFileDescriptorCount;
    private String host;
    private String os;

    private String arc;
    private String sysVersion;

    private int availableProcessors;
    private String javaVersion;
    private String jvmVersion;

    private String serverInfo;
    private String contextPath;
    private String contextDisplayName;
    private Date startDate;
    private String jvmArguments;
    private long freeDiskSpaceInTemp;

    private int threadCount;
    private int peakThreadCount;
    private int daemonThreadCount;
    private long totalStartedThreadCount;
    private long currentThreadCpuTime;
    private long currentThreadUserTime;

    private String vmName;
    private String vmVendor;
    private String vmVersion;
    private String classPath;
    private String libraryPath;
    private String compliationName;
    private long totalCompliationTime;

    // 以下部分属于详细信息部分
    private String pid;
    private String dataBaseVersion;
    private String dataSourceDetails;
    @SuppressWarnings("all")
    private List<ThreadInformations> threadInformationsList;
    @SuppressWarnings("all")
    private List<JobInformations> jobInformationsList;

    @SuppressWarnings("all")
    private List<String> dependenciesList;
    private boolean webXmlExists = localWebXmlExists;
    private boolean pomXmlExists = localPomXmlExists;

    private static JavaInformations JavaInfo = null;

    public static final class ThreadInformationsComparator implements Comparator<ThreadInformations>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(ThreadInformations thread1, ThreadInformations thread2) {
            return thread1.getName().compareToIgnoreCase(thread2.getName());
        }
    }

    public static final class JobInformationsComparator implements Comparator<JobInformations>, Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(JobInformations job1, JobInformations job2) {
            return job1.getName().compareToIgnoreCase(job2.getName());
        }
    }

    public static JavaInformations instance(ServletContext servletContext, boolean includeDetails) {
        if (JavaInfo != null) {
            return JavaInfo;
        } else {
            JavaInfo = new JavaInformations(servletContext, includeDetails);
            return JavaInfo;
        }
    }

    private JavaInformations(ServletContext servletContext, boolean includeDetails) {
        super();
        buildJavaInfo(servletContext, includeDetails);
    }

    /**
     * 重建监控信息
     *
     * @param servletContext
     * @param includeDetails void
     * @Methods Name rebuildJavaInfo
     * @Create In 2016年3月26日 By Jack
     */
    public void rebuildJavaInfo(ServletContext servletContext, boolean includeDetails) {
        clearJavaInfo();
        buildJavaInfo(servletContext, includeDetails);
    }

    /**
     * 清理基本信息
     *
     * @Methods Name clearJavaInfo
     * @Create In 2016年3月26日 By Jack void
     */
    public void clearJavaInfo() {
        // 链接信息
        sessionCount = 0;// SessionListener.getSessionCount();
        sessionAgeSum = 0; // SessionListener.getSessionAgeSum();

        // 数据源信息
        activeThreadCount = 0;
        usedConnectionCount = 0;
        activeConnectionCount = 0;
        maxConnectionCount = 0;
        transactionCount = 0;

        // 系统信息
        memoryInformations = null;
        tomcatInformationsList = null;
        systemLoadAverage = 0;
        systemCpuLoad = 0;
        processCpuTimeMillis = 0;
        unixOpenFileDescriptorCount = 0;
        unixMaxFileDescriptorCount = 0;
        host = "";
        os = "";
        availableProcessors = 0;
        javaVersion = "";
        jvmVersion = "";

        serverInfo = null;
        contextPath = null;
        contextDisplayName = null;
        dependenciesList = null;

        jvmArguments = "";
        threadCount = 0;
        peakThreadCount = 0;
        totalStartedThreadCount = 0;
        freeDiskSpaceInTemp = 0;

        dataBaseVersion = null;
        dataSourceDetails = null;
        threadInformationsList = null;
        jobInformationsList = null;
        pid = null;

        arc = "";
        sysVersion = "";
        processCpuLoad = 0;
        daemonThreadCount = 0;
        currentThreadCpuTime = 0;
        currentThreadUserTime = 0;

        vmName = "";
        vmVendor = "";
        vmVersion = "";

        classPath = "";
        libraryPath = "";
        compliationName = "";
        totalCompliationTime = 0;
    }

    /**
     * @param servletContext
     * @param includeDetails void
     * @Methods Name buildJavaInfo
     * @Create In 2016年3月26日 By Jack
     */
    private void buildJavaInfo(ServletContext servletContext, boolean includeDetails) {
        // 链接信息
        sessionCount = 0;// SessionListener.getSessionCount();
        sessionAgeSum = 0; // SessionListener.getSessionAgeSum();

        // 数据源信息
        activeThreadCount = JdbcWrapper.getActiveThreadCount();
        usedConnectionCount = JdbcWrapper.getUsedConnectionCount();
        activeConnectionCount = JdbcWrapper.getActiveConnectionCount();
        maxConnectionCount = JdbcWrapper.getMaxConnectionCount();
        transactionCount = JdbcWrapper.getTransactionCount();

        // 系统信息
        memoryInformations = new MemoryInformations();
        tomcatInformationsList = TomcatInformations.buildTomcatInformationsList();
        systemLoadAverage = buildSystemLoadAverage();
        systemCpuLoad = buildSystemCpuLoad();
        processCpuTimeMillis = buildProcessCpuTimeMillis();
        unixOpenFileDescriptorCount = buildOpenFileDescriptorCount();
        unixMaxFileDescriptorCount = buildMaxFileDescriptorCount();
        host = Parameters.getHostName() + '@' + Parameters.getHostAddress();
        os = buildOS();

        arc = System.getProperty("os.arch");
        sysVersion = System.getProperty("os.version");
        processCpuLoad = buildProcessCpuLoad();

        availableProcessors = Runtime.getRuntime().availableProcessors();
        javaVersion = System.getProperty("java.runtime.name") + ", " + System.getProperty("java.runtime.version");
        jvmVersion = System.getProperty("java.vm.name") + ", " + System.getProperty("java.vm.version") + ", " + System
                .getProperty("java.vm.info");

        vmName = System.getProperty("java.vm.name");
        vmVendor = System.getProperty("java.vm.vendor");
        vmVersion = System.getProperty("java.vm.version");

        classPath = ManagementFactory.getRuntimeMXBean().getClassPath();
        libraryPath = ManagementFactory.getRuntimeMXBean().getLibraryPath();
        compliationName = ManagementFactory.getCompilationMXBean().getName();
        totalCompliationTime = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();

        if (servletContext == null) {
            serverInfo = null;
            contextPath = null;
            contextDisplayName = null;
            dependenciesList = null;
        } else {
            serverInfo = servletContext.getServerInfo();
            contextPath = Parameters.getContextPath(servletContext);
            contextDisplayName = servletContext.getServletContextName();
            dependenciesList = buildDependenciesList(servletContext);
        }
        startDate = START_DATE;
        jvmArguments = buildJvmArguments();

        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        threadCount = threadBean.getThreadCount();
        peakThreadCount = threadBean.getPeakThreadCount();
        totalStartedThreadCount = threadBean.getTotalStartedThreadCount();
        daemonThreadCount = threadBean.getDaemonThreadCount();
        currentThreadCpuTime = threadBean.getCurrentThreadCpuTime();
        currentThreadUserTime = threadBean.getCurrentThreadUserTime();

        freeDiskSpaceInTemp = Parameters.TEMPORARY_DIRECTORY.getFreeSpace();

        if (includeDetails) {
            dataBaseVersion = buildDataBaseVersion();
            dataSourceDetails = buildDataSourceDetails();
            threadInformationsList = buildThreadInformationsList();
            jobInformationsList = JobInformations.buildJobInformationsList();
            pid = PID.getPID();
        } else {
            dataBaseVersion = null;
            dataSourceDetails = null;
            threadInformationsList = null;
            jobInformationsList = null;
            pid = null;
        }
    }

    public static void setWebXmlExistsAndPomXmlExists(boolean webXmlExists, boolean pomXmlExists) {
        localWebXmlExists = webXmlExists;
        localPomXmlExists = pomXmlExists;
    }

    public boolean doesWebXmlExists() {
        return webXmlExists;
    }

    public boolean doesPomXmlExists() {
        return pomXmlExists;
    }

    private static String buildOS() {
        final String name = System.getProperty("os.name");
        final String version = System.getProperty("os.version");
        final String patchLevel = System.getProperty("sun.os.patch.level");
        final String arch = System.getProperty("os.arch");
        final String bits = System.getProperty("sun.arch.data.model");

        final StringBuilder sb = new StringBuilder();
        sb.append(name).append(", ");
        if (!name.toLowerCase(Locale.ENGLISH).contains("windows")) {
            // version is "6.1" and useless for os.name "Windows 7",
            // and can be "2.6.32-358.23.2.el6.x86_64" for os.name "Linux"
            sb.append(version).append(' ');
        }
        if (!"unknown".equals(patchLevel)) {
            // patchLevel is "unknown" and useless on Linux,
            // and can be "Service Pack 1" on Windows
            sb.append(patchLevel);
        }
        sb.append(", ").append(arch).append('/').append(bits);
        return sb.toString();
    }

    private static long buildProcessCpuTimeMillis() {
        final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (isSunOsMBean(operatingSystem)) {
            // nano-secondes converties en milli-secondes
            return MemoryInformations.getLongFromOperatingSystem(operatingSystem, "getProcessCpuTime") / 1000000;
        }
        return -1;
    }

    private static long buildOpenFileDescriptorCount() {
        final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (isSunOsMBean(operatingSystem) && isSunUnixMBean(operatingSystem)) {
            try {
                return MemoryInformations.getLongFromOperatingSystem(operatingSystem, "getOpenFileDescriptorCount");
            } catch (final Error e) {
                // pour issue 16 (using jsvc on ubuntu or debian)
                return -1;
            }
        }
        return -1;
    }

    private static long buildMaxFileDescriptorCount() {
        final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (isSunOsMBean(operatingSystem) && isSunUnixMBean(operatingSystem)) {
            try {
                return MemoryInformations.getLongFromOperatingSystem(operatingSystem, "getMaxFileDescriptorCount");
            } catch (final Error e) {
                // pour issue 16 (using jsvc on ubuntu or debian)
                return -1;
            }
        }
        return -1;
    }

    private static double buildSystemCpuLoad() {
        // System cpu load.
        // The "recent cpu usage" for the whole system.
        // This value is a double in the [0.0,1.0] interval.
        // A value of 0.0 means that all CPUs were idle during the recent period
        // of time observed,
        // while a value of 1.0 means that all CPUs were actively running 100%
        // of the time during the recent period being observed.
        final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (SYSTEM_CPU_LOAD_ENABLED && isSunOsMBean(operatingSystem)) {
            // systemCpuLoad n'existe qu'à partir du jdk 1.7
            return MemoryInformations.getDoubleFromOperatingSystem(operatingSystem, "getSystemCpuLoad") * 100;
        } else {
            if (buildOS().contains("windows")) {
                return getCpuRatioForWindows();
            } else {
                return getCpuRateForLinux();
            }
        }
    }

    private double buildProcessCpuLoad() {
        if (buildOS().contains("windows")) {
            return getCpuRatioForWindowsByPID();
        } else {
            return getCupRateForLinuxByPID();
        }
    }

    private double getCpuRatioForWindowsByPID() {
        MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
        com.sun.management.OperatingSystemMXBean osm;
        double cpuUsage = 0;
        try {
            osm = ManagementFactory.newPlatformMXBeanProxy(mbsc, ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,
                    com.sun.management.OperatingSystemMXBean.class);
            if (this.beforeCpuTime == 0) {
                this.beforeCpuTime = osm.getProcessCpuTime();
                this.beforeCpuUpTime = System.nanoTime();
            } else {
                if (osm.getProcessCpuTime() > this.beforeCpuTime) {
                    cpuUsage = ((osm.getProcessCpuTime() - this.beforeCpuTime) * 100L) / (System
                            .nanoTime() - this.beforeCpuUpTime);
                    cpuUsage = Math.abs(cpuUsage);
                    this.beforeCpuTime = osm.getProcessCpuTime();
                    this.beforeCpuUpTime = System.nanoTime();
                } else {
                    this.beforeCpuTime = osm.getProcessCpuTime();
                    this.beforeCpuUpTime = System.nanoTime();
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
        }
        return cpuUsage;
    }

    /**
     * 获取当前进程的CPU占有率
     * cmd:top -b -n 1 -p $pid | sed '$d' | sed -n '$p' | awk '{print $9}'
     *
     * @return double
     * @Methods Name getCupRateForLinuxByPID
     * @Create In 2016年4月5日 By Jack
     */
    private static double getCupRateForLinuxByPID() {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader brStat = null;
        Double usage = (double) 0;
        Process process;
        try {
            // Process process = Runtime.getRuntime().exec("top -n 1 -p " +
            // PID.getPID() + " | sed '$d' | sed -n '$p' | awk '{print $9}'");
            if (buildOS().contains("Mac OS X")) {
                String[] cmd = {"/bin/sh", "-c", "top -n 1 -pid " + PID.getPID()};
                process = Runtime.getRuntime().exec(cmd);
            } else {
                // cmds = "ps -aux | grep java | grep " + PID.getPID();
                String[] cmd = {"/bin/sh", "-c", "ps -aux | grep java | grep " + PID.getPID()};
                process = Runtime.getRuntime().exec(cmd);
            }

            is = process.getInputStream();
            isr = new InputStreamReader(is);
            brStat = new BufferedReader(isr);

            // String line = null;
            // while((line = brStat.readLine()) != null ){
            // usage = Double.parseDouble(line);
            // if(usage>100) usage = usage/10;//刚启动会出现CPU100多情况，则处理除于10
            // return usage;
            // }

            String line = brStat.readLine();
            while (!line.contains(PID.getPID())) {
                line = brStat.readLine();
            }
            if (!line.equalsIgnoreCase("")) {
                StringTokenizer st = new StringTokenizer(line);
                int count = 0;
                String value = "";
                while (st.hasMoreElements() && count++ < 3) {
                    value = st.nextToken();
                }
                if (!value.equals("")) {
                    usage = Double.valueOf(value);
                }
            }

        } catch (IOException e) {
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
            freeResource(is, isr, brStat);
        } finally {
            freeResource(is, isr, brStat);
        }

        return usage;
    }

    /**
     * 获取 Linux 系统CPU速率
     *
     * @return double
     * @Methods Name getCpuRateForLinux
     * @Create In 2015年10月27日 By Jack
     */
    private static double getCpuRateForLinux() {
        InputStream is = null;
        InputStreamReader isr = null;
        BufferedReader brStat = null;
        Double usage = (double) 0;
        try {
            Process process = Runtime.getRuntime().exec("top -n 1");
            is = process.getInputStream();
            isr = new InputStreamReader(is);
            brStat = new BufferedReader(isr);

            String line = brStat.readLine();
            if (line != null) {
                while (!line.toLowerCase().contains("cpu")) {
                    line = brStat.readLine();
                }
                line = line.substring(line.indexOf(":") + 1);
                String useAvg[] = line.split(",");
                for (String item : useAvg) {
                    if (item.toLowerCase().contains("id")) {
                        usage = new Double(item.substring(0, item.indexOf("%")));
                        usage = 100 - usage;
                        break;
                    }
                }
            }

            return usage;

        } catch (IOException e) {
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
            freeResource(is, isr, brStat);
            return 1;
        } finally {
            freeResource(is, isr, brStat);
        }
    }

    private static void freeResource(InputStream is, InputStreamReader isr, BufferedReader br) {
        try {
            if (is != null) {
                is.close();
            }
            if (isr != null) {
                isr.close();
            }
            if (br != null) {
                br.close();
            }
        } catch (IOException e) {
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
        }
    }

    /**
     * 获得CPU使用率.
     *
     * @return 返回cpu使用率
     * @author GuoHuang
     */
    private static double getCpuRatioForWindows() {
        try {
            String procCmd = System
                    .getenv("windir") + "\\system32\\wbem\\wmic.exe process get Caption,CommandLine,KernelModeTime,ReadOperationCount,ThreadCount,UserModeTime,WriteOperationCount";

            // 取进程信息
            double[] c0 = readCpu(Runtime.getRuntime().exec(procCmd));
            Thread.sleep(CPUTIME);
            double[] c1 = readCpu(Runtime.getRuntime().exec(procCmd));
            if (c0 != null && c1 != null) {
                double idletime = c1[0] - c0[0];
                double busytime = c1[1] - c0[1];
                return PERCENT * (busytime) / (busytime + idletime);
            } else {
                return 0;
            }
        } catch (Exception e) {
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 读取CPU信息.
     *
     * @param proc
     * @return
     * @author GuoHuang
     */
    private static double[] readCpu(final Process proc) {
        double[] retn = new double[2];
        try {
            proc.getOutputStream().close();
            InputStreamReader ir = new InputStreamReader(proc.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line = input.readLine();
            if (line == null || line.length() < FAULTLENGTH) {
                return null;
            }
            int capidx = line.indexOf("Caption");
            int cmdidx = line.indexOf("CommandLine");
            int rocidx = line.indexOf("ReadOperationCount");
            int umtidx = line.indexOf("UserModeTime");
            int kmtidx = line.indexOf("KernelModeTime");
            int wocidx = line.indexOf("WriteOperationCount");
            double idletime = 0;
            double kneltime = 0;
            double usertime = 0;
            while ((line = input.readLine()) != null) {
                if (line.length() < wocidx) {
                    continue;
                }
                // 字段出现顺序：Caption,CommandLine,KernelModeTime,ReadOperationCount,
                // ThreadCount,UserModeTime,WriteOperation
                String caption = line.substring(capidx, cmdidx - 1).trim();
                String cmd = line.substring(cmdidx, kmtidx - 1).trim();
                if (cmd.contains("wmic.exe")) {
                    continue;
                }
                String s1 = line.substring(kmtidx, rocidx - 1).trim();
                String s2 = line.substring(umtidx, wocidx - 1).trim();
                if (caption.equals("System Idle Process") || caption.equals("System")) {
                    if (s1.length() > 0) {
                        idletime += Long.valueOf(s1);
                    }
                    if (s2.length() > 0) {
                        idletime += Long.valueOf(s2);
                    }
                    continue;
                }
                if (s1.length() > 0) {
                    kneltime += Long.valueOf(s1);
                }
                if (s2.length() > 0) {
                    usertime += Long.valueOf(s2);
                }
            }
            retn[0] = idletime;
            retn[1] = kneltime + usertime;
            return retn;
        } catch (Exception e) {
            log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
            log.error("Details: " + e.getMessage());
        } finally {
            try {
                proc.getInputStream().close();
            } catch (Exception e) {
                log.error(EnvPropertyConfig.getContextProperty("env.setting.server.error.00001013"));
                log.error("Details: " + e.getMessage());
            }
        }
        return null;
    }

    private static double buildSystemLoadAverage() {
        // 系统平均负载在最后一分钟。
        //该系统平均负载的总和
        //可运行实体的数量排队到可用处理器
        //和可用的运行可运行实体的数量
        //处理器
        //平均一段时间。
        final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystem.getSystemLoadAverage() >= 0) {
            // systemLoadAverage n'existe qu'à partir du jdk 1.6
            return operatingSystem.getSystemLoadAverage();
        }
        return -1;
    }

    private static String buildJvmArguments() {
        final StringBuilder jvmArgs = new StringBuilder();
        for (final String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            jvmArgs.append(jvmArg).append('\n');
        }
        if (jvmArgs.length() > 0) {
            jvmArgs.deleteCharAt(jvmArgs.length() - 1);
        }
        return jvmArgs.toString();
    }

    private static List<ThreadInformations> buildThreadInformationsList() {
        final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        final Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        final List<Thread> threads = new ArrayList<>(stackTraces.keySet());

        // 如果 "1.6.0_01".compareTo(Parameters.JAVA_VERSION) > 0;
        // 我们恢复线程的堆栈跟踪没有绕过漏洞6434648
        //1.6.0_01之前
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6434648
        //除了当前线程不同而得到它的堆栈跟踪
        //如果没有错误
        // threads = getThreadsFromThreadGroups();
        // final Thread currentThread = Thread.currentThread();
        // stackTraces = Collections.singletonMap(currentThread,
        // currentThread.getStackTrace());

        final boolean cpuTimeEnabled = threadBean.isThreadCpuTimeSupported() && threadBean.isThreadCpuTimeEnabled();
        final long[] deadlockedThreads = getDeadlockedThreads(threadBean);
        final List<ThreadInformations> threadInfosList = new ArrayList<>(threads.size());
        // 检索这里，因为可能有超过20,000的线程
        final String hostAddress = Parameters.getHostAddress();
        for (final Thread thread : threads) {
            final StackTraceElement[] stackTraceElements = stackTraces.get(thread);
            final List<StackTraceElement> stackTraceElementList = stackTraceElements == null ? null : new ArrayList<>(
                    Arrays.asList(stackTraceElements));
            final long cpuTimeMillis;
            final long userTimeMillis;
            if (cpuTimeEnabled) {
                cpuTimeMillis = threadBean.getThreadCpuTime(thread.getId()) / 1000000;
                userTimeMillis = threadBean.getThreadUserTime(thread.getId()) / 1000000;
            } else {
                cpuTimeMillis = -1;
                userTimeMillis = -1;
            }
            final boolean deadlocked = deadlockedThreads != null && Arrays
                    .binarySearch(deadlockedThreads, thread.getId()) >= 0;
            // stackTraceElementList ArrayList是不能修改的列表
            //对于可读性XML
            threadInfosList.add(new ThreadInformations(thread, stackTraceElementList, cpuTimeMillis, userTimeMillis,
                    deadlocked, hostAddress));
        }
        // 我们回到ArrayList和不修改的列表为XML可读性
        //通过XStream的
        return threadInfosList;
    }

    private static List<Thread> getThreadsFromThreadGroups() {
        ThreadGroup group = Thread.currentThread().getThreadGroup(); // NOPMD
        while (group.getParent() != null) {
            group = group.getParent();
        }
        final Thread[] threadsArray = new Thread[group.activeCount()];
        group.enumerate(threadsArray, true);
        return Arrays.asList(threadsArray);
    }

    private static long[] getDeadlockedThreads(ThreadMXBean threadBean) {
        final long[] deadlockedThreads;
        if (threadBean.isSynchronizerUsageSupported()) {
            deadlockedThreads = threadBean.findDeadlockedThreads();
        } else {
            deadlockedThreads = threadBean.findMonitorDeadlockedThreads();
        }
        if (deadlockedThreads != null) {
            Arrays.sort(deadlockedThreads);
        }
        return deadlockedThreads;
    }

    private static String buildDataBaseVersion() {
        if (Parameters.isNoDatabase()) {
            return null;
        }
        final StringBuilder result = new StringBuilder();
        try {
            //我们首先看到是否使用JDBC驱动程序
            //因为如果没有数据源将引发异常
            if (Parameters.getLastConnectUrl() != null) {
                final Connection connection = DriverManager
                        .getConnection(Parameters.getLastConnectUrl(), Parameters.getLastConnectInfo());
                connection.setAutoCommit(false);
                try {
                    appendDataBaseVersion(result, connection);
                } finally {
                    // rollback inutile ici car on ne fait que lire les
                    // meta-data (+ cf issue 38)
                    connection.close();
                }
                return result.toString();
            }

            // 我们正在寻找一个datasource与initialcontext显示名称
            //版本/和BDD +名称和版本的JDBC驱动程序
            ///（名称中查找JNDI是datasource属
            // JDBC / XXX是一名datasource）标准图<字符串>，datasource datasources = jdbcwrapp
            final Map<String, DataSource> dataSources = JdbcWrapper.getJndiAndSpringDataSources();
            for (final Map.Entry<String, DataSource> entry : dataSources.entrySet()) {
                final String name = entry.getKey();
                final DataSource dataSource = entry.getValue();
                // on ne doit pas changer autoCommit pour la connection d'une
                // DataSource
                // (ou alors il faudrait remettre l'autoCommit après, issue 233)
                // connection.setAutoCommit(false);
                try (Connection connection = dataSource.getConnection()) {
                    if (result.length() > 0) {
                        result.append("\n\n");
                    }
                    result.append(name).append(":\n");
                    appendDataBaseVersion(result, connection);
                }
                // rollback inutile ici car on ne fait que lire les
                // meta-data (+ cf issue 38)

            }
        } catch (final Exception e) {
            result.append(e.toString());
        }
        if (result.length() > 0) {
            return result.toString();
        }
        return null;
    }

    private static void appendDataBaseVersion(StringBuilder result, Connection connection) throws SQLException {
        final DatabaseMetaData metaData = connection.getMetaData();
        // Sécurité: pour l'instant on n'indique pas metaData.getUserName()
        result.append(metaData.getURL()).append('\n');
        result.append(metaData.getDatabaseProductName()).append(", ").append(metaData.getDatabaseProductVersion())
                .append('\n');
        result.append("Driver JDBC:\n").append(metaData.getDriverName()).append(", ")
                .append(metaData.getDriverVersion());
    }

    private static String buildDataSourceDetails() {
        final Map<String, Map<String, Object>> dataSourcesProperties = JdbcWrapper.getBasicDataSourceProperties();
        final StringBuilder sb = new StringBuilder();
        for (final Map.Entry<String, Map<String, Object>> entry : dataSourcesProperties.entrySet()) {
            final Map<String, Object> dataSourceProperties = entry.getValue();
            if (dataSourceProperties.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            final String name = entry.getKey();
            if (name != null) {
                sb.append(name).append(":\n");
            }
            for (final Map.Entry<String, Object> propertyEntry : dataSourceProperties.entrySet()) {
                sb.append(propertyEntry.getKey()).append(" = ").append(propertyEntry.getValue()).append('\n');
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    private static List<String> buildDependenciesList(ServletContext servletContext) {
        final String directoryTomcat = "/WEB-INF/lib/";
        final String directoryNotTomcat = "/lib/";
        String directory;

        Set<String> dependencies;
        try {
            dependencies = servletContext.getResourcePaths(directoryTomcat);
            directory = directoryTomcat;
        } catch (final Exception e) {
            // Tomcat 8 can throw
            // "IllegalStateException: The resources may not be accessed if they are not currently started"
            // for some ServletContext states (issue 415)
            return Collections.emptyList();
        }
        if (dependencies == null || dependencies.isEmpty()) {
            try {
                dependencies = servletContext.getResourcePaths(directoryNotTomcat);
                directory = directoryNotTomcat;
            } catch (final Exception e) {
                // Tomcat 8 can throw
                // "IllegalStateException: The resources may not be accessed if they are not currently started"
                // for some ServletContext states (issue 415)
                return Collections.emptyList();
            }
        }
        if (dependencies == null || dependencies.isEmpty()) {
            return Collections.emptyList();
        }
        final List<String> result = new ArrayList<>(dependencies.size());
        for (final String dependency : dependencies) {
            result.add(dependency.substring(directory.length()));
        }
        Collections.sort(result);
        return result;
    }

    private static boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
        // on ne teste pas operatingSystem instanceof
        // com.sun.management.OperatingSystemMXBean
        // car le package com.sun n'existe à priori pas sur une jvm tierce
        final String className = operatingSystem.getClass().getName();
        return "com.sun.management.OperatingSystem".equals(className) || "com.sun.management.UnixOperatingSystem"
                .equals(className)
                // sun.management.OperatingSystemImpl pour java 8
                || "sun.management.OperatingSystemImpl".equals(className);
    }

    private static boolean isSunUnixMBean(OperatingSystemMXBean operatingSystem) {
        for (final Class<?> inter : operatingSystem.getClass().getInterfaces()) {
            if ("com.sun.management.UnixOperatingSystemMXBean".equals(inter.getName())) {
                return true;
            }
        }
        return false;
    }

    public MemoryInformations getMemoryInformations() {
        return memoryInformations;
    }

    public List<TomcatInformations> getTomcatInformationsList() {
        return tomcatInformationsList;
    }

    public int getSessionCount() {
        return sessionCount;
    }

    public long getSessionAgeSum() {
        return sessionAgeSum;
    }

    public long getSessionMeanAgeInMinutes() {
        if (sessionCount > 0) {
            return sessionAgeSum / sessionCount / 60000;
        }
        return -1;
    }

    public int getActiveThreadCount() {
        return activeThreadCount;
    }

    public int getUsedConnectionCount() {
        return usedConnectionCount;
    }

    public int getActiveConnectionCount() {
        return activeConnectionCount;
    }

    public int getMaxConnectionCount() {
        return maxConnectionCount;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public double getUsedConnectionPercentage() {
        if (maxConnectionCount > 0) {
            return 100d * usedConnectionCount / maxConnectionCount;
        }
        return -1d;
    }

    public long getProcessCpuTimeMillis() {
        return processCpuTimeMillis;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public double getSystemCpuLoad() {
        return systemCpuLoad;
    }

    public long getUnixOpenFileDescriptorCount() {
        return unixOpenFileDescriptorCount;
    }

    public long getUnixMaxFileDescriptorCount() {
        return unixMaxFileDescriptorCount;
    }

    public double getUnixOpenFileDescriptorPercentage() {
        if (unixOpenFileDescriptorCount >= 0) {
            return 100d * unixOpenFileDescriptorCount / unixMaxFileDescriptorCount;
        }
        return -1d;
    }

    public String getHost() {
        return host;
    }

    public String getOS() {
        return os;
    }

    public int getAvailableProcessors() {
        return availableProcessors;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getJvmVersion() {
        return jvmVersion;
    }

    public String getPID() {
        return pid;
    }

    public String getServerInfo() {
        return serverInfo;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getContextDisplayName() {
        return contextDisplayName;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getJvmArguments() {
        return jvmArguments;
    }

    public long getFreeDiskSpaceInTemp() {
        return freeDiskSpaceInTemp;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getPeakThreadCount() {
        return peakThreadCount;
    }

    public long getTotalStartedThreadCount() {
        return totalStartedThreadCount;
    }

    public String getDataBaseVersion() {
        return dataBaseVersion;
    }

    public String getDataSourceDetails() {
        return dataSourceDetails;
    }

    public List<ThreadInformations> getThreadInformationsList() {
        // on trie sur demande (si affichage)
        final List<ThreadInformations> result = new ArrayList<>(threadInformationsList);
        Collections.sort(result, new ThreadInformationsComparator());
        return Collections.unmodifiableList(result);
    }

    public List<JobInformations> getJobInformationsList() {
        // on trie sur demande (si affichage)
        final List<JobInformations> result = new ArrayList<>(jobInformationsList);
        Collections.sort(result, new JobInformationsComparator());
        return Collections.unmodifiableList(result);
    }

    public int getCurrentlyExecutingJobCount() {
        int result = 0;
        for (final JobInformations jobInformations : jobInformationsList) {
            if (jobInformations.isCurrentlyExecuting()) {
                result++;
            }
        }
        return result;
    }

    public boolean isDependenciesEnabled() {
        return dependenciesList != null && !dependenciesList.isEmpty();
    }

    public List<String> getDependenciesList() {
        if (dependenciesList != null) {
            return Collections.unmodifiableList(dependenciesList);
        }
        return Collections.emptyList();
    }

    public String getDependencies() {
        if (!isDependenciesEnabled()) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final String dependency : getDependenciesList()) {
            if (dependency.endsWith(".jar") || dependency.endsWith(".JAR")) {
                sb.append(dependency);
                sb.append(",\n");
            }
        }
        if (sb.length() >= 2) {
            sb.delete(sb.length() - 2, sb.length());
        }
        return sb.toString();
    }

    public boolean isStackTraceEnabled() {
        for (final ThreadInformations threadInformations : threadInformationsList) {
            final List<StackTraceElement> stackTrace = threadInformations.getStackTrace();
            if (stackTrace != null && !stackTrace.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean isJobEnabled() {
        return jobInformationsList != null && !jobInformationsList.isEmpty();
    }


    /**
     * @return the double processCpuLoad
     */
    public double getProcessCpuLoad() {
        return processCpuLoad;
    }

    /**
     * @return the String sysVersion
     */
    public String getSysVersion() {
        return sysVersion;
    }

    /**
     * @return the String arc
     */
    public String getArc() {
        return arc;
    }


    /**
     * @return the int daemonThreadCount
     */
    public int getDaemonThreadCount() {
        return daemonThreadCount;
    }

    /**
     * @return the long currentThreadCpuTime
     */
    public long getCurrentThreadCpuTime() {
        return currentThreadCpuTime;
    }


    /**
     * @return the long currentThreadUserTime
     */
    public long getCurrentThreadUserTime() {
        return currentThreadUserTime;
    }


    /**
     * @return the String vmName
     */
    public String getVmName() {
        return vmName;
    }

    /**
     * @return the String vmVendor
     */
    public String getVmVendor() {
        return vmVendor;
    }

    /**
     * @return the String vmVersion
     */
    public String getVmVersion() {
        return vmVersion;
    }

    /**
     * @return the String classPath
     */
    public String getClassPath() {
        return classPath;
    }

    /**
     * @return the String libraryPath
     */
    public String getLibraryPath() {
        return libraryPath;
    }

    /**
     * @return the String compliationName
     */
    public String getCompliationName() {
        return compliationName;
    }


    /**
     * @return the Logger log
     */
    public static Logger getLog() {
        return log;
    }

    /**
     * @return the int CPUTIME
     */
    public static int getCputime() {
        return CPUTIME;
    }

    /**
     * @return the int PERCENT
     */
    public static int getPercent() {
        return PERCENT;
    }

    /**
     * @return the int FAULTLENGTH
     */
    public static int getFaultlength() {
        return FAULTLENGTH;
    }

    /**
     * @return the double HIGH_USAGE_THRESHOLD_IN_PERCENTS
     */
    public static double getHighUsageThresholdInPercents() {
        return HIGH_USAGE_THRESHOLD_IN_PERCENTS;
    }

    /**
     * @return the long serialVersionUID
     */
    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /**
     * @return the boolean SYSTEM_CPU_LOAD_ENABLED
     */
    public static boolean isSystemCpuLoadEnabled() {
        return SYSTEM_CPU_LOAD_ENABLED;
    }

    /**
     * @return the boolean localWebXmlExists
     */
    public static boolean isLocalWebXmlExists() {
        return localWebXmlExists;
    }

    /**
     * @return the boolean localPomXmlExists
     */
    public static boolean isLocalPomXmlExists() {
        return localPomXmlExists;
    }

    /**
     * @return the double beforeCpuTime
     */
    public double getBeforeCpuTime() {
        return beforeCpuTime;
    }

    /**
     * @return the double beforeCpuUpTime
     */
    public double getBeforeCpuUpTime() {
        return beforeCpuUpTime;
    }

    /**
     * @return the String os
     */
    public String getOs() {
        return os;
    }

    /**
     * @return the long totalCompliationTime
     */
    public long getTotalCompliationTime() {
        return totalCompliationTime;
    }

    /**
     * @return the String pid
     */
    public String getPid() {
        return pid;
    }

    /**
     * @return the boolean webXmlExists
     */
    public boolean isWebXmlExists() {
        return webXmlExists;
    }

    /**
     * @return the boolean pomXmlExists
     */
    public boolean isPomXmlExists() {
        return pomXmlExists;
    }

    /**
     * @return the JavaInformations JavaInfo
     */
    public static JavaInformations getJavaInfo() {
        return JavaInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass()
                .getSimpleName() + "[pid=" + getPID() + ", host=" + getHost() + ", javaVersion=" + getJavaVersion() + ", serverInfo=" + getServerInfo() + ']';
    }
}
