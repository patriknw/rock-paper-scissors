class GameUI {
    constructor() {
        this.playerId = '';
        this.gameId = '';
        this.lobbyId = 'lobby1';
        this.lobbyPollingInterval = null;
        this.gamePollingInterval = null;
        this.setupEventListeners();
    }

    setupEventListeners() {
        document.getElementById('create-player-btn').addEventListener('click', () => this.createPlayer());
        document.getElementById('join-lobby-btn').addEventListener('click', () => this.joinLobby());
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
                this.startLobbyPolling();
            } else {
                // If join fails, re-enable the button
                joinButton.disabled = false;
                joinButton.classList.remove('hidden');
            }
        } catch (error) {
            console.error('Error joining lobby:', error);
            // If there's an error, re-enable the button
            const joinButton = document.getElementById('join-lobby-btn');
            joinButton.disabled = false;
            joinButton.classList.remove('hidden');
        }
    }

    async checkLobbyState() {
        try {
            const response = await fetch(`/game/lobby/${this.lobbyId}`);
            if (response.ok) {
                const lobbyState = await response.json();
                if (lobbyState.player1Id && lobbyState.player2Id) {
                    this.gameId = lobbyState.gameId;
                    clearInterval(this.lobbyPollingInterval);
                    document.getElementById('lobby-section').classList.add('hidden');
                    document.getElementById('game-section').classList.remove('hidden');
                    this.startGamePolling();
                }
            }
        } catch (error) {
            console.error('Error checking lobby state:', error);
        }
    }

    startLobbyPolling() {
        this.lobbyPollingInterval = setInterval(() => this.checkLobbyState(), 1000);
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
            }
        } catch (error) {
            console.error('Error updating game state:', error);
        }
    }

    updateUI(gameState) {
        const playerMovesDiv = document.getElementById('player-moves');
        const opponentMovesDiv = document.getElementById('opponent-moves');

        const isPlayer1 = this.playerId === gameState.firstPlayerId;
        const playerMoves = isPlayer1 ? gameState.firstPlayerMoves : gameState.secondPlayerMoves;
        const opponentMoves = isPlayer1 ? gameState.secondPlayerMoves : gameState.firstPlayerMoves;

        playerMovesDiv.innerHTML = playerMoves.map(move => this.getMoveEmoji(move)).join(' ');
        opponentMovesDiv.innerHTML = opponentMoves.map(move => this.getMoveEmoji(move)).join(' ');

        if (gameState.winnerId) {
            const gameStatus = document.querySelector('.game-status p');
            gameStatus.textContent = gameState.winnerId === this.playerId ? 'You won!' : 'You lost!';
            document.querySelectorAll('.move-btn').forEach(btn => btn.disabled = true);
            clearInterval(this.gamePollingInterval);
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
}

// Initialize the game UI when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new GameUI();
});
