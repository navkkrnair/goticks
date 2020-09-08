package com.goticks;

import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.Route;
import com.goticks.actors.RestApi;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

public class Main
{

    public static void main(String[] args) throws IOException
    {
        final ActorSystem system = ActorSystem.create("GoTicks");

        Config config = ConfigFactory.load();
        String host = config.getString("http.host");
        int port = config.getInt("http.port");

        Route routes = new RestApi(system).getRoutes();

        CompletionStage<ServerBinding> serverBinding = Http.get(system)
                .newServerAt(host, port)
                .bind(routes);

        System.out.println("Server started");
        System.in.read();

        serverBinding.thenApply(binding -> binding.unbind())
                .thenAccept(done -> system.terminate());
    }
}
