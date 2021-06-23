import axios, {AxiosError, AxiosInstance} from "axios";
import {GamePlayer, GameRoom, GetGameRoomsRequest, UserRegisterRequest, UserRegisterResponse} from "../models/Models";

export class GameServerApiClient {

  constructor(private readonly axiosInstance: AxiosInstance) {
  }

  checkLogin: () => Promise<GamePlayer> = async () => {
    return await this.post("/checkLogin", {});
  }

  login: (userid: string, password: string) => Promise<boolean> = async (userid, password) => {
    const params = new URLSearchParams();
    params.append('username', userid);
    params.append('password', password);
    const {status} = await this.axiosInstance.post("/login", params);
    return status / 100 === 2;
  }

  logout = async () => {
    await this.post("/logout", {});
  };

  register: (userRegisterRequest: UserRegisterRequest) => Promise<UserRegisterResponse> = async (request) => {
    return await this.post("/players/register", request);
  }

  getGameRoom: (gameRoomId: string) => Promise<GameRoom> = async (gameRoomId) => {
    return await this.get(`/gamerooms/${gameRoomId}`);
  }

  getGameRooms: (request: GetGameRoomsRequest) => Promise<GameRoom> = async (request) => {
    return await this.post("/gamerooms/", request);
  }

  private get: <T, R>(path: string) => Promise<R> = async (path) => {
    try {
      const {data} = await this.axiosInstance.get(path);
      return data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        this.handleAxiosError(error);
      } else {
        this.handleUnexpectedError(error);
      }
    }
  }

  private post: <T, R>(path: string, body: T) => Promise<R> = async (path, body) => {
    try {
      const {data} = await this.axiosInstance.post(path, body);
      return data;
    } catch (error) {
      if (axios.isAxiosError(error)) {
        this.handleAxiosError(error);
      } else {
        this.handleUnexpectedError(error);
      }
    }
  }

  private handleAxiosError = (e: AxiosError) => {
  }

  private handleUnexpectedError = (e: Error) => {
  }

}