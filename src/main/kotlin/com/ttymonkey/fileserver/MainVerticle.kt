package com.ttymonkey.fileserver

import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

class MainVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val server = vertx.createHttpServer()
        val router = createRouter()

        server.requestHandler(router).listen(8888)
        println("HTTP server started on port 8888")
    }

    private fun createRouter() = Router.router(vertx).apply {
        route("/files/:filename").coroutineHandler { context ->
            val fileName = context.request().getParam("filename")
            try {
                val buffer = vertx.fileSystem().readFile("files/$fileName").await()
                context.response().putHeader("Content-Type", "text/plain")
                    .end(buffer)
            } catch (e: Exception) {
                context.response().setStatusCode(404).end()
            }
        }
    }

    private fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
        handler { context ->
            launch(context.vertx().dispatcher()) {
                try {
                    fn(context)
                } catch (e: Exception) {
                    context.fail(e)
                }
            }
        }
    }
}
