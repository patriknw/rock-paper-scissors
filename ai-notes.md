# AI notes

This was an experiment to understand how well AI could write code for an Akka application.

I used [Qodo](https://www.qodo.ai) in IntelliJ (1.0.0-prerelease.5) with (mostly) Claude 3.5 Sonnet.

Without context about Akka SDK it started out with Akka libraries or older versions of the SDK. To bootstrap the context I included the sources of another sample in this project directory.

## Full history of prompt instructions

### Core game domain and entity

> I'm developing a multi-player game for rock-paper-scissors. Use the Akka SDK to define entities representing players and a game. Use EventSourcedEntity in the Akka 3 SDK. Use the same code structure as in template/src directory.

>> good, but define the events and the state in io.akka.sample.domain package

>> Represent the moves as enums according to the game rules, paper, rock, scissors

>> also update the events and other places to use the move enums as appropriate

> Add a method in the Game to evaluate a winner, if any, according to the game rules

> The game is played as the winner of 3 rounds. Represent the result of evaluateWinner as an enum with InProgress, Tie, Player1Winner, Player2Winner

> We should have two lists of moves. One for the first player, and the other for the second player. Then we can compare the moves of the first round by the moves with index 0 in the two lists. Moves for second round by the moves with index 1 and so on.

> Write a junit test for the Game, focus on evaluateWinner

> Make Game immutable, right now it is updating the lists of moves.

>> don't change evaluateWinner, it should be enough to make copies of the lists in addMove

>> we don't need firstPlayerScore and secondPlayerScore as instance variables. Those can be variables in evaluateWinner

> Rewrite GameTest to make use of that the Game is immutable

> Change the evaluation of winner to be the first player winning two rounds. If it's a tie the game continues until one of the players has won 2 times.

> Update the GameTest according to the new logic of evaluateWinner

> In makeMove of the GameEntity we should evaluate if the move results in a winner of the game, and then persist another GameOver event with the winning player id

> Rewrite Game to be a record

> There are some problems in GameTest. Rewrite it to follow the same pattern as testRockBeatsScissors

> Add the missing GameOver event and add the missing TypeName annotations to the events

> In makeMove, add validation that the player can't advance more than one move more than the other player. If that is the case reply with an error.

> Add a unit test for the GameEntity using Akka's EventSourcedTestKit

> Update the description of the game in the readme to explain the rules of the game in an easy way

### Player and lobby entities

> Add a Player entity. Use an Akka KeyValueEntity. A Player should have an id and name

>> Define the PlayerState in the domain package, and use a Java record.

> Add a Lobby entity that fills two slots of players. It also has a uuid representing the game id to be started when two players have joined in the lobby. Use a KeyValueEntity, since we are not interested in the history.

> Show me the LobbyState

>> Let's use Optional for the player ids in the LobbyState, since the state can have no player, one player or two players

> Show me the LobbyEntity again, and now using the LobbyState with optional player slots. It can use emptyState to define an empty LobbyState

> That's good, with one small addition. When a player joins the lobby and the current state already has two players it should create a new LobbyState with a new game id and the new player as player1

> Add a unit test for the LobbyEntity using Akka's KeyValueEntityTestKit

>> good, in testJoinLobbyFull we should also verify that the game id is changed after adding player3

### Start game via consumer of lobby events

> Add an Akka SDK Consumer that listens on state changes from the LobbyEntity, which is a KeyValueEntity

> good, when the LobbyState has two players it should create a GameEntity with the game id in the LobbyState. Use the componentClient to call the GameEntity. When the LobbyState doesn't have two players it can return the ignore effect.

>> almost, but use "method" and "invokeAsync" of the componentClient

>> that is still not correct usage of the componentClient to create a GameEntity.

> Change startGame to take one parameter containing the player ids.

>> Use a Java record with two player ids for that parameter to startGame

>> define that PlayerIds record

> Update other places that are using the PlayerEntity startGame to use the new parameter

> Add an integration test that is using Akka's TestKitSupport to verify the whole flow.
> - joining two players to the lobby
> - verify that a new GameEntity has been created
    > Use the componentClient to interact with the LobbyEntity and the GameEntity. Note that the game id can be retrieved from the LobbyEntity.

>> use method instead of call for the componentClient, and invokeAsync instead of params and execute

>> better, but for method you should use a method reference instead of a string, such as LobbyEntity::joinLobby

> Use await from the TestKitSupport instead of toCompletableFuture().join()

> good, when verifying the GameEntity we must use Awaitability since that is asynchronous

### Retrieve game state

> Add a getState method to the GameEntity to retrieve current state. Use ReadOnlyEffect

> Use PlayerState as parameter to createPlayer, instead of two parameters

> similar in makeMove, add a separate MoveRequest record in the GameEntity

> Change joinLobby so that it returns the updated LobbyState

> Update the test since joinLobby is now returning LobbyState

### Game endpoint

> Create a GameEndpoint that is a HttpEndpoint. It should provide
> - create and get player
> - join lobby
> - get game state
> - make a game move
> Use separate records for the endpoint, so that we don't leak domain or application records outside the service

> good, let's modify the signatures of some of the methods
> joinLobby can return CompletionStage of JoinLobbyResponse
> similar with getGameState and getPlayer

>> I only mean in the GameEndpoint, not in the entity

>> the reply is missing in joinLobby in the endpoint, use thenApply and transform the LobbyState to the JoinLobbyResponse

> In GetGameStateResponse we can represent the moves as strings, so that we don't leak domain classes outside the service.

> In GetGameStateResponse we should include information about if the game has a winner. It can be an Optional with the player id of the winner, if any. The winner can be evaluated from the GameState

> Add instructions in the readme of how to make HTTP requests to the GameEndpoint

> Add an integration test for the GameEndpoint using the TestKitSupport

> Instead of calling the GameEndpoint directly, interact with it using the httpClient

> interact with the GameEndpoint over HTTP using the httpClient that is provided by the TestKitSupport

> I have updated testCreatePlayer to use the httpClient in the right way, please update the other tests in GameEndpointIntegrationTest

> Let's add on more test there, which simulates the whole flow of a game. Create players, join lobby, make moves, get game state until we have a winner.

> Let's only keep that test of full game flow, and remove the other tests. It is currently using "game1" as the game id, but it has to retrieve the game id from the join lobby response, because it's a uuid

> Instead of raw json, we can use the request records. I have updated player1Request. Update the other requests.

>> Use the ObjectMapper to parse the response of the game state instead of string comparison

### UI

> Create a html/javascript UI for the game. I would like a modern look. Use css to define the style. Use similar colors as https://akka.io

> Seems like the UI is not working. When I join the lobby with the first player nothing happens. It should wait for the second player to join. It must get the lobby state so that it sees when the second player has joined, and then switch screen where the moves are drawn.

> Add the method to get the lobby state in the GameEndpoint

> After clicking join lobby hide the join button, so that the player can't join twice.

> There is a flaw in polling the lobby, since two other players may use the lobby and create another game.

### Change game creation

> I want to be able to create a GameEntity with only one player. Add a createGame method in GameEntity, and make secondPlayerId optional in the Game record.

> Use a separate GameCreated event and use Java Optional instead of null

> Define the GameCreated event

> Update the Game record accordingly, with Optional secondPlayerId

> Add or modify tests in GameEntityTest according to the new behavior of creating a game with one player

> Make createGame idempotent, so that it replies with done without persisting anything if the requested player is the same as the current player in the Game state

> Make startGame idempotent if the requested playerIds are the same as the players in the game state

> Update the GameEntityTest accordingly to the idempotent behavior

> In the LobbyConsumer, create the GameEntity if the state only has one player, otherwise start the game as of now

> Update the IntegrationTest according to the new flow and optional second player

> Update GameEndpoint to take the optional second player into account

> Now we should update the UI. Instead of polling the lobby to get the lobby state to see when second player has joined we should poll the game state immediately after joining the lobby. The game id is included in the response from joinLobby. When the game state includes two players it can switch from the lobby screen to the make moves screen.

### Score and moves

> We should calculate a score for the game that is in progress. Add firstPlayerScore that returns number of won rounds for the first player. Add same for player two.

> I think you introduced a regression, because the existing test GameTest.testPlayer2WinsAfterThreeRounds fails

> /improve I have reverted completely to the previous evaluateWinner because I think that was more correct. Maybe you can make use of the new isWinningMove to reduce duplication

**_NOTE:_** good refactoring, but it got evaluateWinner wrong again, change manually

> Add tests to GameTest to verify the score methods

> Include the score of the two players in the GetGameStateResponse. Also include a rounds field, which is number of completed rounds.

> Good, let's only include moves from completed rounds in the GetGameStateResponse, so that the opponent can't see the move from the other player until both has made their move in the round.

> Update the UI to show the score and the number of rounds from the GetGameStateResponse

> Update the index.html to include the score and rounds display

> Seems like we have a bug in the UI. After both players have joined the lobby it doesn't switch over to the screen where the players can make the moves

>> There is still something wrong with the UI. I can see that both players join the game and I can make a move from player 1, but nothing happens when I try to make a move from player 2.

>> sorry, it's still not possible to make any moves from player 2. note that we changed player moves to only include moves from completed rounds

> Since we only include moves from completed games in the GetGameStateResponse from the endpoint we must include a count of how many moves each player has made, also including count of moves from the round in progress.

> Update the UI accordingly, to use the counts to know which turn it is

> Thanks that is working. Now I'd like to adjust one more thing in the UI. Both players can make the moves of a round at the same time or in any order. Therefore I would like the gameStatus to show "Your turn!" for both players at first, and once a player has made a move it should show "Opponent's turn...", until the round is completed, and then show "Your turn!" again.

### Game statistics

> Add information about number of won games and number of lost games, and total number of played games to the Player

> Add method to the GameEntity to update game statistics. One method of gameWon and another for gameLost. To make these idempotent we can include the game id as parameter and keep track of the 10 most recent games in the Player.

> Add method to the GameEntity to update game statistics. One method of gameWon and another for gameLost. To make these idempotent we can include the game id as parameter and keep track of the 10 most recent games in the Player.

>> sorry, wrong instructions, I want these methods in the PlayerEntity

>> the methods can take a single String gameId parameter instead

> Add a list of game ids to the Player. We will use them for idempotency checks. Include a boolean method to ask if the player has already record game statistics for a given game id.
> Include the game id in the increment methods, and update the lists from those methods. Make sure that the record is immutable, i.e. copy the lists when modifying.
> Also, keep only the 10 most recent game ids.

> ok, but we can use a single list of game ids for both won and lost games

> Make gameWon and gameLost in the PlayerEntity idempotent by using the new game id tracking in the Player

> Let's add a consumer of game events and when it sees a GameOver event it should call gameWon of the PlayerEntity, using component client. Note that the id of the game can be retrieved in the consumer with messageContext.eventSubject

> I see that you get the looser player id via the GameEntity. I think it would be better if we include the looser player id in the GameOver event

> include the looser player id in the GameOver event

> Include the looser id in GameOver event from the GameEntity

> Back to the Game event consumer. when it sees a GameOver event it should update the game statistics of the PlayerEntity

> Update the IntegrationTest to verify the game statistics are updated when the game has been completed with a winner.

**_NOTE:_** it updated the test in a great way, made relevant player moves to have a winner

### Leaderboard view

> A game should of course have a leaderboard, how would you implement that?

**_NOTE:_** it followed the same pattern, LeaderBoardConsumer that updates LeaderBoardEntity from PlayerEntity updates

> I have a better suggestion, Let's use an Akka SDK View that is updated from Player change events.

> Looks good, describe the difference between rank and score?

> Alright, we don't need rank in this leaderboard

> Add a test for the LeaderboardView, use Akka's TestKitSupport

> getTopPlayers must return a concrete record instead of List<PlayerStats>, let's name that Leaderboard

> Update the test accordingly

> limit is a reserved word in the sql, use another parameter name of the method

> There is a test failure in testScoreCalculation: LeaderboardViewIntegrationTest expected: `<player6>` but was: `<player4>`

**_NOTE:_** Great finding: The test failure occurs because we have multiple tests running in the same test class that are affecting each other. The testGetPlayerStats creates "player4" with 2 wins and 1 loss, which remains in the view when testScoreCalculation runs. We need to clean up the view between tests.

> To avoid interference with the other test methods we should filter out only "player5" and "player6" from the leaderboard result

> Expose the LeaderboardView in the GameEndpoint

> good, but remove the maxResults parameter. hardcode 10 as the maxResults in the call to the view

> Verify that the leaderboard is updated in the IntegrationTest

> also, verify that the leaderboard is updated in the GameEndpointIntegrationTest

> Add the leaderboard to the UI. We only need to show the leaderboard list view, not stats for a single player. Update both html, css and js

> Could you repeat the whole game.js so I'm sure I got the changes right?

> We should adjust the UI so that when a game is over there should be a button go show the first page with leaderboard and possibility to join a new game. We can keep the same player, and hide the create new player part. Update both js and html.

### Architecture description

> Describe the architecture of the application and the main interactions between components. Add the description to the README.
> It would be great if you can draw some Mermaid diagrams to illustrate the interactions.

> Good, I'd like a few adjustments. Remove mention of CQRS, since that isn't important here.
> Regarding scalability and resiliency we can also mention that Akka is distributed by design and supports multi-region and mutli-cloud deployments.
> Maybe you could also incorporate some of Akka's Key Principles
> Elastic: Akka applications automatically scale to varying workloads and position active-active replicas close to users.
> Agile: Akka allows for updates, rebalancing, and repartitioning of workloads, enabling no-downtime maintenance.
> Resilient: Akka applications function as self-managed in-memory databases, ensuring recoverability and replicability to handle potential failures.

### Bug fix

> We need to validate that the first and second players are not the same when joining the lobby. Reply with error if that is the case.

> In the GameConsumer we need to ignore the change if the LobbyState contains the same player as both player one and two 
