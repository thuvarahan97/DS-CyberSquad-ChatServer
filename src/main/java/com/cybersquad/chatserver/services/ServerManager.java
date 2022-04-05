package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.GossipingProtocol.gossipMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.coordinatorMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.electionMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.informCoordinatorMessage;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.newtIdentityApprovalRequest;
import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.newtRoomIdApprovalRequest;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;

import com.cybersquad.chatserver.ChatServer;
import com.cybersquad.chatserver.models.Server;
import com.cybersquad.chatserver.protocols.LeaderElectionProtocol;

public class ServerManager {
	private static ServerManager serverManager;
	private static ChatServer mServer;

	public static ServerManager getInstance(ChatServer server) {
		if (serverManager == null) {
			serverManager = new ServerManager(server);
		}
		return serverManager;
	}

	public static ChatServer getServer() {
		return mServer;
	}

	public static String getServerIdOfRoom(String roomID) {
		Map<String, ArrayList<String>> globalState = mServer.getGlobalServerState();
		for (String key : globalState.keySet()) {
			for (String room : globalState.get(key)) {
				if (room.compareTo(roomID) == 0) {
					return key;
				}
			}
		}
		return null;
	}

	public static void gossipServerState() {
		try {
			send(gossipMessage(mServer.getState(), mServer.getOtherServerIdJSONArray(), mServer.getServerID()), mServer.getRandomNeighbour());
		} catch (IOException e) {
			try {
				send(gossipMessage(mServer.getState(), mServer.getOtherServerIdJSONArray(), mServer.getServerID()), mServer.getRandomNeighbour());
			} catch (IOException ioException) {
				System.out.println("Gossing failed. Server " + mServer.getRandomNeighbour() + " is down.");
			}
		}
	}

	public static void initiateLeaderElection() {
		if (mServer.getIsElectionInProgress()) {
			return;
		}
		System.out.println("Starting leader election process.");
		mServer.setLeader(mServer.getServerID());
		mServer.setIsElectionInProgress(true);
		mServer.setIsOKMessageReceived(false);
		ChatServer.wasLeader = false;
		ArrayList<Server> otherServers = mServer.getOtherServers();
		int count = 0;
		for (Server server : otherServers) {
			try {
				send(electionMessage(mServer.getServerID()), server.getServerID());
				count += 1;
			} catch (IOException e) {
				System.out.println("Election message failed. Server " + server.getServerID() + " is down.");
			}
		}
		if (count == 0) {
			mServer.setLeader(mServer.getServerID());
			mServer.setIsElectionInProgress(false);
			mServer.setIsLeaderAvailableMessageReceived(true);
			System.out.println("Server " + mServer.getServerID() + " is the Leader.");
			for (Server server : otherServers) {
				try {
					send(coordinatorMessage(mServer.getServerID()), server.getServerID());
				} catch (IOException e) {
					System.out.println("Coordinator message failed. Server " + server.getServerID() + " is down.");
				}
			}
			return;
		}
		
		// Inform Coordinator or Send Coordinator Message after 5 seconds
		Timer timer = new Timer();
		TimerTask task = new TimerTask() {
		    @Override
		    public void run() {
		    	if (!mServer.getIsElectionInProgress()) {
            		return;
            	}
            	if (mServer.getLeader() != null && mServer.getLeader() != mServer.getServerID()) {
					try {
						send(informCoordinatorMessage(mServer.getServerID()), mServer.getLeader());
					} catch (IOException e) {
						initiateLeaderElection();
					}
            	} else {
        			mServer.setLeader(mServer.getServerID());
        			mServer.setIsElectionInProgress(false);
					mServer.setIsLeaderAvailableMessageReceived(true);
        			System.out.println("Server " + mServer.getServerID() + " is the Leader.");
            		for (Server server : otherServers) {
        				try {
        					send(coordinatorMessage(mServer.getServerID()), server.getServerID());
        				} catch (IOException e) {
        					System.out.println("Coordinator message failed. Server " + server.getServerID() + " is down.");
        				}
        			}
        			return;
            	}
		        timer.cancel();
		    }   
		};
		timer.schedule(task, TimeUnit.MILLISECONDS.convert(LeaderElectionProtocol.timeout_informcoordinator, TimeUnit.SECONDS));
	}

	public static String isAvailableIdentity(String identity) {
		if (mServer.getGlobalIdentity().contains(identity)) {
			return "FALSE";
		} else {
			if (!ChatServer.isLeader()) {
				JSONObject newtIdentityApprovalRequest = newtIdentityApprovalRequest(mServer.getServerID(), identity);
				if (!mServer.getLeader().isEmpty()) {
					try {
						send(newtIdentityApprovalRequest, mServer.getLeader());
						return "WAITING";
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println(
								"New identity request is declined. Leader server " + mServer.getLeader() + " is down.");
						initiateLeaderElection();
						return "FALSE";
					}
				}
			} else {
				return "TRUE";
			}
		}
		return "FALSE";
	}

	public static String isAvailableRoomID(String roomID) {
		if (getServerIdOfRoom(roomID) != null) {
			return "FALSE";
		} else {
			if (!ChatServer.isLeader()) {
				JSONObject newtRoomIdApprovalRequest = newtRoomIdApprovalRequest(roomID, mServer.getServerID());
				if (!mServer.getLeader().isEmpty()) {
					try {
						send(newtRoomIdApprovalRequest, mServer.getLeader());
						return "WAITING";
					} catch (IOException e) {
						e.printStackTrace();
						System.out.println(
								"New room request is declined. Leader server " + mServer.getLeader() + " is down.");
						initiateLeaderElection();
						return "FALSE";
					}
				} else {
					initiateLeaderElection();
				}
			} else {
				return "TRUE";
			}
		}
		return "FALSE";
	}

	private static void send(JSONObject message, String serverID) throws IOException {
		Server server = mServer.getServer(serverID);
		if (server != null) {
			Socket ss = server.getSocket();
			OutputStream out = ss.getOutputStream();
			out.write((message.toString() + "\n").getBytes(StandardCharsets.UTF_8));
			out.flush();
			out.close();
		}
		System.out.println("Message Sent to Server " + serverID + ": " + message);

	}

	public static void sendBroadcast(JSONObject message) {
		for (Server server : mServer.getOtherServers()) {
			try {
				send(message, server.getServerID());
			} catch (IOException e) {
				System.out.println("Broadcast message failed. Server " + server.getServerID() + " is down.");
				if (server.getServerID() == mServer.getLeader()) {
					initiateLeaderElection();
				}
			}
		}
	}

	private ServerManager(ChatServer server) {
		ServerManager.mServer = server;
	}

	public synchronized void deleteIdentity(String identity) {
		mServer.removeIdentity(identity);
	}

	public synchronized void deleteRoom(String room, String serverID) {
		mServer.removeRoom(room, serverID);
	}
}
