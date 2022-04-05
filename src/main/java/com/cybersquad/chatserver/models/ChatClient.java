package com.cybersquad.chatserver.models;

import com.cybersquad.chatserver.services.ClientMessageThread;

public class ChatClient {
	private final String chatClientID;
	private ChatRoom chatRoom;
	private ClientMessageThread clientMessageThread;

	public ChatClient(String chatClientID, ClientMessageThread messageThread) {
		this.chatRoom = null;
		this.chatClientID = chatClientID;
		this.clientMessageThread = messageThread;
	}

	public void deleteMessageThread() {
		this.clientMessageThread = null;
	}

	public synchronized String getChatClientID() {
		return this.chatClientID;
	}

	public synchronized ChatRoom getChatRoom() {
		return this.chatRoom;
	}

	public synchronized ClientMessageThread getClientMessageThread() {
		return this.clientMessageThread;
	}

	public void setChatRoom(ChatRoom chatRoom) {
		this.chatRoom = chatRoom;
	}
}
