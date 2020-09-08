package com.goticks.actors;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.IntStream;

import static akka.pattern.Patterns.ask;
import static akka.pattern.Patterns.pipe;
import static java.util.stream.Collectors.toList;

public class BoxOffice extends AbstractActor
{
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    private final Map<String, ActorRef> actorRefs = new HashMap<>();

    @Override
    public Receive createReceive()
    {
        return receiveBuilder()
                .match(CreateEvent.class, createEvent ->
                {
                    log.info("CreatEvent message received, creating a TicketSeller");
                    actorRefs.put(createEvent.name, createTicketSeller(createEvent.name));
                    List<TicketSeller.Ticket> ticketList = IntStream.range(1, 11)
                            .mapToObj(i -> new TicketSeller.Ticket(i))
                            .collect(toList());
                    log.info("Telling TicketSeller to Add tickets");
                    actorRefs.get(createEvent.name)
                            .tell(new TicketSeller.Add(ticketList), getSelf());
                    getSender().tell(new EventCreated(
                            new Event(createEvent.name, createEvent.tickets)), getSelf());
                })
                .match(GetTickets.class, getTickets ->
                {
                    actorRefs.get(getTickets.name)
                            .forward(new TicketSeller.Buy(getTickets.tickets), getContext());
                })
                .match(GetEvents.class, getEvents ->
                {
                    log.info("Asking for all  the events from TicketSeller's");
                    List<CompletableFuture<Event>> listOfCompletableFutureEvent = actorRefs.values()
                            .stream()
                            .map(actorRef ->
                                    ask(actorRef, new TicketSeller.GetEvent(), Duration.ofSeconds(2))
                                            .thenApply(o -> (Event) o))
                            .map(CompletionStage::toCompletableFuture)
                            .collect(toList());
                    CompletableFuture<List<Event>> completableFutureOfEventList = sequence(listOfCompletableFutureEvent);
                    pipe(completableFutureOfEventList, getContext().getDispatcher()).to(getSender());
                })
                .match(GetEvent.class, getEvent ->
                {
                    log.info("Asking TicketSeller to give details of Event {}", getEvent.name);
                    CompletionStage<Event> eventCompletionStage = ask(actorRefs.get(getEvent.name), new TicketSeller.GetEvent(), Duration.ofSeconds(2))
                            .thenApply(o -> (Event) o);
                    pipe(eventCompletionStage, getContext().dispatcher()).to(sender());
                })
                .build();

    }

    private ActorRef createTicketSeller(String eventName)
    {
        return getContext().actorOf(Props.create(TicketSeller.class, eventName), eventName);
    }

    private <T> CompletableFuture<List<T>> sequence(List<CompletableFuture<T>> completableFutures)
    {
        log.info("Changing from List<CompletableFuture> to CompletableFuture<List>");
        return CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[completableFutures.size()]))
                .thenApply(aVoid -> completableFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(toList()));
    }

    @Data
    static class CreateEvent
    {
        final String name;
        final int tickets;
    }

    @Data
    static class GetEvent
    {
        final String name;
    }

    static class GetEvents
    {
    }

    @Data
    static class GetTickets
    {
        final String name;
        final int tickets;
    }

    @Data
    static class CancelEvent
    {
        final String name;
    }

    @Data
    static class Event
    {
        final String name;
        final int tickets;

        @JsonCreator
        public Event(@JsonProperty("name") String name, @JsonProperty("tickets") int tickets)
        {
            this.name = name;
            this.tickets = tickets;
        }
    }

    @Data
    static class Events
    {
        final List[] events;
    }

    static class EventResponse
    {
    }


    @Data
    static class EventCreated
    {
        final Event event;

        @JsonCreator
        public EventCreated(@JsonProperty("event") Event event)
        {
            this.event = event;
        }
    }

    static class EventExists extends EventResponse
    {
    }


}
