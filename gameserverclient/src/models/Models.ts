export type GameRoom = {
  gameRoomId: string;
  roomOwnerId: string;
  roomOwnerDisplayName: string;
  roomOwnerIconUrl: string;
  gameState: "START_PHASE" | "GAME_PLAYING" | "GAME_FINISHED";
  joinedPlayers: GamePlayer[];
}

export type GamePlayer = {
  playerId: string;
  displayName: string;
  iconUrl: string;
}

export type GetGameRoomsRequest = {
  limit: number;
  exclusiveStartKey?: string;
}

export type GetGameRoomsResponse = {
  gameRooms: GameRoom[];
}

export type UserRegisterRequest = {
  playerName: string;
  playerPassword: string;
}

export type UserRegisterResponse = {
  playerId: string;
}

export type GameRoomCreateRequest = {
  roomSize: number;
  nOfRounds: number;
  deckType: "STANDARD" | "EXPANSION";
}