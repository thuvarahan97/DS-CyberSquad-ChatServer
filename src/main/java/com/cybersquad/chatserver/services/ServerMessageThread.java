package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.GossipingProtocol.gossipMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.availableMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.coordinatorMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.newIdentityApprovalReply;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.newtRoomIdApprovalReply;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.okMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.cybersquad.chatserver.ChatServer;
import com.cybersquad.chatserver.models.Server;
import com.cybersquad.chatserver.protocols.LeaderElectionProtocol;

public class ServerMessageThread implements Runnable {
	private static ServerManager serverManager;
	private ServerSocket serverServerSocket;
	private BufferedReader input;
	private JSONParser parser = new JSONParser();
	private final AtomicBoolean running = new AtomicBoolean(true);

	public ServerMessageThread(ServerSocket serverServerSocket, ServerManager manager) throws IOException {
		this.serverServerSocket = serverServerSocket;
		ServerMessageThread.serverManager = manager;
	}

	private static void MessageReceive(JSONObject message) throws IOException {
		String type = (String) message.get("type");
		String kind = (String) message.get("kind");

		switch (type) {
		case "deleteidenity":
			String deleteIdentity = (String) message.get("identity");
			serverManager.deleteIdentity(deleteIdentity);
			break;

		case "deleteroom":
			String deleteRoom = (String) message.get("roomid");
			String roomServerId = (String) message.get("serverid");
			serverManager.deleteRoom(deleteRoom, roomServerId);
			break;

		case "bully":
			String serverID = (String) message.get("serverid");
			switch (kind) {
			case "ELECTION":
				if (serverID != ServerManager.getServer().getServerID()) {
					ChatServer.wasLeader = false;
					if (ChatServer.isLeader()) {
						ChatServer.wasLeader = true;
					}
					ServerManager.getServer().setIsElectionInProgress(true);
					System.out.println("Leader election process has been started by the server " + serverID + ".");
					ServerManager.getServer().setLeader(null);
				}
				if (serverID.compareTo(ServerManager.getServer().getServerID()) < 0) {
					try {
						send(okMessage(ServerManager.getServer().getServerID()), serverID);
						ServerManager.getServer();
						if (ChatServer.isLeader()) {
							send(coordinatorMessage(ServerManager.getServer().getServerID()), serverID);
						}
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println("OK or Coordinator message failed. Server " + serverID + " is down.");
					}
				}
				Timer timer = new Timer();
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						ChatServer server = ServerManager.getServer();
						if (!ChatServer.isLeader() && server.getIsElectionInProgress()) {
							server.setIsElectionInProgress(false);
							ServerManager.initiateLeaderElection();
						}
						timer.cancel();
					}
				};
				timer.schedule(task, TimeUnit.MILLISECONDS.convert(LeaderElectionProtocol.timeout_leaderelection, TimeUnit.SECONDS));
				break;

			case "COORDINATOR":
				if (serverID.compareTo(ServerManager.getServer().getServerID()) < 0) {
					ServerManager.initiateLeaderElection();
				} else {
					ServerManager.getServer().setLeader(serverID);
					ServerManager.getServer().setIsElectionInProgress(false);
					ServerManager.getServer().setIsLeaderAvailableMessageReceived(true);
					System.out.println("Server " + serverID + " has been elected as Leader.");
					if ((ServerManager.getServer().getLeader() != null
							&& ServerManager.getServer().getLeader().equals(serverID)) || ChatServer.wasLeader()) {
						try {
							send(gossipMessage(ServerManager.getServer().getState(),
									ServerManager.getServer().getOtherServerIdJSONArray(), 
									ServerManager.getServer().getServerID()), serverID);
						} catch (IOException e) {
							ServerManager.initiateLeaderElection();
						}
					}
					ChatServer.wasLeader = false;
				}
				break;

			case "OK":
				ServerManager.getServer().setIsOKMessageReceived(true);
				System.out.println("OK message is received from server " + serverID + ".");
				if (ServerManager.getServer().getIsElectionInProgress()) {
					ServerManager.getServer().setIsOKMessageReceived(false);
					if (serverID.compareTo(ServerManager.getServer().getServerID()) > 0) {
						ServerManager.getServer().setLeader(serverID);
						ChatServer.wasLeader = false;
					}
				}
				break;

			case "CHECK":
				if (serverID != ServerManager.getServer().getServerID()) {
					if (ChatServer.isLeader()) {
						try {
							send(availableMessage(ServerManager.getServer().getServerID()), serverID);
						} catch (IOException e) {
							e.printStackTrace();
							System.out.println("Leader availability checking server " + serverID + " is down.");
						}
					}
				}
				break;

			case "AVAILABLE":
				ServerManager.getServer().setIsLeaderAvailableMessageReceived(true);
				if (ChatServer.isDebugLeaderCheck) {
					System.out.println("Leader server " + serverID + " is available.");
				}
				break;

			case "INFORM":
				for (Server server : ServerManager.getServer().getOtherServers()) {
					try {
						send(coordinatorMessage(ServerManager.getServer().getServerID()), server.getServerID());
					} catch (IOException e) {
						System.out.println("Coordinator message failed. Server " + server.getServerID() + " is down.");
					}
				}
				ServerManager.getServer().setLeader(ServerManager.getServer().getServerID());
				ServerManager.getServer().setIsElectionInProgress(false);
				System.out.println("Server " + ServerManager.getServer().getServerID() + " is the leader.");
				ChatServer.wasLeader = false;
				return;

			default:
				System.out.println("Incorrect Message: " + message);
			}
			break;

		case "gossip":
			switch (kind) {
			case "STATEUPDATE":
				JSONObject state = (JSONObject) message.get("state");
				ServerManager.getServer().updateState(state);
				JSONArray neighbour = (JSONArray) message.get("gossipservers");
				neighbour.remove(ServerManager.getServer().getServerID());
				System.out.println("Gossip Neighbour Servers: " + neighbour);
				if (neighbour.size() > 0) {
					String gossipNeighbour = (String) neighbour.get(new Random().nextInt(neighbour.size()));
					message = gossipMessage(state, neighbour, ServerManager.getServer().getServerID());
					try {
						send(message, gossipNeighbour);
					} catch (IOException ex) {
						System.out.println("Gossiping to Neighbour Server " + gossipNeighbour + " failed.");
						gossipNeighbour = (String) neighbour.get(new Random().nextInt(neighbour.size()));
						System.out.println("Retrying gossiping to Neighbour Server " + gossipNeighbour + ".");
						try {
							send(message, gossipNeighbour);
						} catch (IOException e) {
							System.out.println("Gossiping failed. Neighbour Server " + gossipNeighbour + " is down.");
						}
					}
				}
				break;
			default:
				System.out.println("Incorrect Message: " + message);
			}
			break;

		case "confirmIdentity":
			String identity = (String) message.get("identity");
			System.out.println("Received approval confirmation for identity " + identity + " from the leader.");
			boolean approved = (boolean) message.get("approved");
			ClientManager.replyIdentityRequest(identity, approved);
			break;

		case "requestIdentityApproval":
			String i = (String) message.get("identity");
			String serverid = (String) message.get("serverid");
			System.out
					.println("Approval request for identity " + i + " has been received from server " + serverid + ".");
			if (ServerManager.getServer().getGlobalIdentity().contains(i)) {
				send(newIdentityApprovalReply(false, i), serverid);
			} else {
				ServerManager.getServer().addNewIdentity(i);
				send(newIdentityApprovalReply(true, i), serverid);
				try {
					send(gossipMessage(ServerManager.getServer().getState(),
							ServerManager.getServer().getOtherServerIdJSONArray(),
							ServerManager.getServer().getServerID()),
							ServerManager.getServer().getRandomNeighbour());
				} catch (IOException e) {
					try {
						send(gossipMessage(ServerManager.getServer().getState(),
								ServerManager.getServer().getOtherServerIdJSONArray(),
								ServerManager.getServer().getServerID()),
								ServerManager.getServer().getRandomNeighbour());
					} catch (IOException ioException) {
						System.out.println("Gossiping failed. A neighbour server is down.");
					}
				}
			}
			break;

		case "requestRoomIDApproval":
			String rID = (String) message.get("roomid");
			String sID = (String) message.get("serverid");
			System.out.println("Received approval request for roomID " + rID + " from server " + sID + ".");
			if (ServerManager.getServer().getRooms().contains(rID)) {
				send(newtRoomIdApprovalReply(false, rID), sID);
			} else {
				ServerManager.getServer().addRoom(sID, rID);
				send(newtRoomIdApprovalReply(true, rID), sID);
			}
			try {
				send(gossipMessage(ServerManager.getServer().getState(),
						ServerManager.getServer().getOtherServerIdJSONArray(),
						ServerManager.getServer().getServerID()),
						ServerManager.getServer().getRandomNeighbour());
			} catch (IOException e) {
				try {
					send(gossipMessage(ServerManager.getServer().getState(),
							ServerManager.getServer().getOtherServerIdJSONArray(),
							ServerManager.getServer().getServerID()),
							ServerManager.getServer().getRandomNeighbour());
				} catch (IOException ioException) {

				}
			}
			break;

		case "confirmRoomID":
			String rid = (String) message.get("roomid");
			boolean ridApproved = (boolean) message.get("approved");
			System.out.println("Received approval confirmation for roomID " + rid + " from the leader.");
			ClientManager.replyNewRoomRequest(rid, ridApproved);
			break;

		default:
			System.out.println("Incorrect Message: " + message);
		}
	}

	public static void send(JSONObject message, String serverID) throws IOException {
		Server server = ServerManager.getServer().getServer(serverID);
		if (server != null) {
			Socket ss = server.getSocket();
			OutputStream out = ss.getOutputStream();
			out.write((message.toString() + "\n").getBytes(StandardCharsets.UTF_8));
			out.flush();
		}
		checkDebugAndPrint(message, "Message Sent to Server " + serverID + ": " + message);
	}

	@Override
	public void run() {
		try {
			ServerManager.initiateLeaderElection();
			while (this.running.get()) {
				Socket serverSocket = serverServerSocket.accept();
				this.input = new BufferedReader(
						new InputStreamReader(serverSocket.getInputStream(), StandardCharsets.UTF_8));
				if (this.input != null) {
					String input_line = this.input.readLine();
					if (input_line != null) {
						JSONObject message = (JSONObject) parser.parse(input_line);
						if (message.containsKey("serverid")) {
							checkDebugAndPrint(message, "Message Received from Server " + message.get("serverid") + ": " + message);
						} else {
							checkDebugAndPrint(message, "Server Message Received: " + message);
						}
						MessageReceive(message);
					}
				}
			}
			this.input.close();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}

	public static void checkDebugAndPrint(JSONObject message, String printStatement) {
		if (message.containsKey("kind")
				&& (message.get("kind").equals("CHECK") || message.get("kind").equals("AVAILABLE"))) {
			if (ChatServer.isDebugLeaderCheck()) {
				System.out.println(printStatement);
			}
		} else {
			System.out.println(printStatement);
		}
	}

}
