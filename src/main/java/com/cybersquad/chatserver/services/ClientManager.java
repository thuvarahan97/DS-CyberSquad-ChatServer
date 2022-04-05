package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getCreateRoomReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getMessageBroadcast;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getMoveJoinReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getNewIdentityReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getRoomChangeBroadcast;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getRouteUser;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getWhoReply;
import static com.cybersquad.chatserver.protocols.ServerToServerProtocol.sendDeleteIdenity;
import static com.cybersquad.chatserver.protocols.ServerToServerProtocol.sendDeleteRoom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;

import com.cybersquad.chatserver.ChatServer;
import com.cybersquad.chatserver.models.ChatClient;
import com.cybersquad.chatserver.models.ChatRoom;
import com.cybersquad.chatserver.models.Server;

public class ClientManager {

	private static ArrayList<ChatClient> chatClients;
	public static RoomManager roomManager;
	private static Map<String, ClientMessageThread> identitySubscribers;

	public static void replyIdentityRequest(String identity, boolean approved) {
		if (identitySubscribers.containsKey(identity)) {
			if (approved) {
				System.out.println("New identity has been approved.");
				ClientMessageThread clientMessageThread = identitySubscribers.get(identity);
				ChatClient client = new ChatClient(identity, clientMessageThread);
				chatClients.add(client);
				client.setChatRoom(roomManager.getMainHall());
				roomManager.addToMainHall(client);
				clientMessageThread.setClient(client);
				try {
					clientMessageThread.send(getNewIdentityReply(client.getChatClientID(), true));
					sendMainhallBroadcast(client);
				} catch (IOException e) {
					e.printStackTrace();
				}
				identitySubscribers.remove(identity);
				if (ChatServer.isLeader()) {
					ServerManager.gossipServerState();
				}
			} else {
				System.out.println("New identity request is declined.");
				ClientMessageThread clientMessageThread = identitySubscribers.get(identity);
				identitySubscribers.remove(identity);
				try {
					clientMessageThread.send(getNewIdentityReply(identity, false));
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public static void replyNewRoomRequest(String roomid, boolean approved) {
		Map<String, ChatClient> roomSubs = RoomManager.createRoomSubscribers;
		if (roomSubs.containsKey(roomid)) {
			ChatClient client = roomSubs.get(roomid);
			ClientMessageThread clientMessageThread = client.getClientMessageThread();
			if (approved) {
				System.out.println("New room has been approved.");
				ChatRoom previousroom = client.getChatRoom();
				ChatRoom newroom = roomManager.createRoom(roomid, client);
				client.setChatRoom(newroom);
				roomSubs.remove(roomid);
				try {
					clientMessageThread.send(getCreateRoomReply(roomid, true));
					sendRoomChangeBroadcast(client, previousroom, newroom);
				} catch (IOException ioException) {
					ioException.printStackTrace();
				}
				previousroom.removeMember(client);
				if (ChatServer.isLeader()) {
					ServerManager.gossipServerState();
				}
			} else {
				System.out.println("New room request is declined.");
				roomSubs.remove(roomid);
				try {
					clientMessageThread.send(getCreateRoomReply(roomid, false));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void sendMainhallBroadcast(ChatClient client) {
		JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), "", roomManager.getMainHall().getID());
		roomManager.broadcastMessageToMembers(roomManager.getMainHall(), message);
	}

	public static void sendRoomChangeBroadcast(ChatClient client, ChatRoom formerRoom, ChatRoom newRoom) {
		JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), formerRoom.getID(), newRoom.getID());
		roomManager.broadcastMessageToMembers(formerRoom, message);
		roomManager.broadcastMessageToMembers(newRoom, message);
	}

	public ClientManager(RoomManager manager) {
		chatClients = new ArrayList<>();
		ClientManager.roomManager = manager;
		identitySubscribers = new HashMap<>();
	}

	public synchronized boolean chatClientQuit(ChatClient client) {
		if (client != null) {
			chatClients.remove(client);
			roomManager.getServer().removeIdentity(client.getChatClientID());
			JSONObject deleteMessage = sendDeleteIdenity(client.getChatClientID());
			ServerManager.sendBroadcast(deleteMessage);
			boolean isOwner = roomManager.removeUserFromChatRoom(client);
			return isOwner;
		}
		return false;

	}

	public synchronized boolean clientDeleteRoom(ChatClient client, String roomId) {
		ChatClient owner = roomManager.findOwnerOfRoom(roomId);
		if (owner != client) {
			return false;
		} else if (owner == null) {
			return false;
		}
		ownerDeleteRoom(client);
		JSONObject deleteMessage = sendDeleteRoom(roomId, roomManager.getServer().getServerID());
		ServerManager.sendBroadcast(deleteMessage);
		return true;
	}

	public synchronized String isAvailableIdentity(String identity, ClientMessageThread clientMessageThread) {
		for (ChatClient clients : chatClients) {
			if (clients.getChatClientID().equalsIgnoreCase(identity)) {
				return "FALSE";
			}
		}
		switch (ServerManager.isAvailableIdentity(identity)) {
		case "WAITING":
			identitySubscribers.put(identity, clientMessageThread);
			return "WAITING";
		case "FALSE":
			return "FALSE";
		case "TRUE":
			identitySubscribers.put(identity, clientMessageThread);
			return "TRUE";
		default:
			return "FALSE";
		}
	}

	public synchronized boolean joinRoom(ChatClient client, String roomID) {
		ChatRoom previousRoom = client.getChatRoom();
		if (roomManager.findIfOwner(client)) {
			return false;
		}
		ChatRoom joinRoom = roomManager.findRoomExists(roomID);
		//System.out.println(joinRoom);
		if (joinRoom != null) {
			client.setChatRoom(joinRoom);
			previousRoom.removeMember(client);
			joinRoom.addMember(client);
			JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), previousRoom.getID(),
					joinRoom.getID());
			roomManager.broadcastMessageToMembers(previousRoom, message);
			roomManager.broadcastMessageToMembers(joinRoom, message);
			return true;
		} else {
			Server s = roomManager.findGlobalRoom(roomID);
			if (s == null) {
				return false;
			} else {
				previousRoom.removeMember(client);
				JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), previousRoom.getID(), roomID);
				roomManager.broadcastMessageToMembers(previousRoom, message);
				chatClients.remove(client);
				JSONObject routeMessage = getRouteUser(client.getChatClientID(), s.getIPAddress(), roomID,
						String.valueOf(s.getClientPort()));
				try {
					client.getClientMessageThread().send(routeMessage);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return true;
			}
		}
	}

	public ArrayList<String> listGlobalRoomIds() {
		ArrayList<String> roomIDs = roomManager.getRoomIDs();
		return roomIDs;
	}

	public JSONObject listRoomDetails(ChatClient client) {
		ChatRoom room = client.getChatRoom();
		JSONObject message = getWhoReply(room.getID(), room.getUserIDs(), room.getOwner().getChatClientID());
		return message;
	}

	public synchronized void moveJoinRoom(String identity, String joinRoomId, ClientMessageThread recieveThread,
			String formerRoomId) {
		ChatRoom room = roomManager.findRoomExists(joinRoomId);
		ChatClient client = new ChatClient(identity, recieveThread);
		recieveThread.setClient(client);
		JSONObject movejoinreply = getMoveJoinReply(identity, true, roomManager.getServer().getServerID());
		try {
			client.getClientMessageThread().send(movejoinreply);
			chatClients.add(client);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (room != null) {
			room.addMember(client);
			client.setChatRoom(room);
			JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), formerRoomId, room.getID());
			roomManager.broadcastMessageToMembers(room, message);
		} else {
			ChatRoom mainHall = roomManager.getMainHall();
			mainHall.addMember(client);
			client.setChatRoom(mainHall);
			JSONObject message = getRoomChangeBroadcast(client.getChatClientID(), formerRoomId, mainHall.getID());
			roomManager.broadcastMessageToMembers(mainHall, message);
		}
	}

	public synchronized boolean newIdentity(String identity, ClientMessageThread clientMessageThread) {
		if (!roomManager.isValidID(identity)) {
			return false;
		}
		switch (isAvailableIdentity(identity, clientMessageThread)) {
		case "WAITING":
			System.out.println("Client is waiting for identity approval.");
			return true;
		case "FALSE":
			System.out.println("Client " + identity + " is already in use.");
			return false;
		case "TRUE":
			replyIdentityRequest(identity, true);
			return true;
		default:
			return false;
		}
	}

	public synchronized boolean newRoom(String roomID, ChatClient client) {
		if (!roomManager.isValidID(roomID)) {
			return false;
		}
		switch (roomManager.isAvailableRoomName(roomID, client)) {
		case "WAITING":
			System.out.println("Client is waiting for room approval.");
			return true;
		case "FALSE":
			System.out.println("Room " + roomID + " is already in use.");
			return false;
		case "TRUE":
			replyNewRoomRequest(roomID, true);
			return true;
		default:
			return false;
		}
	}

	public synchronized void ownerDeleteRoom(ChatClient client) {
		ChatRoom room = client.getChatRoom();
		roomManager.deleteRoom(room);
	}

	public void sendMessage(String content, ChatClient user) {
		JSONObject message = getMessageBroadcast(content, user.getChatClientID());
		roomManager.broadcastMessageToMembers(user.getChatRoom(), message);
	}
}
