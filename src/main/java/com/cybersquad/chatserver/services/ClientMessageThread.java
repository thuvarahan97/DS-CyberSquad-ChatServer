package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getCreateRoomReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getDeleteRoomReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getListReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getNewIdentityReply;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.getRoomChangeBroadcast;
import static com.cybersquad.chatserver.protocols.ServerToClientProtocol.quitOwnerReply;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.cybersquad.chatserver.ChatServer;
import com.cybersquad.chatserver.models.ChatClient;

public class ClientMessageThread implements Runnable {
	private final Socket socket;
	private BufferedReader input;
	private DataOutputStream output;
	private ClientManager manager;
	private JSONParser parser = new JSONParser();
	private ChatClient client;
	private ChatServer server;
	private final AtomicBoolean isRunning = new AtomicBoolean(true);

	public ClientMessageThread(Socket socket, ClientManager manager, ChatServer server) throws IOException {
		this.manager = manager;
		this.socket = socket;
		this.output = new DataOutputStream(socket.getOutputStream());
		this.client = null;
		this.server = server;
	}

	public ChatClient getClient() {
		return client;
	}

	public void MessageReceive(JSONObject message) throws IOException {
		String type = (String) message.get("type");
		String identity;
		switch (type) {
		case "newidentity":
			identity = (String) message.get("identity");
			if (server.getIsElectionInProgress()) {
				System.out.println("New identity request is declined. Leader election is in progress.");
				send(getNewIdentityReply(identity, false));
				this.input.close();
				socket.close();
			} else {
				if (manager.newIdentity(identity, this)) {
					System.out.println(
							"New identity request is waiting for leader server's approval or has been approved.");
				} else {
					System.out.println("New identity request has been declined by the leader.");
					send(getNewIdentityReply(identity, false));
					this.input.close();
					socket.close();
				}
			}
			break;

		case "message":
			String msg = (String) message.get("content");
			if (message.containsKey("identity")) {
				send(message);
			} else {
				manager.sendMessage(msg, this.client);
			}
			break;

		case "list":
			ArrayList<String> roomIds = manager.listGlobalRoomIds();
			send(getListReply(roomIds));
			break;

		case "who":
			JSONObject reply = manager.listRoomDetails(this.client);
			send(reply);
			break;

		case "createroom":
			String roomID = (String) message.get("roomid");
			if (server.getIsElectionInProgress()) {
				System.out.println("New room request is declined. Leader election is in progress.");
				send(getCreateRoomReply(roomID, false));
			} else {
				ChatClient mClient = this.client;
				if (manager.newRoom(roomID, mClient)) {
					System.out.println("New room request is waiting for leader server's approval or has been approved.");
				} else {
					System.out.println("New room request has been declined by the leader.");
					send(getCreateRoomReply(roomID, false));
				}
			}
			break;

		case "joinroom":
			String jrid = (String) message.get("roomid");
			boolean success = manager.joinRoom(this.client, jrid);
			if (!success) {
				send(getRoomChangeBroadcast(this.client.getChatClientID(), jrid, jrid));
			}
			break;

		case "movejoin":
			String joinRoomid = (String) message.get("roomid");
			String formerRoomid = (String) message.get("former");
			String clientIdentity = (String) message.get("identity");
			manager.moveJoinRoom(clientIdentity, joinRoomid, this, formerRoomid);
			break;

		case "deleteroom":
			String rid = (String) message.get("roomid");
			boolean isApproved = manager.clientDeleteRoom(this.client, rid);
			if (isApproved) {
				send(getDeleteRoomReply(rid, true));
			} else {
				send(getDeleteRoomReply(rid, false));
			}
			break;

		case "roomchange":
			send(message);
			break;

		case "quit":
			boolean isOwner = manager.chatClientQuit(this.client);
			try {
				send(quitOwnerReply(client.getChatClientID(), client.getChatRoom().getID()));
				if (isOwner) {
					manager.ownerDeleteRoom(client);
				}
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
			break;
		default:
			System.out.println("Incorrect Message: " + message);
			break;
		}
	}

	@Override
	public void run() {
		try {
			this.input = new BufferedReader(
					new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
			while (this.isRunning.get()) {
				try {
					JSONObject message = (JSONObject) parser.parse(input.readLine());
					System.out.println("Client Message Received: " + message);
					this.MessageReceive(message);
				} catch (NullPointerException ne) {
					isRunning.set(false);
				}
			}
			this.input.close();
			this.socket.close();
		} catch (IOException e) {
			this.client.deleteMessageThread();
			boolean isOwner = manager.chatClientQuit(this.client);
			//System.out.println(this.client);
			if (this.client != null) {
				if (isOwner) {
					manager.ownerDeleteRoom(client);
				}
			}
			isRunning.set(false);
		} catch (ParseException e) {
			System.out.println("Message Error: " + e.getMessage());
		}
	}

	public void send(JSONObject message) throws IOException {
		output.write((message.toString() + "\n").getBytes(StandardCharsets.UTF_8));
		output.flush();
		System.out.println("Client Message Sent:" + message);
	}

	public void setClient(ChatClient client) {
		this.client = client;
	}
}
