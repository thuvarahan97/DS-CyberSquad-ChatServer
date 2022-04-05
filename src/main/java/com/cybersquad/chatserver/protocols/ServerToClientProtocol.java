package com.cybersquad.chatserver.protocols;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONObject;

public class ServerToClientProtocol {

	public static JSONObject getCreateRoomReply(String roomid, Boolean approved) {
		JSONObject msg = new JSONObject();
		msg.put("type", "createroom");
		msg.put("roomid", roomid);
		msg.put("approved", approved.toString());
		return msg;
	}

	public static JSONObject getCreateRoomRequest(String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "createroom");
		msg.put("roomid", roomid);
		return msg;
	}

	public static JSONObject getDeleteRoomReply(String roomid, Boolean approved) {
		JSONObject msg = new JSONObject();
		msg.put("type", "deleteroom");
		msg.put("roomid", roomid);
		msg.put("approved", approved.toString());
		return msg;
	}

	public static JSONObject getDeleteRoomRequest(String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "deleteroom");
		msg.put("roomid", roomid);
		return msg;
	}

	public static JSONObject getJoinRoomRequest(String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "joinroom");
		msg.put("roomid", roomid);
		return msg;
	}

	public static JSONObject getListReply(ArrayList<String> rooms) {
		JSONObject msg = new JSONObject();
		msg.put("type", "roomlist");
		msg.put("rooms", rooms);
		return msg;
	}

	public static JSONObject getListRequest() {
		JSONObject msg = new JSONObject();
		msg.put("type", "list");
		return msg;
	}

	public static JSONObject getMessageBroadcast(String content, String identity) {
		JSONObject msg = new JSONObject();
		msg.put("type", "message");
		msg.put("content", content);
		msg.put("identity", identity);
		return msg;
	}

	public static JSONObject getMessageRequest(String content) {
		JSONObject msg = new JSONObject();
		msg.put("type", "message");
		msg.put("content", content);
		return msg;
	}

	public static JSONObject getMoveJoinReply(String identity, Boolean approved, String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "serverchange");
		msg.put("approved", approved.toString());
		msg.put("serverid", serverid);
		return msg;
	}

	public static JSONObject getMoveJoinRequest(String identity, String former, String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "movejoin");
		msg.put("identity", identity);
		msg.put("former", former);
		msg.put("roomid", roomid);
		return msg;
	}

	public static JSONObject getNewIdentityReply(String identity, Boolean approved) {
		JSONObject msg = new JSONObject();
		msg.put("type", "newidentity");
		msg.put("identity", identity);
		msg.put("approved", approved.toString());
		return msg;
	}

	public static JSONObject getNewIdentityRequest(String identity) {
		JSONObject msg = new JSONObject();
		msg.put("type", "newidentity");
		msg.put("identity", identity);
		return msg;
	}

	public static JSONObject getQuitRequest() {
		JSONObject msg = new JSONObject();
		msg.put("type", "quit");
		return msg;
	}

	public static JSONObject getRoomChangeBroadcast(String identity, String former, String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "roomchange");
		msg.put("identity", identity);
		msg.put("roomid", roomid);
		msg.put("former", former);
		return msg;
	}

	public static JSONObject getRouteUser(String identity, String host, String roomid, String port) {
		JSONObject msg = new JSONObject();
		msg.put("type", "route");
		msg.put("roomid", roomid);
		msg.put("host", host);
		msg.put("port", port);
		return msg;
	}

	public static JSONObject getWhoReply(String roomid, List<String> identities, String owner) {
		JSONObject msg = new JSONObject();
		msg.put("type", "roomcontents");
		msg.put("roomid", roomid);
		msg.put("identities", identities);
		msg.put("owner", owner);
		return msg;
	}

	public static JSONObject getWhoRequest() {
		JSONObject msg = new JSONObject();
		msg.put("type", "who");
		return msg;
	}

	public static JSONObject quitOwnerReply(String identity, String former) {
		JSONObject msg = new JSONObject();
		msg.put("type", "roomchange");
		msg.put("identity", identity);
		msg.put("roomid", "");
		msg.put("former", former);
		return msg;
	}

}
