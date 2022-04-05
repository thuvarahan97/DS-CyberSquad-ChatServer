package com.cybersquad.chatserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.cybersquad.chatserver.models.Server;

public class ChatServer extends Server {
	
	public static boolean isDebugLeaderCheck = false;
	public static boolean isDebugLeaderCheck() {
		return isDebugLeaderCheck;
	}

	// Whether this server is the leader server
	private static boolean isLeader = false;
	public static boolean isLeader() {
		return isLeader;
	}
	
	// Whether this server was the leader server before leader election
	public static boolean wasLeader = false;
	public static boolean wasLeader() {
		return wasLeader;
	}

	private ArrayList<Server> otherServers = new ArrayList<>();

	private ArrayList<String> globalIdentity;
	private Map<String, ArrayList<String>> globalServerState;

	// Leader Server ID
	private String leader;
	
	private boolean isElectionInProgress = false;
	private boolean isOKMessageReceived = false;
	private boolean isLeaderAvailableMessageReceived = true;

	public ChatServer(String serverId, String ipAddress, int clientPort, int coordinationPort) {
		super(serverId, ipAddress, clientPort, coordinationPort);
		this.globalIdentity = new ArrayList<>();
		this.globalServerState = new HashMap<>();
	}

	public boolean getIsElectionInProgress() {
		return this.isElectionInProgress;
	}

	public void setIsElectionInProgress(boolean value) {
		this.isElectionInProgress = value;
	}

	public boolean getIsOKMessageReceived() {
		return isOKMessageReceived;
	}

	public void setIsOKMessageReceived(boolean value) {
		this.isOKMessageReceived = value;
	}

	public boolean getisLeaderAvailableMessageReceived() {
		return this.isLeaderAvailableMessageReceived;
	}

	public void setIsLeaderAvailableMessageReceived(boolean value) {
		this.isLeaderAvailableMessageReceived = value;
	}

	public void addNewIdentity(String identity) {
		this.globalIdentity.add(identity);

	}

	public void addRoom(String server, String room) {
		if (globalServerState.containsKey(server)) {
			ArrayList<String> s = globalServerState.get(server);
			if (!s.contains(room)) {
				s.add(room);
			}
		} else {
			ArrayList<String> r = new ArrayList<>();
			r.add(room);
			globalServerState.put(server, r);
		}
	}

	public synchronized void addServer(Server server) {
		this.otherServers.add(server);
	}

	public ArrayList<String> getGlobalIdentity() {
		return this.globalIdentity;
	}

	public Map<String, ArrayList<String>> getGlobalServerState() {
		return globalServerState;
	}

	public JSONArray getIdentityJSONArray() {
		JSONArray arr = new JSONArray();
		globalIdentity.forEach(i -> arr.add(i));
		return arr;
	}

	public String getLeader() {
		return this.leader;
	}

	public JSONArray getOtherServerIdJSONArray() {
		JSONArray arr = new JSONArray();
		otherServers.forEach(s -> arr.add(s.getServerID()));
		return arr;
	}

	public ArrayList<String> getOtherServerIDs() {
		ArrayList<String> arr = new ArrayList<>();
		otherServers.forEach(s -> arr.add(s.getServerID()));
		return arr;
	}

	public ArrayList<Server> getOtherServers() {
		return this.otherServers;
	}

	public String getRandomNeighbour() {
		return getOtherServerIDs().get(new Random().nextInt(otherServers.size()));
	}

	public ArrayList<String> getRooms() {
		ArrayList<String> rooms = new ArrayList<>();
		globalServerState.keySet().forEach(key -> globalServerState.get(key).forEach(room -> {
			if (!rooms.contains(room)) {
				rooms.add(room);

			}
		}));
		return rooms;
	}

	public Server getServer(String id) {
		for (Server server : otherServers) {
			if (server.getServerID().equals(id)) {
				return server;
			}
		}
		return null;
	}

	public JSONObject getState() {
		JSONObject state = new JSONObject();
		state.put("serverRooms", new JSONObject(globalServerState));
		state.put("identity", getIdentityJSONArray());
		return state;
	}

	public synchronized void removeIdentity(String clientID) {
		globalIdentity.remove(clientID);
	}

	public synchronized void removeRoom(String roomId, String serverId) {
		if (globalServerState.containsKey(serverId)) {
			if (globalServerState.get(serverId).contains(roomId)) {
				globalServerState.get(serverId).remove(roomId);
			}
		}
	}

	public void setLeader(String serverId) {
		if (serverId != null && serverId.compareTo(this.getServerID()) == 0) {
			ChatServer.isLeader = true;
		} else {
			ChatServer.isLeader = false;
		}
		this.leader = serverId;
	}

	public void updateState(JSONObject state) {
		Map<String, JSONArray> serverRooms = (Map<String, JSONArray>) state.get("serverRooms");
		JSONArray identity = (JSONArray) state.get("identity");
		for (Object i : identity) {
			String identityString = (String) i;
			if (!this.globalIdentity.contains(identityString)) {
				this.globalIdentity.add(identityString);
			}
		}
		serverRooms.keySet().forEach(server -> {
			if (server != this.getServerID()) {
				if (this.globalServerState.containsKey(server)) {
					for (Object s : serverRooms.get(server)) {
						String serverString = (String) s;
						if (!globalServerState.get(server).contains(serverString)) {
							System.out.println(server + " : " + serverString);
							this.globalServerState.get(server).add(serverString);
						}
					}
				}
			}
		});
	}
}
