package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.GithubAuth;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class MainVerticle extends AbstractVerticle {
  OAuth2Auth authGit, authKey;
  OAuth2AuthHandler authGitHandler, authKeyHandler;

  @Override
  public void start() throws Exception {
    JsonObject keyJson = config().getJsonObject("auth").getJsonObject("keycloak");
    JsonObject github = config().getJsonObject("auth").getJsonObject("github");

    authGit = GithubAuth.create(vertx, github.getString("clientId"), github.getString("clientSecret"));
    authKey = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, keyJson);
    Router router = Router.router(vertx);
    authGitHandler = OAuth2AuthHandler.create(vertx, authGit);
    authKeyHandler = OAuth2AuthHandler.create(vertx, authKey, "http://localhost:8090/callback");

    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
    authGitHandler.setupCallback(router.route("/callback")).withScope("user");
    authKeyHandler.setupCallback(router.route("/callback")).withScope("profile");

    router.route("/git/*").handler(authGitHandler);
    router.route("/key/*").handler(authKeyHandler);    

    router.get("/git/userinfo").handler(this::gitUserInfoHandler);
    router.get("/git/logout")  .handler(this::gitLogout);
    router.get("/key/userinfo").handler(this::keyUserInfoHandler);
    router.get("/key/logout")  .handler(this::keyLogout);

    router.route().handler(this::defaultHandler);
    vertx.createHttpServer().requestHandler(router).listen(config().getInteger("port"),
      config().getString("host"));
  }

  void gitUserInfoHandler(RoutingContext ctx){
    authGit.userInfo(ctx.user())
    .onSuccess(user->ctx.response().end(user.toString()))
    .onFailure(ar->ctx.response().end(ar.getMessage()));
  }
  void keyUserInfoHandler(RoutingContext ctx){
    authKey.userInfo(ctx.user())
    .onSuccess(user->ctx.response().end(user.toString()))
    .onFailure(ar->ctx.response().end(ar.getMessage()));
}
  void gitLogout(RoutingContext ctx){
    ctx.session().destroy();
    ctx.end("Session destroyed");
  }
  void keyLogout(RoutingContext ctx){
    ctx.redirect(authKey.endSessionURL(ctx.user()));
}
  void defaultHandler(RoutingContext ctx){
    ctx.response().end("<h1>Sorry, we have not this page. Go to <a href='/'>main page</a></h1>");
  }

}
