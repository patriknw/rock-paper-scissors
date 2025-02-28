# ROCK-PAPER-SCISSORS

Rock-Paper-Scissors is a game played to settle disputes between two people. Thought to be a game of chance that depends on random luck similar to flipping coins or drawing straws, the game is often taught to children to help them settle arguments between themselves on their own without adult intervention. However, the game actually can be a game that has an element of skill that requires quick thinking and perceptive reasoning.1

The game is played with three possible hand signals that represent a rock, paper, and scissors. The rock is a closed fist; paper is a flat hand with fingers and thumb extended and the palm facing downward; and scissors is a fist with the index and middle fingers fully extended toward the opposing player. Rock wins against scissors; paper wins against rock; and scissors wins against paper. If both players throw the same hand signal, it is considered a tie, and play resumes until there is a clear winner.

The hand signals are given simultaneously by both players. The ritual used to get players in sync with each other so they can deliver their throws simultaneously is called the prime. This action requires retracting the player’s fist from full-arm extension towards the shoulder and then back to full extension. To ensure a fair match the players must be in sync with their primes. Players must determine before play how many times they pump their arms during the prime phase, usually two or three times before the final delivery of their throw.


## Implementation

To understand the Akka concepts that are the basis for this example, see [Development Process](https://doc.akka.io/concepts/development-process.html) in the documentation.


This project contains the skeleton to create an Akka service. To understand more about these components, see [Developing services](https://doc.akka.io/java/index.html). Examples can be found [here](https://doc.akka.io/java/samples.html).


Use Maven to build your project:

```shell
mvn compile
```


When running an Akka service locally.

To start your service locally, run:

```shell
mvn compile exec:java
```

This command will start your Akka service. With your Akka service running, the endpoint it's available at:

```shell
curl http://localhost:9000/hello
```


You can use the [Akka Console](https://console.akka.io) to create a project and see the status of your service.

Build container image:

```shell
mvn clean install -DskipTests
```

Install the `akka` CLI as documented in [Install Akka CLI](https://doc.akka.io/reference/cli/index.html).

Deploy the service using the image tag from above `mvn install`:

```shell
akka service deploy rock-paper-scissors rock-paper-scissors:tag-name --push
```

Refer to [Deploy and manage services](https://doc.akka.io/operations/services/deploy-service.html)
for more information.
