class GameUI {
    constructor() {
        this.playerId = '';
        this.gameId = '';
        this.lobbyId = 'lobby1';
        this.gamePollingInterval = null;
        this.leaderboardPollingInterval = null;
        this.setupEventListeners();
        this.startLeaderboardPolling();
    }

    setupEventListeners() {
        document.getElementById('create-player-btn').addEventListener('click', () => this.createPlayer());
        document.getElementById('join-lobby-btn').addEventListener('click', () => this.joinLobby());
        document.getElementById('new-game-btn').addEventListener('click', () => this.startNewGame());
        document.querySelectorAll('.move-btn').forEach(button => {
            button.addEventListener('click', (e) => this.makeMove(e.target.dataset.move));
        });
    }

    async createPlayer() {
        const playerId = document.getElementById('player-id').value;
        const playerName = document.getElementById('player-name').value;

        if (!playerId || !playerName) {
            alert('Please enter both Player ID and Name');
            return;
        }

        try {
            const response = await fetch('/game/player', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: playerId, name: playerName })
            });

            if (response.ok) {
                this.playerId = playerId;
                document.getElementById('login-section').classList.add('hidden');
                document.getElementById('lobby-section').classList.remove('hidden');
            }
        } catch (error) {
            console.error('Error creating player:', error);
        }
    }

    async joinLobby() {
        try {
            const joinButton = document.getElementById('join-lobby-btn');
            joinButton.disabled = true;
            joinButton.classList.add('hidden');

            const response = await fetch(`/game/lobby/${this.lobbyId}/join`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerId: this.playerId })
            });

            if (response.ok) {
                const data = await response.json();
                this.gameId = data.gameId;
                this.startGamePolling();
            } else {
                joinButton.disabled = false;
                joinButton.classList.remove('hidden');
            }
        } catch (error) {
            console.error('Error joining lobby:', error);
            const joinButton = document.getElementById('join-lobby-btn');
            joinButton.disabled = false;
            joinButton.classList.remove('hidden');
        }
    }

    async makeMove(move) {
        try {
            const response = await fetch(`/game/${this.gameId}/move`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ playerId: this.playerId, move: move })
            });

            if (response.ok) {
                this.updateGameState();
            }
        } catch (error) {
            console.error('Error making move:', error);
        }
    }

    async updateGameState() {
        try {
            const response = await fetch(`/game/${this.gameId}`);
            if (response.ok) {
                const gameState = await response.json();
                this.updateUI(gameState);

                // Switch to game section when second player joins
                if (gameState.secondPlayerId &&
                    gameState.secondPlayerId.length > 0 &&
                    document.getElementById('lobby-section').classList.contains('hidden') === false) {
                    document.getElementById('lobby-section').classList.add('hidden');
                    document.getElementById('game-section').classList.remove('hidden');
                }
            }
        } catch (error) {
            console.error('Error updating game state:', error);
        }
    }

    updateUI(gameState) {
        const playerMovesDiv = document.getElementById('player-moves');
        const opponentMovesDiv = document.getElementById('opponent-moves');
        const gameStatus = document.querySelector('.game-status p');
        const scoreDiv = document.querySelector('.score');
        const roundsDiv = document.querySelector('.rounds');
        const gameOverActions = document.getElementById('game-over-actions');

        const isPlayer1 = this.playerId === gameState.firstPlayerId;
        const playerMoves = isPlayer1 ? gameState.firstPlayerMoves : gameState.secondPlayerMoves;
        const opponentMoves = isPlayer1 ? gameState.secondPlayerMoves : gameState.firstPlayerMoves;
        const playerScore = isPlayer1 ? gameState.firstPlayerScore : gameState.secondPlayerScore;
        const opponentScore = isPlayer1 ? gameState.secondPlayerScore : gameState.firstPlayerScore;
        const playerMoveCount = isPlayer1 ? gameState.firstPlayerMoveCount : gameState.secondPlayerMoveCount;
        const opponentMoveCount = isPlayer1 ? gameState.secondPlayerMoveCount : gameState.firstPlayerMoveCount;

        // Update moves (only showing completed rounds)
        playerMovesDiv.innerHTML = playerMoves.map(move => this.getMoveEmoji(move)).join(' ');
        opponentMovesDiv.innerHTML = opponentMoves.map(move => this.getMoveEmoji(move)).join(' ');

        // Update score
        scoreDiv.innerHTML = `Score: You ${playerScore} - ${opponentScore} Opponent`;

        // Update rounds
        roundsDiv.innerHTML = `Round: ${gameState.completedRounds + 1}`;

        // Update game status based on game state
        if (gameState.winnerId) {
            gameStatus.textContent = gameState.winnerId === this.playerId ? 'You won!' : 'You lost!';
            document.querySelectorAll('.move-btn').forEach(btn => btn.disabled = true);
            clearInterval(this.gamePollingInterval);
            // Show game over actions
            gameOverActions.classList.remove('hidden');
            // Update leaderboard immediately when game ends
            this.updateLeaderboard();
        } else if (!gameState.secondPlayerId || gameState.secondPlayerId.length === 0) {
            gameStatus.textContent = 'Waiting for opponent to join...';
            document.querySelectorAll('.move-btn').forEach(btn => btn.disabled = true);
            gameOverActions.classList.add('hidden');
        } else {
            // Determine if player has already moved this round
            const hasMovedThisRound = playerMoveCount > gameState.completedRounds;
            const isRoundComplete = gameState.completedRounds * 2 === playerMoveCount + opponentMoveCount;

            if (isRoundComplete) {
                gameStatus.textContent = 'Your turn!';
                document.querySelectorAll('.move-btn').forEach(btn => btn.disabled = false);
            } else {
                gameStatus.textContent = hasMovedThisRound ? "Waiting for opponent..." : "Your turn!";
                document.querySelectorAll('.move-btn').forEach(btn => btn.disabled = hasMovedThisRound);
            }
            gameOverActions.classList.add('hidden');
        }
    }

    getMoveEmoji(move) {
        const emojis = {
            'ROCK': '✊',
            'PAPER': '✋',
            'SCISSORS': '✌️'
        };
        return emojis[move] || '';
    }

    startGamePolling() {
        this.gamePollingInterval = setInterval(() => this.updateGameState(), 1000);
    }

    async updateLeaderboard() {
        try {
            const response = await fetch('/game/leaderboard');
            if (response.ok) {
                const leaderboard = await response.json();
                this.updateLeaderboardUI(leaderboard);
            }
        } catch (error) {
            console.error('Error updating leaderboard:', error);
        }
    }

    updateLeaderboardUI(leaderboard) {
        const leaderboardList = document.getElementById('leaderboard-list');
        leaderboardList.innerHTML = leaderboard.players
            .map((player, index) => `
                <div class="leaderboard-entry">
                    <div class="leaderboard-position">${index + 1}</div>
                    <div class="leaderboard-name">${player.playerName}</div>
                    <div class="leaderboard-score">${player.score.toFixed(1)}</div>
                    <div class="leaderboard-stats">
                        ${player.gamesWon}W ${player.gamesLost}L
                    </div>
                </div>
            `)
            .join('');
    }

    startLeaderboardPolling() {
        this.updateLeaderboard();  // Initial update
        this.leaderboardPollingInterval = setInterval(() => this.updateLeaderboard(), 5000);
    }

    startNewGame() {
        // Hide game section and game over actions
        document.getElementById('game-section').classList.add('hidden');
        document.getElementById('game-over-actions').classList.add('hidden');
        // Show lobby section
        document.getElementById('lobby-section').classList.remove('hidden');
        // Reset join button
        const joinButton = document.getElementById('join-lobby-btn');
        joinButton.disabled = false;
        joinButton.classList.remove('hidden');
        // Clear game ID and intervals
        this.gameId = '';
        if (this.gamePollingInterval) {
            clearInterval(this.gamePollingInterval);
            this.gamePollingInterval = null;
        }
    }
}

// Initialize the game UI when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new GameUI();
});
