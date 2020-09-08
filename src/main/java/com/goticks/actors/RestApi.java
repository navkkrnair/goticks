package com.goticks.actors;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;

import static akka.http.javadsl.server.Directives.*;
import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.Patterns.ask;

public class RestApi
{
    private final ActorSystem system;
    private final ActorRef boxOffice;
    private static final Logger log = LoggerFactory.getLogger(RestApi.class);


    public RestApi(ActorSystem system)
    {
        this.system = system;
        this.boxOffice = createBoxOffice();
    }

    private ActorRef createBoxOffice()
    {
        return system.actorOf(Props.create(BoxOffice.class), "boxOffice");
    }

    public Route getRoutes()
    {
        return eventRoute;
    }

    private final Route eventRoute = pathPrefix("events",
            () -> concat(
                    pathEndOrSingleSlash(() -> post(() -> entity(Jackson.unmarshaller(EventDescription.class), eventDescription ->
                    {
                        log.info("Adding a new Event");
                        return completeOKWithFuture(createEvent(eventDescription.name, eventDescription.tickets), Jackson.marshaller());
                    }))),

                    path(segment(), name -> completeOKWithFuture(getEvent(name), Jackson.marshaller())),

                    get(() -> completeOKWithFuture(getAllEvents(), Jackson.marshaller()))
            ));

    private CompletionStage<List<BoxOffice.Event>> getAllEvents()
    {
        log.info("Getting all the events");
        return ask(this.boxOffice, new BoxOffice.GetEvents(), Duration.ofSeconds(2))
                .thenApply(o -> (List<BoxOffice.Event>) o);
    }

    private CompletionStage<BoxOffice.Event> getEvent(String eventName)
    {
        log.info("Asking BoxOffice for Event {}", eventName);
        return ask(this.boxOffice, new BoxOffice.GetEvent(eventName), Duration.ofSeconds(2))
                .thenApply(o -> (BoxOffice.Event) o);
    }


    private CompletionStage<BoxOffice.EventCreated> createEvent(String name, int noOfTickets)
    {
        log.info("Asking BoxOffice to CreateEvent");
        return ask(this.boxOffice, new BoxOffice.CreateEvent(name, noOfTickets), Duration.ofSeconds(2))
                .thenApply(o -> (BoxOffice.EventCreated) o);
    }

    @Data
    static class EventDescription
    {
        private final String name;
        private final int tickets;

        @JsonCreator
        public EventDescription(@JsonProperty("name") String name, @JsonProperty("tickets") int tickets)
        {
            this.name = name;
            this.tickets = tickets;
        }
    }

    @Data
    static class TicketRequest
    {
        private final int tickets;
    }

    @Data
    static class Error
    {
        private final String message;
    }
}
