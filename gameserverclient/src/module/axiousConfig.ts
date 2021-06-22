import axios from "axios";
import {GameServerApiClient} from "../clients/GameServerApiClient";

const instance = axios.create({
  baseURL: 'http://localhost:3000'
});

export default instance;

export const gameServerApiClient = new GameServerApiClient(instance);