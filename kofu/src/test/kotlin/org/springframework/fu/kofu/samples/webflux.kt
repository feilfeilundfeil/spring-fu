package org.springframework.fu.kofu.samples

import org.springframework.boot.WebApplicationType
import org.springframework.boot.logging.LogLevel
import org.springframework.context.event.ContextStartedEvent
import org.springframework.fu.kofu.application
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.mongo.reactiveMongodb
import org.springframework.fu.kofu.webflux.cors
import org.springframework.fu.kofu.webflux.mustache
import org.springframework.fu.kofu.webflux.webClient
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.buildAndAwait
import org.springframework.web.reactive.function.server.coRouter
import org.springframework.web.reactive.function.server.router
import reactor.core.publisher.Mono

private fun webFluxRouter() {
	application(WebApplicationType.REACTIVE) {
		webFlux {
			router {
				val htmlHandler = ref<ReactiveHtmlHandler>()
				val apiHandler = ref<ReactiveApiHandler>()
				GET("/", htmlHandler::blog)
				GET("/article/{id}", htmlHandler::article)
				"/api".nest {
					GET("/", apiHandler::list)
					POST("/", apiHandler::create)
					PUT("/{id}", apiHandler::update)
					DELETE("/{id}", apiHandler::delete)
				}
			}
		}
	}
}

private fun webFluxIncludeRouter() {
	fun routes(htmlHandler: ReactiveHtmlHandler, apiHandler: ReactiveApiHandler) = router {
		GET("/", htmlHandler::blog)
		GET("/article/{id}", htmlHandler::article)
		"/api".nest {
			GET("/", apiHandler::list)
			POST("/", apiHandler::create)
			PUT("/{id}", apiHandler::update)
			DELETE("/{id}", apiHandler::delete)
		}
	}
	application(WebApplicationType.REACTIVE) {
		beans {
			bean(::routes)
		}
	}
}

private fun webFluxCustomEngine() {
	fun routes(htmlHandler: ReactiveHtmlHandler, apiHandler: ReactiveApiHandler) = router {
		GET("/", htmlHandler::blog)
		GET("/article/{id}", htmlHandler::article)
		"/api".nest {
			GET("/", apiHandler::list)
			POST("/", apiHandler::create)
			PUT("/{id}", apiHandler::update)
			DELETE("/{id}", apiHandler::delete)
		}
	}
	application(WebApplicationType.REACTIVE) {
		beans {
			bean(::routes)
		}
		webFlux {
			engine = jetty()
		}
	}
}

private fun webFluxCoRouter() {
	application(WebApplicationType.REACTIVE) {
		beans {
			bean<CoHtmlHandler>()
			bean<CoApiHandler>()
		}
		webFlux {
			coRouter {
				val htmlHandler = ref<CoHtmlHandler>()
				val apiHandler = ref<CoApiHandler>()
				GET("/", htmlHandler::blog)
				GET("/article/{id}", htmlHandler::article)
				"/api".nest {
					GET("/", apiHandler::list)
					POST("/", apiHandler::create)
					PUT("/{id}", apiHandler::update)
					DELETE("/{id}", apiHandler::delete)
				}
			}
		}
	}
}

private fun webFluxIncludeCoRouter() {
	fun routes(htmlHandler: CoHtmlHandler, apiHandler: CoApiHandler) = coRouter {
		GET("/", htmlHandler::blog)
		GET("/article/{id}", htmlHandler::article)
		"/api".nest {
			GET("/", apiHandler::list)
			POST("/", apiHandler::create)
			PUT("/{id}", apiHandler::update)
			DELETE("/{id}", apiHandler::delete)
		}
	}
	application(WebApplicationType.NONE) {
		beans {
			bean<CoHtmlHandler>()
			bean<CoApiHandler>()
			bean(::routes)
		}
		webFlux()
	}
}

private fun webFluxApplicationDsl() {
	fun routes(htmlHandler: CoHtmlHandler, apiHandler: CoApiHandler) = coRouter {
		GET("/", htmlHandler::blog)
		GET("/article/{id}", htmlHandler::article)
		"/api".nest {
			GET("/", apiHandler::list)
			POST("/", apiHandler::create)
			PUT("/{id}", apiHandler::update)
			DELETE("/{id}", apiHandler::delete)
		}
	}

	val dataConfiguration = configuration {
		beans {
			bean<UserRepository>()
			bean<ArticleRepository>()
		}
		reactiveMongodb {
			uri = "mongodb://myserver.com/foo"
		}
		listener<ContextStartedEvent> {
			ref<UserRepository>().init()
			ref<ArticleRepository>().init()
		}
	}

	val webConfiguration = configuration {
		beans {
			bean<ReactiveHtmlHandler>()
			bean<ReactiveApiHandler>()
			bean(::routes)
		}
		webFlux {
			port = if (profiles.contains("test")) 8181 else 8080
			cors {
				"example.com" { }
			}
			mustache()
			codecs {
				string()
				jackson()
			}
		}
		webClient {
			codecs {
				string()
				jackson()
			}
		}
	}

	val app = application(WebApplicationType.REACTIVE) {
		logging {
			level = LogLevel.INFO
			level("org.springframework", LogLevel.DEBUG)
		}
		configurationProperties<City>(prefix = "city")
		enable(dataConfiguration)
		enable(webConfiguration)
	}

	fun main(args: Array<String>) = app.run(profiles = "data, webflux")
}


private class ReactiveHtmlHandler(private val userRepository: UserRepository,
				  private val articleRepository: ArticleRepository) {

	fun blog(request: ServerRequest) = ServerResponse.ok().build()
	fun article(request: ServerRequest) = ServerResponse.ok().build()
}

interface ReactiveApiHandler {
	fun list(request: ServerRequest): Mono<ServerResponse>
	fun create(request: ServerRequest): Mono<ServerResponse>
	fun update(request: ServerRequest): Mono<ServerResponse>
	fun delete(request: ServerRequest): Mono<ServerResponse>
}

class CoHtmlHandler(
		private val userRepository: UserRepository,
		private val articleRepository: ArticleRepository) {

	suspend fun blog(request: ServerRequest) = ServerResponse.ok().buildAndAwait()
	suspend fun article(request: ServerRequest) = ServerResponse.ok().buildAndAwait()

}

class CoApiHandler {
	suspend fun list(request: ServerRequest) = ServerResponse.ok().buildAndAwait()
	suspend fun create(request: ServerRequest) = ServerResponse.ok().buildAndAwait()
	suspend fun update(request: ServerRequest) = ServerResponse.ok().buildAndAwait()
	suspend fun delete(request: ServerRequest) = ServerResponse.ok().buildAndAwait()

}
