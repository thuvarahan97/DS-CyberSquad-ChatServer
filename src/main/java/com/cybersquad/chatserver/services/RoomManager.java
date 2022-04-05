package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getRoomChangeBroadcast;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.quitOwnerReply;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.cybersquad.chatserver.ChatServer;
import com.cybersquad.chatserver.models.ChatClient;
import com.cybersquad.chatserver.models.ChatRoom;
import com.cybersquad.chatserver.models.Server;

public class RoomManager {

	private static ArrayList<ChatRoom> chatRooms;
	private static ChatRoom mainHall;
	public static Map<String, ChatClient> createRoomSubscribers;
	private ChatServer mServer;

	public RoomManager(ChatServer server) {
		String roomID = "MainHall-" + server.getServerID();
		RoomManager.mainHall = new ChatRoom(roomID, server.getServerID());
		server.addRoom(server.getServerID(), mainHall.getID());
		RoomManager.chatRooms = new ArrayList<>();
		chatRooms.add(RoomManager.mainHall);
		createRoomSubscribers = new HashMap<>();
		this.mServer = server;
	}

	public synchronized void addToMainHall(ChatClient client) {
		RoomManager.mainHall.addMember(client);
		this.mServer.addNewIdentity(client.getChatClientID());
	}

	public synchronized void broadcastMessageToMembers(ChatRoom room, JSONObject jsonObject) {
		ArrayList<ChatClient> members = room.getMembers();
		members.forEach(user -> {
			try {
				if (user.getClientMessageThread() != null) {
					user.getClientMessageThread().MessageReceive(jsonObject);
				}
			} catch (IOException | NullPointerException e) {
				e.printStackTrace();
			}
		});
	}

	public synchronized ChatRoom createRoom(String roomId, ChatClient client) {
		ChatRoom room = new ChatRoom(roomId, client);
		RoomManager.chatRooms.add(room);
		mServer.addRoom(mServer.getServerID(), roomId);
		return room;
	}

	public synchronized void deleteRoom(ChatRoom room) {
		chatRooms.remove(room);
		mServer.removeRoom(room.getID(), mServer.getServerID());
		ArrayList<ChatClient> members = room.getMembers();
		for (ChatClient client : members) {
			JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), room.getID(), mainHall.getID());
			broadcastMessageToMembers(room, message);
			JSONObject message2 = getRoomChangeBroadcast(client.getChatClientID(), room.getID(), mainHall.getID());
			broadcastMessageToMembers(mainHall, message2);
		}
		ArrayList<ChatClient> members_temp = (ArrayList<ChatClient>) members.clone();
		for (ChatClient client : members_temp) {
			room.removeMember(client);
			mainHall.addMember(client);
			client.setChatRoom(mainHall);
		}
	}

	public Server findGlobalRoom(String roomID) {
		Map<String, ArrayList<String>> globalState = mServer.getGlobalServerState();
		for (String key : globalState.keySet()) {
			for (String room : globalState.get(key)) {
				//System.out.println(room);
				if (room.compareTo(roomID) == 0) {
					return mServer.getServer(key);
				}
			}
		}

		return null;
	}

	public boolean findIfOwner(ChatClient client) {
		for (ChatRoom room : RoomManager.chatRooms) {
			if (room.getOwner().equals(client)) {
				return true;
			}
		}
		return false;
	}

	public ChatClient findOwnerOfRoom(String roomId) {
		for (ChatRoom room : RoomManager.chatRooms) {
			if (room.getID().equalsIgnoreCase(roomId)) {
				return room.getOwner();
			}
		}
		return null;
	}

	public ChatRoom findRoomExists(String roomId) {
		for (ChatRoom room : RoomManager.chatRooms) {
			if (room.getID().equalsIgnoreCase(roomId)) {
				return room;
			}
		}
		return null;
	}

	public ChatRoom getMainHall() {
		return RoomManager.mainHall;
	}

	public ArrayList<String> getRoomIDs() {
		return mServer.getRooms();
	}

	public ChatServer getServer() {
		return mServer;
	}

	public synchronized String isAvailableRoomName(String roomId, ChatClient client) {
		for (ChatRoom r : chatRooms) {
			if (r.getID().equalsIgnoreCase(roomId)) {
				return "FALSE";
			}
		}
		switch (ServerManager.isAvailableRoomID(roomId)) {
		case "WAITING":
			createRoomSubscribers.put(roomId, client);
			return "WAITING";
		case "FALSE":
			return "FALSE";
		case "TRUE":
			createRoomSubscribers.put(roomId, client);
			return "TRUE";
		default:
			return "FALSE";
		}
	}

	public synchronized boolean isValidID(String id) {
		if ((id.matches("[a-zA-Z0-9]+")) && (Character.isAlphabetic(id.charAt(0))) && (id.length() >= 3)
				&& (id.length() <= 16)) {
			for (ChatRoom room : chatRooms) {
				if (room.getID().equalsIgnoreCase(id)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public synchronized boolean removeUserFromChatRoom(ChatClient client) {
		ChatRoom room = client.getChatRoom();
		JSONObject quitMessage = quitOwnerReply(client.getChatClientID(), room.getID());
		broadcastMessageToMembers(room, quitMessage);
		room.getMembers().remove(client);
		if (client.equals(room.getOwner())) {
			return true;
		} else {
			return false;
		}
	}
}
