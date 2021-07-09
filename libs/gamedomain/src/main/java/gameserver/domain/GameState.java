package gameserver.domain;

import akka.serialization.jackson.CborSerializable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;

import java.util.*;
import java.util.stream.Collectors;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "start_phase", value = GameState.StartPhase.class),
        @JsonSubTypes.Type(name = "bidding_phase", value = GameState.BiddingPhase.class),
        @JsonSubTypes.Type(name = "trick_phase", value = GameState.TrickPhase.class),
        @JsonSubTypes.Type(name = "next_trick_lead_player_changing", value = GameState.NextTrickLeadPlayerChanging.class),
        @JsonSubTypes.Type(name = "hand_change_waiting", value = GameState.HandChangeWaiting.class),
        @JsonSubTypes.Type(name = "future_predicate_waiting", value = GameState.FuturePredicateWaiting.class),
        @JsonSubTypes.Type(name = "bid_declare_change_waiting", value = GameState.BidDeclareChangeWaiting.class),
        @JsonSubTypes.Type(name = "finished_phase", value = GameState.FinishedPhase.class),
})
public interface GameState extends CborSerializable {

    PlayerId getRoomOwnerId();

    GameRule getRule();

    GameStateType getStateName();

    List<PlayerId> getPlayerIds();

    List<GameEvent> getEventQueue();

    @Data
    @Builder
    class StartPhase implements GameState {
        @NonNull GameRule rule;

        @NonNull PlayerId dealerId;
        @NonNull List<PlayerId> playerIds;

        @Builder.Default
        @NonNull List<GameEvent> eventQueue = new ArrayList<>();

        @Override
        public PlayerId getRoomOwnerId() {
            return dealerId;
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.START_PHASE;
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public InputCheckResult canJoin(PlayerId playerId) {
            if (playerIds.contains(playerId)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.FAILED_JOIN_ALREADY_JOINED_PLAYER).build();
            } else if (playerIds.size() >= rule.getRoomSize()) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.FAILED_JOIN_EXCEED_MAX_NUMBER_OF_PLAYERS).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public void join(PlayerId playerId) {
            playerIds.add(playerId);
        }

        public InputCheckResult canLeave(PlayerId playerId) {
            if (!playerIds.contains(playerId)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.FAILED_LEAVE_SPECIFIED_PLAYER_NOT_EXISTS).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public void leave(PlayerId playerId) {
            playerIds.remove(playerId);

            if (dealerId.equals(playerId)) {
                if (playerIds.size() <= 0) {
                    return;
                }

                dealerId = playerIds.get(0);
            }
        }

        public InputCheckResult canStartBid() {
            if (playerIds.size() < GameRule.ROOM_MIN_SIZE) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.FAILED_START_INSUFFICIENT_PLAYERS).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public BiddingPhase startBidding() {
            return BiddingPhase.newGame(dealerId, rule, dealerId, playerIds);
        }

        public static StartPhase empty(GameRule gameRule, PlayerId playerId) {
            final var playerIds = new ArrayList<PlayerId>();
            return StartPhase.builder()
                    .rule(gameRule)
                    .dealerId(playerId)
                    .playerIds(playerIds)
                    .build();
        }

    }

    @Data
    @Builder
    public class BiddingPhase implements GameState {
        @NonNull PlayerId roomOwnerId;
        @NonNull GameRule rule;

        @NonNull List<PlayerId> playerIds;
        @NonNull PlayerId dealerId;
        int round;
        @NonNull LinkedList<Card> deck;
        @NonNull Map<PlayerId, Player> idToPlayer;
        @NonNull ScoreBoard scoreBoard;

        @Builder.Default
        @NonNull List<GameEvent> eventQueue = new ArrayList<>();

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public void addGameEvent(GameEvent event) {
            eventQueue.add(event);
        }

        public InputCheckResult canBid(PlayerId playerId, int bid) {
            final var player = idToPlayer.get(playerId);
            if (player == null) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.SPECIFIED_PLAYER_ID_DOES_NOT_EXISTS).build();
            }

            if (bid > round || bid < 0) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.DECLARED_INVALID_BID_VALUE).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public boolean canStartRound() {
            return idToPlayer.values().stream()
                    .allMatch(p -> p.getDeclaredBid() != null);
        }

        public void bid(PlayerId playerId, int bid) {
            final var player = idToPlayer.get(playerId);
            player.setDeclaredBid(bid);
        }

        public static BiddingPhase newGame(
                PlayerId roomOwnerId, GameRule rule, PlayerId dealerId, List<PlayerId> playerIds
        ) {
            return startRound(roomOwnerId, 1, rule, dealerId, playerIds, ScoreBoard.empty());
        }

        public static BiddingPhase startRound(
                PlayerId roomOwnerId, int round, GameRule rule, PlayerId dealerId,
                List<PlayerId> playerIds, ScoreBoard scoreBoard
        ) {
            final var deck = rule.provideNewDeck();
            Collections.shuffle(deck);

            final var players = new HashMap<PlayerId, Player>();
            for (final PlayerId playerId : playerIds) {
                final var player = new Player(playerId);

                final var hand = new HashMap<CardId, Card>();
                for (int r = 1; r <= round; r++) {
                    final var card = deck.pop();
                    hand.put(card.getCardId(), card);
                }
                player.setCards(hand);

                players.put(playerId, player);
            }

            return BiddingPhase
                    .builder()
                    .roomOwnerId(roomOwnerId)
                    .rule(rule)
                    .deck(deck)
                    .dealerId(dealerId)
                    .playerIds(playerIds)
                    .round(round)
                    .idToPlayer(players)
                    .scoreBoard(scoreBoard)
                    .build();
        }

        public TrickPhase startTrick() {
            final var players = playerIds.stream()
                    .map(idToPlayer::get)
                    .collect(Collectors.toCollection(LinkedList<Player>::new));

            TrickPhase.rotatePlayers(dealerId, players);

            return TrickPhase.builder()
                    .roomOwnerId(roomOwnerId)
                    .rule(rule)
                    .round(round)
                    .dealerId(dealerId)
                    .playerIds(playerIds)
                    .players(players)
                    .deck(deck)
                    .scoreBoard(scoreBoard)
                    .build();
        }

    }

    @Data
    @Builder
    class TrickPhase implements GameState {
        @NonNull PlayerId roomOwnerId;
        @NonNull GameRule rule;
        int round;
        @NonNull PlayerId dealerId;
        @NonNull List<PlayerId> playerIds;
        @NonNull LinkedList<Player> players;
        @NonNull LinkedList<Card> deck;

        @Builder.Default
        @NonNull List<CardId> stack = new ArrayList<>();

        Card.NumberCard.CardColor mustFollow;
        @Builder.Default
        @NonNull LinkedList<PlayedCard> field = new LinkedList<>();

        @Builder.Default
        int trick = 1;

        @NonNull ScoreBoard scoreBoard;

        @Builder.Default
        @NonNull List<GameEvent> eventQueue = new ArrayList<>();

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public void clearEventQueue() {
            eventQueue = new ArrayList<>();
        }

        public void addGameEvent(GameEvent event) {
            eventQueue.add(event);
        }

        protected static void rotatePlayers(PlayerId dealerId, LinkedList<Player> players) {
            for (int i = 0; i < players.size(); i++) {
                final var head = players.getFirst();
                if (head.getPlayerId().equals(dealerId)) {
                    break;
                }
                players.offer(players.pop());
            }

            if (!players.getFirst().getPlayerId().equals(dealerId)) {
                throw new IllegalStateException("dealer is not included in player, dealer=" + dealerId);
            }
        }

        public void rotatePlayers(PlayerId dealerId) {
            rotatePlayers(dealerId, players);
        }

        public boolean isLastTrick() {
            return trick == round;
        }

        public boolean isRoundFinished() {
            return trick > round;
        }

        public boolean isGameFinished() {
            return round >= rule.getNOfRounds();
        }

        public Player nextPlayer() {
            return players.getFirst();
        }

        public void playerChange() {
            players.offer(players.pop());
        }

        public boolean isFinishedTrick() {
            return nextPlayer().getPlayerId().equals(dealerId);
        }

        public Player getPlayerOf(PlayerId playerId) {
            return players.stream()
                    .filter(player -> player.getPlayerId().equals(playerId))
                    .findFirst()
                    .orElse(null);
        }

        public InputCheckResult canPlay(PlayerId playerId, Card playCard) {
            if (isRoundFinished()) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.ROUND_HAS_ALREADY_ENDED).build();
            }

            final var nextPlayer = nextPlayer();
            if (!playerId.equals(nextPlayer.getPlayerId())) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.IS_NOT_NEXT_PLAYER).build();
            }

            if (!nextPlayer.hasCard(playCard.getCardId())) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.HAS_NOT_CARD).build();
            }

            if (!canPut(nextPlayer, playCard)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.CANT_PUT_CARD_ON_FIELD).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public void play(PlayerId playerId, Card playCard) {
            final var nextPlayer = nextPlayer();

            nextPlayer.removeCard(playCard.getCardId());
            final var playedCard = PlayedCard.builder().playerId(playerId).card(playCard).build();
            field.offer(playedCard);

            playerChange();
        }

        public TrickFinishResult judge() {
            return judge(new LinkedList<>(field));
        }

        private TrickFinishResult judge(LinkedList<PlayedCard> field) {
            final var battleResult = battle(field);
            final var winner = battleResult.getWinner();

            if (winner.getCard() instanceof Card.Escape) {
                return AllRanAway.builder().winnerId(winner.getPlayerId()).card(winner.getCard()).build();
            } else if (winner.getCard() instanceof Card.Tigress
                    && !((Card.Tigress) winner.getCard()).getIsPirates()) {
                return AllRanAway.builder().winnerId(winner.getPlayerId()).card(winner.getCard()).build();
            } else if (winner.getCard() instanceof Card.Skulking) {
                final var playedFirstMermaid = battleResult.getFirstMermaid();
                if (playedFirstMermaid != null) {
                    return APlayerWon.builder()
                            .winnerId(playedFirstMermaid.getPlayerId())
                            .card(playedFirstMermaid.getCard())
                            .trickBonus(battleResult.getBaseBonus() + 50)
                            .build();
                }

                return APlayerWon.builder()
                        .winnerId(winner.getPlayerId())
                        .card(winner.getCard())
                        .trickBonus(battleResult.getBaseBonus() + 30 * battleResult.getPiratesCount())
                        .build();
            } else if (winner.getCard() instanceof Card.Kraken) {
                final var fieldRemovedKraken = new LinkedList<>(field);
                fieldRemovedKraken.removeIf(playedCard -> playedCard.getCard().getCardId().equals(winner.getCard().getCardId()));

                final var suddenDeath = battle(fieldRemovedKraken);
                final var mustHaveWon = suddenDeath.getWinner();
                return KrakenAppeared.builder().mustHaveWon(mustHaveWon.getPlayerId()).build();
            } else if (winner.getCard() instanceof Card.Pirates) {
                final var pirates = (Card.Pirates) winner.getCard();
                final var piratesEvent = effect(winner.getPlayerId(), pirates);

                return APlayerWon.builder()
                        .winnerId(winner.getPlayerId())
                        .card(winner.getCard())
                        .trickBonus(battleResult.getBaseBonus())
                        .piratesEvent(piratesEvent)
                        .build();
            }

            return APlayerWon.builder()
                    .winnerId(winner.getPlayerId())
                    .card(winner.getCard())
                    .trickBonus(battleResult.getBaseBonus())
                    .build();
        }

        private BattleResult battle(LinkedList<PlayedCard> field) {
            final var _field = new LinkedList<>(field);
            PlayedCard winner = null;
            var baseBonus = 0;
            var piratesCount = 0;
            var skulkingAppeared = false;
            var firstMermaid = (PlayedCard) null;
            while (!_field.isEmpty()) {
                final var next = _field.pop();
                if (next.getCard() instanceof Card.Skulking) {
                    skulkingAppeared = true;
                } else if (next.getCard() instanceof Card.Pirates) {
                    piratesCount++;
                } else if (next.getCard() instanceof Card.Mermaid && firstMermaid == null) {
                    firstMermaid = next;
                } else if (next.getCard() instanceof Card.NumberCard) {
                    baseBonus += ((Card.NumberCard) next.getCard()).getBonusPoint();
                }

                if (winner == null || !winner.getCard().battle(next.getCard())) {
                    winner = next;
                }
            }

            return BattleResult.builder()
                    .baseBonus(baseBonus)
                    .piratesCount(piratesCount)
                    .skulkingAppeared(skulkingAppeared)
                    .firstMermaid(firstMermaid)
                    .winner(winner)
                    .build();
        }

        private PiratesEvent effect(PlayerId winnerId, Card.Pirates pirate) {
            if (pirate instanceof Card.RoiseDLaney) {
                if (!isLastTrick()) {
                    return PiratesEvent.NextTrickLeadPlayerChangeableNotice.builder()
                            .playerId(winnerId)
                            .build();
                }
            } else if (pirate instanceof Card.BahijTheBandit) {
                if (!isLastTrick()) {
                    final var drawn = draw(2);
                    final var winner = getPlayerOf(winnerId);
                    drawn.forEach(card -> winner.getCards().put(card.getCardId(), card));

                    return PiratesEvent.HandChangeAvailableNotice.builder()
                            .playerId(winnerId)
                            .drawCards(drawn.stream().map(Card::getCardId).collect(Collectors.toList()))
                            .build();
                }
            } else if (pirate instanceof Card.RascalOfRoatan) {
                final var rascal = (Card.RascalOfRoatan) pirate;
                if (isLastTrick() && rascal.getBetScore() != null) {
                    return PiratesEvent.GotBonusScore.builder()
                            .playerId(winnerId)
                            .bonusScore(rascal.getBetScore())
                            .build();
                }
            } else if (pirate instanceof Card.JuanitaJade) {
                if (!isLastTrick()) {
                    return PiratesEvent.FuturePredicateAvailable.builder()
                            .playerId(winnerId)
                            .build();
                }
            } else if (pirate instanceof Card.HarryTheGiant) {
                if (!isLastTrick()) {
                    final var winner = getPlayerOf(winnerId);
                    if (winner == null) {
                        throw new IllegalStateException("winner not found, illegal game state: winnerId=" + winnerId.getValue());
                    }

                    final var currentBid = winner.getDeclaredBid();
                    return PiratesEvent.DeclareBidChangeAvailable.builder()
                            .playerId(winnerId)
                            .min(Math.max(0, currentBid - 1))
                            .max(Math.min(round, currentBid + 1))
                            .build();
                }
            }

            return null;
        }

        private List<Card> draw(int n) {
            final var drawn = new ArrayList<Card>();
            for (int i = 0; i < n; i++) {
                drawn.add(deck.pop());
            }
            return drawn;
        }

        public void prepareNextTrick(TrickFinishResult trickFinishResult) {
            // Advance the trick
            trick++;

            // Field initialization
            stack.addAll(field.stream()
                    .map(PlayedCard::getCard)
                    .map(Card::getCardId)
                    .collect(Collectors.toUnmodifiableList()));
            field.clear();

            // Player rotate
            dealerId = trickFinishResult.getWinnerId();
            rotatePlayers(dealerId);

            // Point settlement process
            final var dealer = nextPlayer();
            dealer.gotATrick();
            if (trickFinishResult instanceof APlayerWon) {
                final var result = (APlayerWon) trickFinishResult;
                dealer.addTookBonus(result.getTrickBonus());
            }
        }

        public void calcRoundScore() {
            // Calculate round score
            final var roundScore = players.stream()
                    .collect(Collectors.toMap(
                            Player::getPlayerId,
                            player -> player.getRoundScore(round)));

            scoreBoard.addRoundScore(roundScore);
        }

        public FinishedPhase finish() {
            return FinishedPhase.builder()
                    .roomOwnerId(roomOwnerId)
                    .rule(rule)
                    .lastWinnerId(dealerId)
                    .playerIds(playerIds)
                    .scoreBoard(scoreBoard)
                    .build();
        }

        public BidDeclareChangeWaiting bidDeclareChangeWaiting(
                PiratesEvent.DeclareBidChangeAvailable piratesEvent,
                APlayerWon aPlayerWon
        ) {
            return BidDeclareChangeWaiting.builder()
                    .trickPhase(this)
                    .changingPlayerId(piratesEvent.getPlayerId())
                    .aPlayerWon(aPlayerWon)
                    .eventQueue(eventQueue)
                    .build();
        }

        public NextTrickLeadPlayerChanging nextTrickLeadPlayerChanging(
                PiratesEvent.NextTrickLeadPlayerChangeableNotice piratesEvent,
                APlayerWon aPlayerWon
        ) {
            return NextTrickLeadPlayerChanging.builder()
                    .trickPhase(this)
                    .changingPlayerId(piratesEvent.getPlayerId())
                    .aPlayerWon(aPlayerWon)
                    .eventQueue(eventQueue)
                    .build();
        }

        public HandChangeWaiting handChangeWaiting(
                PiratesEvent.HandChangeAvailableNotice piratesEvent,
                APlayerWon aPlayerWon
        ) {
            return HandChangeWaiting.builder()
                    .trickPhase(this)
                    .changingPlayerId(piratesEvent.getPlayerId())
                    .aPlayerWon(aPlayerWon)
                    .drawCardIds(piratesEvent.getDrawCards())
                    .eventQueue(eventQueue)
                    .build();
        }

        public FuturePredicateWaiting futurePredicateWaiting(
                PiratesEvent.FuturePredicateAvailable piratesEvent,
                APlayerWon aPlayerWon
        ) {
            return FuturePredicateWaiting.builder()
                    .trickPhase(this)
                    .predicatingPlayerId(piratesEvent.getPlayerId())
                    .aPlayerWon(aPlayerWon)
                    .eventQueue(eventQueue)
                    .build();
        }

        private boolean canPut(Player player, Card card) {
            if (mustFollow == null) {
                return true;
            }

            if (card instanceof Card.NumberCard) {
                final var numberCard = (Card.NumberCard) card;

                if (numberCard.getCardColor() == mustFollow) {
                    return true;
                }

                return !player.hasColorCard(mustFollow);
            }

            return true;
        }

        public BiddingPhase nextBiddingPhase() {
            return BiddingPhase.startRound(roomOwnerId, round + 1, rule, dealerId, playerIds, scoreBoard);
        }

    }

    @Value
    @Builder
    public class NextTrickLeadPlayerChanging implements GameState {
        @NonNull TrickPhase trickPhase;
        @NonNull PlayerId changingPlayerId;
        @NonNull GameState.TrickPhase.APlayerWon aPlayerWon;

        @Builder.Default
        @NonNull List<GameEvent> eventQueue = new ArrayList<>();

        @Override
        public PlayerId getRoomOwnerId() {
            return trickPhase.getRoomOwnerId();
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        @Override
        public GameRule getRule() {
            return trickPhase.getRule();
        }

        @Override
        public List<PlayerId> getPlayerIds() {
            return trickPhase.getPlayerIds();
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public InputCheckResult canChangeLeadPlayer(
                PlayerId changePlayerId,
                PlayerId newLeadPlayerId
        ) {
            if (!changePlayerId.equals(changingPlayerId)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.ILLEGAL_PLAYER_ACTION_DETECTED).build();
            }

            if (trickPhase.getPlayerOf(newLeadPlayerId) == null) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.SPECIFIED_PLAYER_ID_DOES_NOT_EXISTS).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

    }

    @Value
    @Builder
    public class HandChangeWaiting implements GameState {
        @NonNull TrickPhase trickPhase;
        @NonNull PlayerId changingPlayerId;
        @NonNull List<CardId> drawCardIds;
        @NonNull GameState.TrickPhase.APlayerWon aPlayerWon;
        @NonNull List<GameEvent> eventQueue;

        @Override
        public PlayerId getRoomOwnerId() {
            return trickPhase.getRoomOwnerId();
        }

        @Override
        public GameRule getRule() {
            return trickPhase.getRule();
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        @Override
        public List<PlayerId> getPlayerIds() {
            return trickPhase.getPlayerIds();
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public InputCheckResult canChangeHand(Set<CardId> returnCards) {
            if (returnCards.size() != 2) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.RETURN_CARD_SIZE_INVALID).build();
            }

            final var player = trickPhase.getPlayerOf(changingPlayerId);
            if (!returnCards.stream().allMatch(player.getCards()::containsKey)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.RETURN_CARD_PLAYER_NOT_HAS).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public void changeHand(Set<CardId> returnCards) {
            final var player = trickPhase.getPlayerOf(changingPlayerId);
            returnCards.forEach(player.getCards()::remove);
        }

    }

    @Value
    @Builder
    class FuturePredicateWaiting implements GameState {
        @NonNull TrickPhase trickPhase;
        @NonNull PlayerId predicatingPlayerId;
        @NonNull GameState.TrickPhase.APlayerWon aPlayerWon;
        @NonNull List<GameEvent> eventQueue;

        @Override
        public PlayerId getRoomOwnerId() {
            return trickPhase.getRoomOwnerId();
        }

        @Override
        public GameRule getRule() {
            return trickPhase.getRule();
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        @Override
        public List<PlayerId> getPlayerIds() {
            return trickPhase.getPlayerIds();
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

    }

    @Value
    @Builder
    class BidDeclareChangeWaiting implements GameState {
        @NonNull TrickPhase trickPhase;
        @NonNull PlayerId changingPlayerId;
        @NonNull GameState.TrickPhase.APlayerWon aPlayerWon;
        @NonNull List<GameEvent> eventQueue;

        @Override
        public PlayerId getRoomOwnerId() {
            return trickPhase.getRoomOwnerId();
        }

        @Override
        public GameRule getRule() {
            return trickPhase.getRule();
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_PLAYING;
        }

        @Override
        public List<PlayerId> getPlayerIds() {
            return trickPhase.getPlayerIds();
        }

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        public InputCheckResult canChangeBid(PlayerId playerId, int changeBid) {
            if (!(changeBid >= -1 && changeBid <= 1)) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.INVALID_CHANGE_BID_VALUE).build();
            }

            final var player = trickPhase.getPlayerOf(playerId);
            final var maybeNewBid = newBid(player, changeBid);
            if (!(maybeNewBid >= 0 && maybeNewBid <= getTrickPhase().getRound())) {
                return InputCheckResult.InvalidInput.builder().invalidInputType(InputCheckResult.InvalidInputType.INVALID_CHANGE_BID_VALUE).build();
            }

            return InputCheckResult.ApplyableInput.INSTANCE;
        }

        public void changeBid(PlayerId playerId, int changeBid) {
            final var player = trickPhase.getPlayerOf(playerId);
            final var newBid = newBid(player, changeBid);

            player.setDeclaredBid(newBid);
        }

        private int newBid(Player player, int changeBid) {
            return player.getDeclaredBid() + changeBid;
        }

    }

    @Value
    @Builder
    public class FinishedPhase implements GameState {
        @NonNull PlayerId roomOwnerId;
        @NonNull GameRule rule;

        @NonNull PlayerId lastWinnerId;
        @NonNull List<PlayerId> playerIds;

        @NonNull ScoreBoard scoreBoard;
        @Builder.Default
        @NonNull List<GameEvent> eventQueue = new ArrayList<>();

        public List<GameEvent> getEventQueue() {
            return List.copyOf(eventQueue);
        }

        @Override
        public GameStateType getStateName() {
            return GameStateType.GAME_FINISHED;
        }

        public void addGameEvent(GameEvent event) {
            eventQueue.add(event);
        }

        public Map<PlayerId, Integer> aggregateResult() {
            final var aggregated = new HashMap<PlayerId, Integer>();
            for (final var roundScore : scoreBoard.getRoundScores()) {
                for (final var entry : roundScore.entrySet()) {
                    aggregated.compute(
                            entry.getKey(),
                            (k, v) -> v == null ? entry.getValue().getAll() : v + entry.getValue().getAll());
                }
            }

            return aggregated;
        }

        public PlayerId getGameWinnerId() {
            return aggregateResult()
                    .entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElseThrow();
        }

        public BiddingPhase replayGame() {
            return BiddingPhase.newGame(roomOwnerId, rule, lastWinnerId, playerIds);
        }

    }

    @Value
    @Builder
    class BattleResult {
        int baseBonus;
        int piratesCount;
        boolean skulkingAppeared;
        PlayedCard firstMermaid;
        PlayedCard winner;
    }

    @Value
    @Builder
    class PlayedCard {
        PlayerId playerId;
        Card card;
    }

    interface TrickResult {
    }

    interface TrickFinishResult extends TrickResult {
        PlayerId getWinnerId();
    }

    @Value
    @Builder
    class APlayerWon implements TrickFinishResult {
        PlayerId winnerId;
        Card card;
        int trickBonus;
        PiratesEvent piratesEvent;
    }

    @Value
    @Builder
    class AllRanAway implements TrickFinishResult {
        PlayerId winnerId;
        Card card;
    }

    @Value
    @Builder
    class KrakenAppeared implements TrickFinishResult {
        PlayerId mustHaveWon;

        public PlayerId getWinnerId() {
            return mustHaveWon;
        }
    }

    @Value
    @Builder
    class RoundFinished implements TrickResult {
        TrickFinishResult trickFinishResult;
    }

    @Value
    @Builder
    class GameFinished implements TrickResult {
        TrickFinishResult trickFinishResult;
    }

}
