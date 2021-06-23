export type GameRoom = {
  gameRoomId: string;
  roomOwnerId: string;
  gameState: "START_PHASE" | "GAME_PLAYING" | "GAME_FINISHED";
  joinedPlayerIds: string[];
}

export type GamePlayer = {
  playerId: string;
  playerName: string;
  icon: string;
}

export type GetGameRoomsRequest = {
  limit: number;
  exclusiveStartKey?: string;
}

export type UserRegisterRequest = {
  playerName: string;
  playerPassword: string;
}

export type UserRegisterResponse = {
  playerId: string;
}