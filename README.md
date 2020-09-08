# goticks
A simple experiment using Akka. Uses Akka http and some Actors. It can create Events and tickets.
1. POST to http://localhost:8080/events with a payload
{
  "name":"Deepavali",
  "tickets": 10
}
to create Events

2. GET to http://localhost:8080/events/Deepavali to retrieve a single event
{
"name": "Deepavali",
"tickets": 10
}

3. GET to http://localhost:8080/events to retrieve all Events
{
"name": "Deepavali",
"tickets": 10
},
  {
"name": "Onam",
"tickets": 10
}

