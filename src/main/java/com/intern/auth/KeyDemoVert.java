package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authorization.AuthorizationProvider;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.authorization.KeycloakAuthorization;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class KeyDemoVert extends AbstractVerticle {
    @Override
    public void start() throws Exception {
        JsonObject keyJson = config().getJsonObject("auth").getJsonObject("keycloak");
        JsonObject gitJson = config().getJsonObject("auth").getJsonObject("github");
        String host = config().getString("host");
        int port = config().getInteger("port");
        String baseURL = "http://"+host+":"+port;
        Router router = Router.router(vertx);
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        OAuth2Auth keyAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, keyJson);
        OAuth2Auth gitAuth = GithubAuth.create(vertx, gitJson.getString("clientId"), gitJson.getString("clientSecret"));
        OAuth2AuthHandler gitAuthHandler = OAuth2AuthHandler.create(vertx, gitAuth, baseURL+"/callbac").setupCallback(router.route("/callbac")).withScope("user");
        OAuth2AuthHandler keyAuthHandler = OAuth2AuthHandler.create(vertx, keyAuth, baseURL+"/callback").setupCallback(router.route("/callback"));
        
        router.route("/git/*").handler(gitAuthHandler);
        router.get("/git/hi").handler(ctx->ctx.end("Hi from Github!"));
        router.get("/git/user").handler(ctx->{
            gitAuth.userInfo(ctx.user())
                .onSuccess(user->ctx.end(user.toString()))
                .onFailure(ar->ctx.end(ar.getMessage()));
        });
        router.get("/git/logout").handler(ctx->{
            gitAuth.revoke(ctx.user())
                .onSuccess(v->System.out.print("Succes"))
                .onFailure(System.out::println);
            ctx.redirect(gitAuth.endSessionURL(ctx.user(), new JsonObject().put("redirect_uri", baseURL+"/hi")));
            ctx.session().destroy();
        });

        router.route("/key/*").handler(keyAuthHandler);
        router.get("/key/hi").handler(ctx->ctx.end("Hi, from Keycloak!"));
        router.get("/key/admin").handler(ctx->{
            AuthorizationProvider authz = KeycloakAuthorization.create();
            authz.getAuthorizations(ctx.user())
            .onSuccess(ar->{
                if(RoleBasedAuthorization.create("admin")
                    .setResource("vertx")
                    .match(ctx.user())){
                        ctx.end("<h1>Hi, boss!</h1>");
                } else{
                    ctx.reroute("/hi");
                }
            })
            .onFailure(ar->ctx.redirect(baseURL + "/hi?msg=" + ar.getMessage()));
        });
        router.get("/key/user").handler(ctx->{
            keyAuth.userInfo(ctx.user())
                .onSuccess(user->ctx.end(user.toString()))
                .onFailure(ar->ctx.end(ar.getMessage()));
        });
        router.get("/key/logout").handler(ctx->{
            ctx.redirect(keyAuth.endSessionURL(ctx.user(), new JsonObject().put("redirect_uri", baseURL+"/hi")));
            ctx.session().destroy();
        });

        router.get("/hi").handler(ctx->{
            String msg = ctx.request().getParam("msg");
            ctx.response().setChunked(true);
            ctx.response().write("<h1>Hi, buddy!</h1>");
            if(msg!=null){
                ctx.response().write("Message: " + msg);
            }
            ctx.end();
        });
        vertx.createHttpServer().requestHandler(router).listen(port, host);
    }
}