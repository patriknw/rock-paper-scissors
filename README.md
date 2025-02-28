# ROCK-PAPER-SCISSORS

## How to play ROCK-PAPER-SCISSORS

Rock-Paper-Scissors is a simple hand game played between two people. It's often used to make a quick decision or settle a dispute. The game involves three possible hand signals:

- **Rock**: A closed fist.
- **Paper**: An open hand with fingers extended.
- **Scissors**: A fist with the index and middle fingers extended, forming a V.

### Game Rules

1. **Objective**: Each player chooses one of the three hand signals. The winner is determined by the following rules:
    - Rock crushes Scissors.
    - Paper covers Rock.
    - Scissors cut Paper.

2. **Tie**: If both players choose the same hand signal, the game is a tie, and players must play again.

3. **Gameplay**: Players count to three in unison and then simultaneously show their chosen hand signal. This is often accompanied by a chant like "Rock, Paper, Scissors, Shoot!"

4. **Winning the Game**: The game can be played in rounds, and the first player to win two rounds is declared the overall winner.

Rock-Paper-Scissors is not just a game of chance; it can involve strategy and quick thinking. Enjoy playing and may the best strategist win!


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
