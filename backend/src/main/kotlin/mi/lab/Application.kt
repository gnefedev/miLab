package mi.lab

import mi.lab.config.TasksConfiguration
import mi.lab.config.WebFluxConfiguration
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

fun main() {
    SpringApplication.run(Application::class.java)
}

@SpringBootApplication
@ComponentScan(excludeFilters = [ComponentScan.Filter(Configuration::class)])
@Import(TasksConfiguration::class, WebFluxConfiguration::class)
class Application
