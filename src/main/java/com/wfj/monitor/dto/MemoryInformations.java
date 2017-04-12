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

import com.wfj.monitor.util.I18N;
import com.wfj.monitor.util.MBeans;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.Serializable;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Informations systèmes sur la mémoire du serveur, sans code html de
 * présentation. L'état d'une instance est initialisé à son instanciation et non
 * mutable; il est donc de fait thread-safe. Cet état est celui d'une instance
 * de JVM java. Les instances sont sérialisables pour pouvoir être transmises au
 * serveur de collecte.
 * 
 * @author Emeric Vernat
 */
public class MemoryInformations implements Serializable {
	private static final long serialVersionUID = 3281861236369720876L;
	private static final String NEXT = ",\n";
	private static final String MO = " Mo";
	private static final Set<ObjectName> NIO_BUFFER_POOLS = new HashSet<ObjectName>();

	static {
		try {
			NIO_BUFFER_POOLS.addAll(new MBeans().getNioBufferPools());
		} catch (final MalformedObjectNameException e) {
			// LOG.debug(e.toString());
		}
	}

	private final long totalMemory;

	private final long freeMemory;
	// usedMemory est la mémoire utilisée du heap (voir aussi non heap dans
	// gestion mémoire)
	private final long usedMemory;
	// maxMemory est la mémoire maximum pour le heap (paramètre -Xmx1024m par
	// exemple)
	private final long maxMemory;
	// usedPermGen est la mémoire utilisée de "Perm Gen" (classes et les
	// instances de String "interned")
	private final long usedPermGen;
	// maxPermGen est la mémoire maximum pour "Perm Gen" (paramètre
	// -XX:MaxPermSize=128m par exemple)
	private final long maxPermGen;
	private final long usedNonHeapMemory;
	private final long usedBufferedMemory;
	private final int loadedClassesCount;
	private final long garbageCollectionTimeMillis;

	private final long usedPhysicalMemorySize;
	private final long usedSwapSpaceSize;
	private final long totalSwapSpaceSize;
	private final long freeSwapSpaceSize;
	private final long freePhysicalMemorySize;
	private final long totalPhysicalMemorySize;
	private final long committedVirtualMemorySize;
	
	private final MemoryUsage heapMemoryUsage;
	private final MemoryUsage nonHeapMemoryUsage;
	private final List<MemPoolInfo> memPoolInfos;
	private final List<GCInfo> gcInfos;
	
	private final String memoryDetails;

	public MemoryInformations() {
		super();
		totalMemory = Runtime.getRuntime().totalMemory();
		freeMemory = Runtime.getRuntime().freeMemory();
		usedMemory = totalMemory - freeMemory;
		maxMemory = Runtime.getRuntime().maxMemory();

		heapMemoryUsage = buildHeapMemoryUsage();
		nonHeapMemoryUsage = buildNonHeapMemoryUsage();
		memPoolInfos = buildMemPoolInfos();
		gcInfos = buildGCInfo();
		
		final MemoryPoolMXBean permGenMemoryPool = getPermGenMemoryPool();
		if (permGenMemoryPool != null) {
			final java.lang.management.MemoryUsage usage = permGenMemoryPool.getUsage();
			usedPermGen = usage.getUsed();
			maxPermGen = usage.getMax();
		} else {
			usedPermGen = -1;
			maxPermGen = -1;
		}
		usedNonHeapMemory = ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed();
		usedBufferedMemory = getUsedBufferMemory();
		loadedClassesCount = ManagementFactory.getClassLoadingMXBean().getLoadedClassCount();
		garbageCollectionTimeMillis = buildGarbageCollectionTimeMillis();

		final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
		committedVirtualMemorySize = getLongFromOperatingSystem(operatingSystem, "getCommittedVirtualMemorySize");
		
		if (isSunOsMBean(operatingSystem)) {
			
			freeSwapSpaceSize = getLongFromOperatingSystem(operatingSystem, "getFreeSwapSpaceSize");
			totalPhysicalMemorySize = getLongFromOperatingSystem(operatingSystem, "getTotalPhysicalMemorySize");
			freePhysicalMemorySize = getLongFromOperatingSystem(operatingSystem, "getFreePhysicalMemorySize");
			totalSwapSpaceSize = getLongFromOperatingSystem(operatingSystem, "getTotalSwapSpaceSize");
			
			usedPhysicalMemorySize = totalPhysicalMemorySize - freePhysicalMemorySize;
			usedSwapSpaceSize = totalSwapSpaceSize - freeSwapSpaceSize;
		} else {
			totalSwapSpaceSize = -1;
			freeSwapSpaceSize = -1;
			totalPhysicalMemorySize = -1;
			freePhysicalMemorySize = -1;
			usedPhysicalMemorySize = -1;
			usedSwapSpaceSize = -1;
		}

		memoryDetails = buildMemoryDetails();
	}
	
	private List<GCInfo> buildGCInfo(){
		List<GarbageCollectorMXBean> gcmList = ManagementFactory.getGarbageCollectorMXBeans();
		List<GCInfo> gcInfos = new ArrayList<GCInfo>();
		for (GarbageCollectorMXBean gcm : gcmList) {
			GCInfo gcInfo = new GCInfo();
			gcInfo.setName(gcm.getName());
			gcInfo.setCollectionCount(gcm.getCollectionCount());
			gcInfo.setCollectionTime(gcm.getCollectionTime());

			gcInfos.add(gcInfo);
		}
		
		return gcInfos;
	}
	
	private List<MemPoolInfo> buildMemPoolInfos(){
		
		List<MemoryPoolMXBean> mpmList = ManagementFactory.getMemoryPoolMXBeans();
		List<MemPoolInfo> mpisList = new ArrayList<MemPoolInfo>();
		for (MemoryPoolMXBean mpm : mpmList) {
			MemPoolInfo mpiInfo = new MemPoolInfo();
			MemoryUsage mu = new MemoryUsage();
			try {
				mpiInfo.setMemoryManagerNames(mpm.getObjectName().getKeyProperty("name"));
                mu.clone(mpm.getUsage());

				mpiInfo.setMemoryUsage(mu);
				mpisList.add(mpiInfo);
			} catch (Exception ignored) {
			}
		}
		
		return mpisList;
	}
	
	private MemoryUsage buildNonHeapMemoryUsage(){
		MemoryUsage mu = new MemoryUsage();
		try {
            mu.clone(ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage());
		} catch (Exception ignored) {
		}
		return mu;
	}
	
	private MemoryUsage buildHeapMemoryUsage(){
		MemoryUsage mu = new MemoryUsage();
		try {
            mu.clone(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
        } catch (Exception ignored) {
        }
		return mu;
	}

	private MemoryPoolMXBean getPermGenMemoryPool() {
		for (final MemoryPoolMXBean memoryPool : ManagementFactory.getMemoryPoolMXBeans()) {
			// name est "Perm Gen" ou "PS Perm Gen" (32 vs 64 bits ?)
			if (memoryPool.getName().endsWith("Perm Gen")) {
				return memoryPool;
			}
		}
		return null;
	}

	private long getUsedBufferMemory() {
		if (NIO_BUFFER_POOLS.isEmpty()) {
			return -1;
		}
		long result = 0;
		final MBeans mBeans = new MBeans();
		try {
			for (final ObjectName objectName : NIO_BUFFER_POOLS) {
				// adds direct and mapped buffers
				result += (Long) mBeans.getAttribute(objectName, "MemoryUsed");
			}
		} catch (final JMException e) {
			// n'est pas censé arriver
			throw new IllegalStateException(e);
		}
		return result;
	}

	private long buildGarbageCollectionTimeMillis() {
		long garbageCollectionTime = 0;
		for (final GarbageCollectorMXBean garbageCollector : ManagementFactory.getGarbageCollectorMXBeans()) {
			garbageCollectionTime += garbageCollector.getCollectionTime();
		}
		return garbageCollectionTime;
	}

	private String buildMemoryDetails() {
		final DecimalFormat integerFormat = I18N.createIntegerFormat();
		final String nonHeapMemory = "Non heap memory = " + integerFormat.format(usedNonHeapMemory / 1024 / 1024) + MO + " (Perm Gen, Code Cache)";
		// classes actuellement chargées
		final String classLoading = "Loaded classes = " + integerFormat.format(loadedClassesCount);
		final String gc = "Garbage collection time = " + integerFormat.format(garbageCollectionTimeMillis) + " ms";
		final OperatingSystemMXBean operatingSystem = ManagementFactory.getOperatingSystemMXBean();
		String osInfo = "";
		if (isSunOsMBean(operatingSystem)) {
			osInfo = "Process cpu time = " + integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getProcessCpuTime") / 1000000) + " ms,\nCommitted virtual memory = "
				+ integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getCommittedVirtualMemorySize") / 1024 / 1024) + MO + ",\nFree physical memory = "
				+ integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getFreePhysicalMemorySize") / 1024 / 1024) + MO + ",\nTotal physical memory = "
				+ integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getTotalPhysicalMemorySize") / 1024 / 1024) + MO + ",\nFree swap space = "
				+ integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getFreeSwapSpaceSize") / 1024 / 1024) + MO + ",\nTotal swap space = "
				+ integerFormat.format(getLongFromOperatingSystem(operatingSystem, "getTotalSwapSpaceSize") / 1024 / 1024) + MO;
		}
		if (usedBufferedMemory < 0) {
			return nonHeapMemory + NEXT + classLoading + NEXT + gc + NEXT + osInfo;
		}
		final String bufferedMemory = "Buffered memory = " + integerFormat.format(usedBufferedMemory / 1024 / 1024) + MO;
		return nonHeapMemory + NEXT + bufferedMemory + NEXT + classLoading + NEXT + gc + NEXT + osInfo;
	}

	private boolean isSunOsMBean(OperatingSystemMXBean operatingSystem) {
		// on ne teste pas operatingSystem instanceof
		// com.sun.management.OperatingSystemMXBean
		// car le package com.sun n'existe à priori pas sur une jvm tierce
		final String className = operatingSystem.getClass().getName();
		return "com.sun.management.OperatingSystem".equals(className) || "com.sun.management.UnixOperatingSystem".equals(className)
		// sun.management.OperatingSystemImpl pour java 8
			|| "sun.management.OperatingSystemImpl".equals(className);
	}

	public static long getLongFromOperatingSystem(OperatingSystemMXBean operatingSystem, String methodName) {
		return (Long) getFromOperatingSystem(operatingSystem, methodName);
	}

	public static double getDoubleFromOperatingSystem(OperatingSystemMXBean operatingSystem, String methodName) {
		return (Double) getFromOperatingSystem(operatingSystem, methodName);
	}

	public static Object getFromOperatingSystem(OperatingSystemMXBean operatingSystem, String methodName) {
		try {
			final Method method = operatingSystem.getClass().getMethod(methodName, (Class<?>[]) null);
			method.setAccessible(true);
			return method.invoke(operatingSystem, (Object[]) null);
		} catch (final InvocationTargetException e) {
			if (e.getCause() instanceof Error) {
				throw (Error) e.getCause();
			} else if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}
			throw new IllegalStateException(e.getCause());
		} catch (final NoSuchMethodException e) {
			throw new IllegalArgumentException(e);
		} catch (final IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public long getUsedMemory() {
		return usedMemory;
	}

	public long getMaxMemory() {
		return maxMemory;
	}

	public double getUsedMemoryPercentage() {
		return 100d * usedMemory / maxMemory;
	}

	public long getUsedPermGen() {
		return usedPermGen;
	}

	public long getMaxPermGen() {
		return maxPermGen;
	}

	public double getUsedPermGenPercentage() {
		if (usedPermGen > 0 && maxPermGen > 0) {
			return 100d * usedPermGen / maxPermGen;
		}
		return -1d;
	}

	public long getUsedNonHeapMemory() {
		return usedNonHeapMemory;
	}

	public long getUsedBufferedMemory() {
		return usedBufferedMemory;
	}

	public int getLoadedClassesCount() {
		return loadedClassesCount;
	}

	public long getGarbageCollectionTimeMillis() {
		return garbageCollectionTimeMillis;
	}

	public long getUsedPhysicalMemorySize() {
		return usedPhysicalMemorySize;
	}

	public long getUsedSwapSpaceSize() {
		return usedSwapSpaceSize;
	}

	public String getMemoryDetails() {
		return memoryDetails;
	}

	/**
	 * @Return the long totalMemory
	 */
	public long getTotalMemory() {
		return totalMemory;
	}

	/**
	 * @Return the long freeMemory
	 */
	public long getFreeMemory() {
		return freeMemory;
	}

	
	/**
	 * @Return the long totalSwapSpaceSize
	 */
	public long getTotalSwapSpaceSize() {
		return totalSwapSpaceSize;
	}

	/**
	 * @Return the long freeSwapSpaceSize
	 */
	public long getFreeSwapSpaceSize() {
		return freeSwapSpaceSize;
	}

	/**
	 * @Return the long freePhysicalMemorySize
	 */
	public long getFreePhysicalMemorySize() {
		return freePhysicalMemorySize;
	}

	/**
	 * @Return the long totalPhysicalMemorySize
	 */
	public long getTotalPhysicalMemorySize() {
		return totalPhysicalMemorySize;
	}

	/**
	 * @Return the long committedVirtualMemorySize
	 */
	public long getCommittedVirtualMemorySize() {
		return committedVirtualMemorySize;
	}
 
	
	/**
	 * @Return the MemoryUsage heapMemoryUsage
	 */
	public MemoryUsage getHeapMemoryUsage() {
		return heapMemoryUsage;
	}

	/**
	 * @Return the MemoryUsage nonHeapMemoryUsage
	 */
	public MemoryUsage getNonHeapMemoryUsage() {
		return nonHeapMemoryUsage;
	}

	/**
	 * @Return the List<MemPoolInfo> memPoolInfos
	 */
	public List<MemPoolInfo> getMemPoolInfos() {
		return memPoolInfos;
	}

	/**
	 * @Return the List<GCInfo> gcInfos
	 */
	public List<GCInfo> getGcInfos() {
		return gcInfos;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[usedMemory=" + getUsedMemory() + ", maxMemory=" + getMaxMemory() + ']';
	}
}
