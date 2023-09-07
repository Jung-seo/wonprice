// chatState.ts
import { atom } from "recoil";

export type ChatList = {
  chatRoomId: number;
  memberId: number;
  productId: number;
  deletedAt: string;

  chatRoom: {
    memberId: number;
    deletedAt: string;
  };
  message: {
    messageId: number;
    content: string;
    createdAt: string;
  };
};

export const chatListState = atom<ChatList[]>({
  key: "chatListState",
  default: [],
});

export type Message = {
  messageId: number;
  senderId: number;
  content: string;
  createdAt: string;
};

export const chatRoomState = atom<ChatList[]>({
  key: "chatRoomState",
  default: [],
});

export const chatRoomIdState = atom({
  key: "chatRoomIdState",
  default: 0, // default value
});

export const currentChatRoomIdState = atom<number | null>({
  key: "currentChatRoomId",
  default: null,
});