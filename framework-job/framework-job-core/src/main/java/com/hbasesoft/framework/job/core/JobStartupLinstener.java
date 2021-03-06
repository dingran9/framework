/**************************************************************************************** 
 Copyright © 2003-2012 hbasesoft Corporation. All rights reserved. Reproduction or       <br>
 transmission in whole or in part, in any form or by any means, electronic, mechanical <br>
 or otherwise, is prohibited without the prior written consent of the copyright owner. <br>
 ****************************************************************************************/
package com.hbasesoft.framework.job.core;

import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;

import com.dangdang.ddframe.job.api.dataflow.DataflowJob;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.config.JobCoreConfiguration;
import com.dangdang.ddframe.job.config.JobTypeConfiguration;
import com.dangdang.ddframe.job.config.dataflow.DataflowJobConfiguration;
import com.dangdang.ddframe.job.config.script.ScriptJobConfiguration;
import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
import com.dangdang.ddframe.job.event.JobEventConfiguration;
import com.dangdang.ddframe.job.event.rdb.JobEventRdbConfiguration;
import com.dangdang.ddframe.job.lite.api.JobScheduler;
import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
import com.dangdang.ddframe.job.reg.base.CoordinatorRegistryCenter;
import com.hbasesoft.framework.common.GlobalConstants;
import com.hbasesoft.framework.common.InitializationException;
import com.hbasesoft.framework.common.StartupListener;
import com.hbasesoft.framework.common.utils.PropertyHolder;
import com.hbasesoft.framework.common.utils.bean.BeanUtil;
import com.hbasesoft.framework.common.utils.logger.LoggerUtil;
import com.hbasesoft.framework.db.core.config.DbParam;
import com.hbasesoft.framework.db.core.utils.DataSourceUtil;
import com.hbasesoft.framework.job.core.annotation.Job;
import com.hbasesoft.framework.job.core.api.ScriptJob;
import com.hbasesoft.framework.job.core.event.JobEventJsonConfiguration;

/**
 * <Description> <br>
 * 
 * @author 王伟<br>
 * @version 1.0<br>
 * @taskId <br>
 * @CreateDate 2018年4月14日 <br>
 * @since V1.0<br>
 * @see com.hbasesoft.framework.job.core <br>
 */
public class JobStartupLinstener implements StartupListener {

    private final String[] packagesToScan = new String[] {
        "com.hbasesoft.*"
    };

    /**
     * Description: <br>
     * 
     * @author 王伟<br>
     * @taskId <br>
     * @param context <br>
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    @Override
    public void complete(ApplicationContext context) {

        // 未开启Job则不进行扫描
        if (!PropertyHolder.getBooleanProperty("job.enable", true)) {
            return;
        }

        try {
            final CoordinatorRegistryCenter regCenter = context.getBean(CoordinatorRegistryCenter.class);

            boolean enableEvent = PropertyHolder.getBooleanProperty("job.event.enable", true);
            JobEventConfiguration jobEventConfig = null;
            if (enableEvent) {
//                String datasourceName = PropertyHolder.getProperty("job.event.datasource", "master");
//                DataSource datasource = DataSourceUtil.registDataSource(datasourceName, new DbParam(datasourceName));
//                jobEventConfig = new JobEventRdbConfiguration(datasource);
                jobEventConfig = new JobEventJsonConfiguration();
            }

            for (String pack : packagesToScan) {
                if (StringUtils.isNotEmpty(pack)) {
                    Set<Class<?>> clazzSet = BeanUtil.getClasses(pack);
                    for (Class<?> clazz : clazzSet) {
                        if (clazz.isAnnotationPresent(Job.class)) {
                            Job job = AnnotationUtils.findAnnotation(clazz, Job.class);

                            String isJobEnable = job.enable();
                            isJobEnable = getPropery(isJobEnable);
                            if (!"true".equalsIgnoreCase(isJobEnable)) {
                                continue;
                            }

                            // Job名称
                            String name = getPropery(job.name());
                            if (StringUtils.isEmpty(name)) {
                                name = StringUtils.uncapitalize(clazz.getSimpleName());
                            }

                            // 分片大小
                            int shardingTotalCount = 1;

                            String shardingItemParameters = getPropery(job.shardingParam());
                            if (StringUtils.isNotEmpty(shardingItemParameters)) {
                                String[] params = StringUtils.split(shardingItemParameters, GlobalConstants.SPLITOR);
                                shardingTotalCount = params.length;
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; i < shardingTotalCount; i++) {
                                    sb.append(i).append(GlobalConstants.EQUAL_SPLITER).append(params[i]);
                                    if (i < shardingTotalCount - 1) {
                                        sb.append(GlobalConstants.SPLITOR);
                                    }
                                }
                                shardingItemParameters = sb.toString();
                            }

                            JobCoreConfiguration coreConfig = JobCoreConfiguration
                                .newBuilder(name, getPropery(job.cron()), shardingTotalCount)
                                .shardingItemParameters(shardingItemParameters).build();

                            JobTypeConfiguration cfg = null;
                            if (SimpleJob.class.isAssignableFrom(clazz)) {
                                cfg = new SimpleJobConfiguration(coreConfig, clazz.getCanonicalName());
                            }
                            else if (DataflowJob.class.isAssignableFrom(clazz)) {
                                cfg = new DataflowJobConfiguration(coreConfig, clazz.getCanonicalName(),
                                    job.streamingProcess());
                            }
                            else if (ScriptJob.class.isAssignableFrom(clazz)) {
                                ScriptJob scriptJob = (ScriptJob) clazz.newInstance();
                                cfg = new ScriptJobConfiguration(coreConfig, scriptJob.loadScript());
                            }

                            if (cfg != null) {
                                JobScheduler jobScheduler = jobEventConfig == null
                                    ? new JobScheduler(regCenter, LiteJobConfiguration.newBuilder(cfg).build())
                                    : new JobScheduler(regCenter, LiteJobConfiguration.newBuilder(cfg).build(),
                                        jobEventConfig);
                                jobScheduler.init();
                                LoggerUtil.info("    success create job [{0}] with name {1}", clazz.getName(), name);
                            }

                        }
                    }
                }
            }
        }
        catch (Exception e) {
            throw new InitializationException(e);
        }

    }

    private static final String getPropery(String propery) {
        if (StringUtils.isNotEmpty(propery) && propery.startsWith("${") && propery.endsWith("}")) {
            return PropertyHolder.getProperty(propery.substring(2, propery.length() - 1));
        }
        return propery;
    }

}
