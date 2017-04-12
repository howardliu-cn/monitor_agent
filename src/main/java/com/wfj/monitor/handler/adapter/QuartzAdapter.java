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
package com.wfj.monitor.handler.adapter;

import com.wfj.monitor.conf.Parameter;
import com.wfj.monitor.conf.Parameters;
import com.wfj.monitor.dto.JobInformations;
import com.wfj.monitor.handler.listener.JobGlobalListener;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Classe permettant de fournir une API adaptée aux différentes versions de Quartz.<br/>
 * L'implémentation par défaut est adaptée à Quartz avant la version 2.<br/>
 * Une autre implémentation avec la même API sera fournie et sera adaptée à la version 2 et aux suivantes.
 *
 * @author Emeric Vernat
 */
public class QuartzAdapter {
    private static final boolean QUARTZ_2 = isQuartz2();
    private static final QuartzAdapter SINGLETON = createSingleton();

    protected QuartzAdapter() {
        super();
    }

    public static QuartzAdapter getSingleton() {
        return SINGLETON;
    }

    private static boolean isQuartz2() {
        try {
            Class.forName("org.quartz.JobKey");
            return true;
        } catch (final ClassNotFoundException e) {
            return false;
        }
    }

    private static QuartzAdapter createSingleton() {
        if (QUARTZ_2) {
            try {
                return (QuartzAdapter) Class.forName("com.wfj.netty.monitor.handler.adapter.QuartzAdapter")
                        .newInstance();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        }
        return new QuartzAdapter();
    }

    public String getJobName(JobDetail jobDetail) {
        return jobDetail.getName();
    }

    public String getJobGroup(JobDetail jobDetail) {
        return jobDetail.getGroup();
    }

    public String getJobFullName(JobDetail jobDetail) {
        return getJobGroup(jobDetail) + '.' + getJobName(jobDetail);
    }

    public String getJobDescription(JobDetail jobDetail) {
        return jobDetail.getDescription();
    }

    public Class<?> getJobClass(JobDetail jobDetail) {
        return jobDetail.getJobClass();
    }

    public Date getTriggerPreviousFireTime(Trigger trigger) {
        return trigger.getPreviousFireTime();
    }

    public Date getTriggerNextFireTime(Trigger trigger) {
        return trigger.getNextFireTime();
    }

    public String getCronTriggerExpression(CronTrigger trigger) {
        // getCronExpression gives a PMD false+
        return trigger.getCronExpression(); // NOPMD
    }

    public long getSimpleTriggerRepeatInterval(SimpleTrigger trigger) {
        return trigger.getRepeatInterval(); // NOPMD
    }

    public JobDetail getContextJobDetail(JobExecutionContext context) {
        return context.getJobDetail();
    }

    public Date getContextFireTime(JobExecutionContext context) {
        return context.getFireTime();
    }

    public void addGlobalJobListener(JobListener jobGlobalListener) throws SchedulerException {
        final Scheduler defaultScheduler;
        if (Boolean.parseBoolean(
                Parameters.getParameter(Parameter.QUARTZ_DEFAULT_LISTENER_DISABLED))) {
            defaultScheduler = null;
//			LOG.debug("Initialization of Quartz default listener has been disabled");
        } else {
            defaultScheduler = StdSchedulerFactory.getDefaultScheduler();
            defaultScheduler.addGlobalJobListener(jobGlobalListener);
        }
        for (final Scheduler scheduler : JobInformations.getAllSchedulers()) {
            if (scheduler != defaultScheduler) {
                scheduler.addGlobalJobListener(jobGlobalListener);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void removeGlobalJobListener() throws SchedulerException {
        for (final Scheduler scheduler : JobInformations.getAllSchedulers()) {
            final List<JobListener> globalJobListeners = scheduler.getGlobalJobListeners();
            for (final JobListener jobListener : new ArrayList<JobListener>(globalJobListeners)) {
                if (jobListener instanceof JobGlobalListener) {
                    try {
                        scheduler.removeGlobalJobListener(jobListener);
                    } catch (final NoSuchMethodError e1) {
                        // pour Quartz 1.7, 1.8 et +,
                        // cette méthode n'existe pas avant Quartz 1.6
                        try {
                            final Class<? extends Scheduler> schedulerClass = scheduler.getClass();
                            schedulerClass.getMethod("removeGlobalJobListener", String.class)
                                    .invoke(scheduler, jobListener.getName());
                        } catch (final Exception e2) {
                            throw new IllegalArgumentException(e2); // NOPMD
                        }
                    }
                }
            }
        }
    }

    public List<JobDetail> getAllJobsOfScheduler(Scheduler scheduler) throws SchedulerException {
        final List<JobDetail> result = new ArrayList<JobDetail>();
        for (final String jobGroupName : scheduler.getJobGroupNames()) {
            for (final String jobName : scheduler.getJobNames(jobGroupName)) {
                final JobDetail jobDetail;
                try {
                    jobDetail = scheduler.getJobDetail(jobName, jobGroupName);
                    // le job peut être terminé et supprimé depuis la ligne ci-dessus
                    if (jobDetail != null) {
                        result.add(jobDetail);
                    }
                } catch (final Exception e) {
                    // si les jobs sont persistés en base de données, il peut y avoir une exception
                    // dans getJobDetail, par exemple si la classe du job n'existe plus dans l'application
//					LOG.debug(e.toString(), e);
                }
            }
        }
        return result;
    }

    public List<Trigger> getTriggersOfJob(JobDetail jobDetail, Scheduler scheduler)
            throws SchedulerException {
        return Arrays.asList(scheduler.getTriggersOfJob(jobDetail.getName(), jobDetail.getGroup()));
    }

    public boolean isTriggerPaused(Trigger trigger, Scheduler scheduler) throws SchedulerException {
        return scheduler.getTriggerState(trigger.getName(),
                trigger.getGroup()) == Trigger.STATE_PAUSED;
    }

    public void pauseJob(JobDetail jobDetail, Scheduler scheduler) throws SchedulerException {
        scheduler.pauseJob(jobDetail.getName(), jobDetail.getGroup());
    }

    public void resumeJob(JobDetail jobDetail, Scheduler scheduler) throws SchedulerException {
        scheduler.resumeJob(jobDetail.getName(), jobDetail.getGroup());
    }
}
