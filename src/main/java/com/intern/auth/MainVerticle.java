package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start() throws Exception {

    JsonObject keyJson = config().getJsonObject("auth").getJsonObject("keycloak");

    OAuth2Auth authGit = OAuth2Auth.create(vertx, gitOptions());
    OAuth2Auth authKey = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, keyJson);

    Router router = Router.router(vertx);
    router.route().handler(SessionHandler.create(SessionStore.create(vertx)));

    router.route("/github/*").handler(ctx -> {
      if (ctx.user() == null) {
        String authorization_uri = authGit.authorizeURL(new JsonObject().put("scope", "user"));
        ctx.session().put("back", ctx.request().path());
        ctx.redirect(authorization_uri);
      } else {
        if (ctx.user().expired()) {
          authGit.refresh(ctx.user()).onSuccess(freshUser -> {
            ctx.setUser(freshUser);
          });
        }
        ctx.next();
      }
    });


    router.get("/callback").handler(ctx -> {
      String code = ctx.request().getParam("code");
      authGit.authenticate(new JsonObject().put("code", code)).onSuccess(user -> {
        ctx.setUser(user);
        ctx.redirect(ctx.session().get("back"));
      }).onFailure(ar -> {
        ctx.response().end("Failed");
      });
    });

    router.get("/github/hi").handler(ctx -> {
      authGit
      .userInfo(ctx.user())
      .onSuccess(user -> {
        ctx.response().end("Hi, " + user.getString("name"));
      })
      .onFailure(ar->
        ctx.response().end(ar.getMessage())
      );
    });

    vertx.createHttpServer().requestHandler(router).listen(config().getInteger("port"),
        config().getString("host"));
  }

  public OAuth2Options gitOptions(){    
    JsonObject github = config().getJsonObject("auth").getJsonObject("github");
    return  new OAuth2Options()
      .setFlow(OAuth2FlowType.AUTH_CODE)
      .setClientID(github.getString("clientId"))
      .setClientSecret(github.getString("clientSecret"))
      .setSite(github.getString("site"))
      .setUserInfoPath(github.getString("userInfoPath"))
      .setUserAgent("vertx")
      .setTokenPath(github.getString("tokenPath"))
      .setAuthorizationPath(github.getString("authorizationPath"));
  }
}
