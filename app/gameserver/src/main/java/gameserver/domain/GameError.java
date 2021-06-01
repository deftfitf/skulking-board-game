package gameserver.domain;

public abstract class GameError extends RuntimeException {
    public GameError(String message) {
        super(message);
    }

    public GameError(String message, Throwable cause) {
        super(message, cause);
    }

    public static class InvalidInputGameError extends GameError {
        public InvalidInputGameError(String message) {
            super(message);
        }

        public InvalidInputGameError(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class IllegalGameStateError extends GameError {
        public IllegalGameStateError(String message) {
            super(message);
        }

        public IllegalGameStateError(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
