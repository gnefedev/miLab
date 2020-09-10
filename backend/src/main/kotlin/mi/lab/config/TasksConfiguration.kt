package mi.lab.config

import mi.lab.process.ProcessRunner
import mi.lab.tasks.FactorialTaskFactory
import mi.lab.tasks.TaskFactoryCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TasksConfiguration(
        private val processRunner: ProcessRunner,
        private val taskFactoryCache: TaskFactoryCache
) {
    //TODO write BeanPostProcessor to wrap factories by annotation
    @Bean
    fun factorialTaskFactory() = taskFactoryCache.wrap(FactorialTaskFactory(processRunner))
}
