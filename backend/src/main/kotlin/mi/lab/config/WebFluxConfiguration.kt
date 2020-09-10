package mi.lab.config

import mu.KLogging
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import javax.validation.ConstraintViolationException

@Configuration
class WebFluxConfiguration {
    private companion object : KLogging()

    @Configuration
    @Order(-2)
    class GlobalErrorHandler : ErrorWebExceptionHandler {
        override fun handle(serverWebExchange: ServerWebExchange, throwable: Throwable): Mono<Void> =
                if (throwable is ConstraintViolationException) {
                    toResponse(serverWebExchange, HttpStatus.BAD_REQUEST, throwable.message ?: "")
                } else {
                    logger.error(throwable) { "internal error" }
                    toResponse(serverWebExchange, HttpStatus.INTERNAL_SERVER_ERROR, "Unknown error")
                }

        private fun toResponse(serverWebExchange: ServerWebExchange, status: HttpStatus, message: String): Mono<Void> {
            serverWebExchange.response.statusCode = status
            serverWebExchange.response.headers.contentType = MediaType.TEXT_PLAIN
            val dataBuffer = serverWebExchange.response.bufferFactory().wrap(message.toByteArray())
            return serverWebExchange.response.writeWith(Mono.just(dataBuffer))
        }

    }
}
