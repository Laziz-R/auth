package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;

public class MainVerticle extends AbstractVerticle{
    @Override
    public void start() throws Exception {
        vertx.createHttpServer(new HttpServerOptions().setPort(8090).setHost("localhost"))
        .requestHandler(req->{
            req.response().end("Hello");
        })
        .listen();
    }
}
