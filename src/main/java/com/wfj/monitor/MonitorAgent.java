package com.wfj.monitor;

import com.wfj.monitor.conf.EnvPropertyConfig;
import com.wfj.monitor.conf.SystemPropertyConfig;
import com.wfj.monitor.counter.SLACounter;
import com.wfj.monitor.transform.MonitoringTransformer;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;

import static com.wfj.monitor.common.Constant.IS_DEBUG;

/**
 * @Class Name MonitorAgent
 * @Author Jack
 * @Create In 2017年1月5日
 */
public class MonitorAgent {
    /**
     * 监控启动方法，完成初始化动作
     * 第一优先级
     * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param args
     * @param inst
     * @Methods Name premain
     * @Create In 2017年1月5日 By Jack
     */
    public static void premain(String args, Instrumentation inst) {
        EnvPropertyConfig.init();
        if (args == null || args.isEmpty()) {
            SystemPropertyConfig.init();
        } else {
            SystemPropertyConfig.init(args);
        }
        if (IS_DEBUG) {
            return;
        }
        SLACounter.init();
        inst.addTransformer(new MonitoringTransformer());
    }

    /**
     * 默认监控启动方法，完成初始化动作
     * 第二优先级
     * 使用参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param args void
     * @Methods Name premain
     * @Create In 2017年1月5日 By Jack
     */
    public static void premain(String args) {
    }

    /**
     * 动态代理注入启动方法，完成初始化动作
     * 需配合线程类型主程序使用
     * 使用非参数形式，如：-javaagent:xxxx=xxx配置JVM
     *
     * @param agentArguments
     * @param instrumentation
     * @throws UnmodifiableClassException void
     * @Methods Name agentmain
     * @Create In 2017年1月5日 By Jack
     */
    public static void agentmain(String agentArguments, Instrumentation instrumentation)
            throws UnmodifiableClassException {
    }
}
