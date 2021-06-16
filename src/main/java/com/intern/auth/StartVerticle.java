package com.intern.auth;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class StartVerticle extends AbstractVerticle{
    @Override
    public void start() throws Exception {
        String path = System.getProperty("user.dir") + "/src/main/resources/config.yaml";
        ConfigStoreOptions yamlStore = new ConfigStoreOptions()
          .setType("file")
          .setFormat("yaml")
          .setConfig(new JsonObject()
            .put("path", path)
        );
        ConfigRetrieverOptions options = new ConfigRetrieverOptions().addStore(yamlStore);
        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);
        retriever.getConfig()
        .onSuccess(conf->{
          vertx.deployVerticle(new KeyDemoVert(), new DeploymentOptions().setConfig(conf));
        });
    }
}
