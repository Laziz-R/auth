package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {

    JsonObject keyJson = config().getJsonObject("auth").getJsonObject("keycloak");
    JsonObject github = config().getJsonObject("auth").getJsonObject("github");

    OAuth2Auth authGit = GithubAuth.create(vertx, github.getString("clientId"), github.getString("clientSecret"));
    OAuth2AuthHandler authGitHandler = OAuth2AuthHandler.create(vertx, authGit);
    
    Router router = Router.router(vertx);
    router.route().handler(SessionHandler.create(SessionStore.create(vertx)));
    authGitHandler.setupCallback(router.route("/callback"));

    router.route("/git/*").handler(authGitHandler);
    router.get("/git/logout")
      .handler(ctx->{
        // authGit.endSessionURL(ctx.user());
        // ctx.session().destroy();
        authGit.revoke(ctx.user())
        .onSuccess(ar->ctx.response().end("Logged out!"))
        .onFailure(ar->ctx.response().end(ar.getMessage()));
    });
    router.get("/git/userinfo")
      .handler(ctx->{
        authGit.userInfo(ctx.user())
        .onSuccess(user->ctx.response().end(user.toString()))
        .onFailure(ar->ctx.response().end(ar.getMessage()));
    });


    router.route().handler(ctx->{
      ctx.response().end("Sorry, we have not this page.");
    });
    vertx.createHttpServer().requestHandler(router).listen(config().getInteger("port"),
        config().getString("host"));
  }
}
