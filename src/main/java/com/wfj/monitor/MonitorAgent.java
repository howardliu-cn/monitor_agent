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
}
