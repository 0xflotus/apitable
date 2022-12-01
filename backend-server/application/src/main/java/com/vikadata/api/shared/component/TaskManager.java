package com.vikadata.api.shared.component;

import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

import com.vikadata.core.util.SpringContextHolder;

import org.springframework.stereotype.Component;

import static org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME;

/**
 * <p>
 * Asynchronous Thread Task Manager
 * </p>
 *
 * @author Shawn Deng
 */
@Component
@Slf4j
public class TaskManager {

    public static TaskManager me() {
        return SpringContextHolder.getBean(TaskManager.class);
    }

    public void execute(Runnable runnable) {
        SpringContextHolder.getBean(APPLICATION_TASK_EXECUTOR_BEAN_NAME, Executor.class).execute(runnable);
    }
}
