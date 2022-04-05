package com.cybersquad.chatserver.models;

import java.util.ArrayList;

public class ChatRoom {
	private final String id;
	private ArrayList<ChatClient> members = new ArrayList<>();
	private final ChatClient owner;

	public ChatRoom(String roomId, ChatClient owner) {
		this.id = roomId;
		this.owner = owner;
		this.members.add(owner);
	}

	public ChatRoom(String roomId, String serverid) {
		this.id = roomId;
		this.owner = new ChatClient(serverid, null);
	}

	public synchronized boolean addMember(ChatClient user) {
		if (!members.contains(user)) {
			members.add(user);
			return true;
		} else {
			return false;
		}
	}

	public synchronized String getID() {
		return id;
	}

	public synchronized ArrayList<ChatClient> getMembers() {
		return members;
	}

	public synchronized ChatClient getOwner() {
		return owner;
	}

	public synchronized ArrayList<String> getUserIDs() {
		ArrayList<String> userIds = new ArrayList<>();
		if (members.size() > 0) {
			members.forEach(user -> userIds.add(user.getChatClientID()));
		}
		return userIds;
	}

	public synchronized boolean removeMember(ChatClient user) {
		if (user.equals(owner)) {
			return false;
		}
		members.remove(user);
		return true;
	}
}
