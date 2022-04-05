package com.cybersquad.chatserver.protocols;

import org.json.simple.JSONObject;

public class LeaderElectionProtocol {

	public static int timeout_leaderelection = 10; // in Seconds
	public static int timeout_leadercheck = 5; // in Seconds
	public static int timeout_informcoordinator = 5; // in Seconds

	// Leader confirms availability
	public static JSONObject availableMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "AVAILABLE");
		msg.put("serverid", serverid);
		return msg;
	}

	// Check whether leader availability
	public static JSONObject checkMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "CHECK");
		msg.put("serverid", serverid);
		return msg;
	}

	// COORDINATOR message
	public static JSONObject coordinatorMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "COORDINATOR");
		msg.put("serverid", serverid);
		return msg;
	}

	// ELECTION message
	public static JSONObject electionMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "ELECTION");
		msg.put("serverid", serverid);
		return msg;
	}

	// INFORM Coordinator message
	public static JSONObject informCoordinatorMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "INFORM");
		msg.put("serverid", serverid);
		return msg;
	}

	public static JSONObject newIdentityApprovalReply(boolean approved, String identity) {
		JSONObject msg = new JSONObject();
		msg.put("type", "confirmIdentity");
		msg.put("approved", approved);
		msg.put("identity", identity);
		return msg;
	}

	public static JSONObject newtIdentityApprovalRequest(String serverid, String identity) {
		JSONObject msg = new JSONObject();
		msg.put("type", "requestIdentityApproval");
		msg.put("serverid", serverid);
		msg.put("identity", identity);
		return msg;
	}

	public static JSONObject newtRoomIdApprovalReply(boolean approved, String roomid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "confirmRoomID");
		msg.put("approved", approved);
		msg.put("roomid", roomid);
		return msg;
	}

	public static JSONObject newtRoomIdApprovalRequest(String roomid, String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "requestRoomIDApproval");
		msg.put("roomid", roomid);
		msg.put("serverid", serverid);
		return msg;
	}

	// OK message
	public static JSONObject okMessage(String serverid) {
		JSONObject msg = new JSONObject();
		msg.put("type", "bully");
		msg.put("kind", "OK");
		msg.put("serverid", serverid);
		return msg;
	}

}
