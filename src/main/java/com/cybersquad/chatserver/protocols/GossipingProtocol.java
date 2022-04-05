package com.cybersquad.chatserver.protocols;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class GossipingProtocol {
	public static JSONObject gossipMessage(JSONObject state, JSONArray gossipServerList, String serverID) {
		JSONObject msg = new JSONObject();
		msg.put("type", "gossip");
		msg.put("kind", "STATEUPDATE");
		msg.put("state", state);
		msg.put("gossipservers", gossipServerList);
		msg.put("serverid", serverID);
		return msg;
	}
}