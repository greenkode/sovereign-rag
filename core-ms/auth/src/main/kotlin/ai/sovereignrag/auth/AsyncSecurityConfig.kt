package ai.sovereignrag.auth

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor
import java.util.concurrent.Executor

/**
 * Async Security Configuration
 *
 * Enables automatic propagation of Spring SecurityContext to background threads.
 * This ensures that tenant information (stored as principal in SecurityContext)
 * is available in @Async methods and CompletableFuture operations.
 *
 * How it works:
 * - SecurityContext is stored in ThreadLocal by default (only main thread)
 * - DelegatingSecurityContextAsyncTaskExecutor wraps the executor
 * - When a task is submitted, it captures current SecurityContext
 * - When task executes in background thread, SecurityContext is set there
 * - After task completes, SecurityContext is cleared automatically
 *
 * Usage:
 * - @Async methods automatically get SecurityContext propagated
 * - TenantContext.getCurrentTenant() works in background threads
 * - No manual context management needed
 */
@Configuration
@EnableAsync
class AsyncSecurityConfig {

    /**
     * Configure async task executor with SecurityContext propagation
     *
     * This executor is used by @Async methods and ensures SecurityContext
     * (including tenant ID) is available in background threads.
     */
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("async-security-")
        executor.initialize()

        // Wrap executor to propagate SecurityContext to async threads
        return DelegatingSecurityContextAsyncTaskExecutor(executor)
    }

    /**
     * General purpose executor for manual CompletableFuture operations
     *
     * Use this for manually creating background tasks that need SecurityContext:
     * CompletableFuture.supplyAsync({ ... }, securityContextExecutor)
     */
    @Bean(name = ["securityContextExecutor"])
    fun securityContextExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("security-executor-")
        executor.initialize()

        // Return wrapped executor that propagates SecurityContext
        return DelegatingSecurityContextExecutor(executor)
    }
}
