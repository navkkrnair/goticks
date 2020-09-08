package com.goticks.actors;

import akka.actor.AbstractActor;
import akka.actor.PoisonPill;
import akka.actor.Status;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.*;

public class TicketSeller extends AbstractActor
{
    private final LoggingAdapter log = Logging.getLogger(context().system(), this);
    private final String eventName;
    private final Queue<Ticket> tickets = new LinkedList<>();

    public TicketSeller(String eventName)
    {
        this.eventName = eventName;
    }

    @Override
    public Receive createReceive()
    {
        return receiveBuilder()
                .match(Add.class, add ->
                {
                    log.info("Adding {} tickets", add.tickets.size());
                    tickets.addAll(add.tickets);
                })
                .match(Buy.class, buy ->
                {
                    log.info("Buying {} tickets", buy.noOfTickets);
                    if (this.tickets.size() >= buy.noOfTickets)
                    {
                        List<Ticket> tempTickets = new ArrayList<>();
                        tempTickets.add(tickets.poll());
                        tempTickets.add(tickets.poll());
                        log.info("{} Tickets issued", buy.noOfTickets);
                        getSender().tell(new Tickets(this.eventName, tempTickets), getSelf());
                    }
                    else
                    {
                        getSender().tell(new Status.Failure(
                                new Exception("Only " + this.tickets.size() + " tickets available")), getSelf());
                    }
                })
                .match(GetEvent.class, getEvent ->
                {
                    log.info("Getting Event details for Event {}", this.eventName);
                    getSender().tell(new BoxOffice.Event(this.eventName, this.tickets.size()), getSelf());
                })
                .match(Cancel.class, cancel ->
                {
                    log.info("Cancelling the Event {}", this.eventName);
                    getSender().tell(new BoxOffice.Event(this.eventName, this.tickets.size()), getSelf());
                    getSelf().tell(PoisonPill.getInstance(), getSelf());
                })
                .build();
    }

    @Data
    static class Ticket
    {
        final int id;
    }

    @Data
    static class Add
    {
        final List<Ticket> tickets;
    }


    @Data
    static class Buy
    {
        final int noOfTickets;
    }

    @Data
    static class Tickets
    {
        final String name;
        final List<Ticket> entries;
    }

    static class GetEvent
    {
    }

    static class Cancel
    {
    }
}







