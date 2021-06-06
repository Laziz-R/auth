package com.intern.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.SessionStore;

public class MainVerticle extends AbstractVerticle {
    @Override
    public void start() throws Exception {
      JsonObject github = config().getJsonObject("auth").getJsonObject("github");
      JsonObject keycloak = config().getJsonObject("auth").getJsonObject("keycloak");
      OAuth2Auth oauth2 = OAuth2Auth.create(vertx,
              new OAuth2Options()
                  .setFlow(OAuth2FlowType.AUTH_CODE)
                  .setClientID(github.getString("clientId"))
                  .setClientSecret(github.getString("clientSecret"))
                  .setSite(github.getString("site"))
                  .setTokenPath(github.getString("tokenPath"))
                  .setAuthorizationPath(github.getString("authorizationPath")));

        Router router = Router.router(vertx);
        router.route().handler(SessionHandler.create(SessionStore.create(vertx)));
        router.route("/github/*").handler(ctx->{
          if(ctx.user()!=null && !ctx.user().expired()){
            ctx.next();
          }
          String authorization_uri = oauth2.authorizeURL(new JsonObject()
            .put("scope", github.getString("scope"))
            .put("state", ctx.normalizedPath()));
          ctx.redirect(authorization_uri);
        });

        router.get("/callback").handler(ctx->{
          String code = ctx.request().getParam("code");
          String state = ctx.request().getParam("state");
          
          oauth2.authenticate(new JsonObject()
          .put("code", code))
          .onSuccess(user->{
            System.err.println("code: " + code);
            ctx.setUser(user);
            ctx.redirect(state);
          })
          .onFailure(ar->{
            ctx.response().end("Failed");
          });
        });

        router.get("/github/hi")
        .handler(ctx->{
          oauth2.userInfo(ctx.user())
          .onSuccess(user->{
            ctx.response().end(user.toString());
          });
        });

        vertx.createHttpServer().requestHandler(router)
                .listen(config().getInteger("port"), config().getString("host"));
    }
}
