package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class KeyDemoVert extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        JsonObject keyJson = config().getJsonObject("auth").getJsonObject("keycloak");
        String host = config().getString("host");
        int port = config().getInteger("port");

        Router router = Router.router(vertx);
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        OAuth2Auth keyAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, keyJson);
        OAuth2AuthHandler authHandler = OAuth2AuthHandler.create(vertx, keyAuth).setupCallback(router.route("/callback"));
        router.route("/key2/*").handler(authHandler);
        router.get("/key2/hi").handler(ctx->ctx.end("Hiiiiiii"));
        router.get("/key2/user").handler(ctx->{
            keyAuth.userInfo(ctx.user())
                .onSuccess(user->ctx.end(user.toString()))
                .onFailure(ar->ctx.end(ar.getMessage()));
        });

        router.get("/hi").handler(ctx->ctx.end("<h1>Hi, buddy!</h1>"));

        router.get("/key/*").handler(ctx->{
            if(ctx.user()==null){
                ctx.session().put("back", ctx.request().absoluteURI());
                String auth_url = keyAuth.authorizeURL(new JsonObject()
                    .put("redirect_uri", "http://"+host+":"+port+"/callback")
                    .put("scope", "profile"));
                ctx.redirect(auth_url);
            } else{
                if(ctx.user().expired()){
                    keyAuth.refresh(ctx.user())
                        .onSuccess(user->ctx.setUser(user));
                }
                ctx.next();
            }
        });
        router.get("/key/hi").handler(ctx->ctx.end("<h1>Hi, boss!</h1>"));

        router.get("/callback").handler(ctx->{
            JsonObject tokenConfig = new JsonObject()
                .put("code", ctx.request().getParam("code")) 
                .put("redirect_uri", ctx.session().get("back"));
            keyAuth.authenticate(tokenConfig)
                .onComplete(ar->{
                    if(ar.succeeded()){
                        ctx.setUser(ar.result());
                    } else{
                        ctx.end(ar.cause().getMessage() + "\n" + ctx.session().get("back"));
                    }
                });
        });
        vertx.createHttpServer().requestHandler(router).listen(port, host);
    }
}