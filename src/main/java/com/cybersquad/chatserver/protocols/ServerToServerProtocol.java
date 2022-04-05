package com.cybersquad.chatserver.protocols;

import org.json.simple.JSONObject;

public class ServerToServerProtocol {

	public static JSONObject sendDeleteIdenity(String identity) {
		JSONObject msg = new JSONObject();
		msg.put("type", "deleteidenity");
		msg.put("identity", identity);
		return msg;
	}

	public static JSONObject sendDeleteRoom(String roomId, String serverId) {
		JSONObject msg = new JSONObject();
		msg.put("type", "deleteroom");
		msg.put("roomid", roomId);
		msg.put("serverid", serverId);
		return msg;
	}
}
