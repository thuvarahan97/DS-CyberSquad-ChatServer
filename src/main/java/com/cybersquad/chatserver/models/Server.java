package com.cybersquad.chatserver.models;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
	private final String serverID;
	private final String ipAddress;
	private final int clientPort;
	private final int coordinationPort;
	private ServerSocket clientServerSocket;
	private ServerSocket serverServerSocket;
	private Socket socket;
	private ArrayList<String> roomIDs;

	public Server(String serverID, String ipAddress, int clientPort, int coordinationPort) {
		this.clientPort = clientPort;
		this.coordinationPort = coordinationPort;
		this.serverID = serverID;
		this.ipAddress = ipAddress;
		this.roomIDs = new ArrayList<>();
		this.roomIDs.add("MainHall-" + serverID);
	}

	public synchronized void addRoomID(String roomID) {
		this.roomIDs.add(roomID);
	}

	public synchronized void deleteRoomID(String roomID) {
		this.roomIDs.remove(roomID);
	}

	public synchronized int getClientPort() {
		return this.clientPort;
	}

	public ServerSocket getClientServerSocket() throws IOException {
		if (clientServerSocket == null) {
			this.clientServerSocket = new ServerSocket(this.getClientPort(), 50);
		}
		return clientServerSocket;
	}

	public synchronized int getCoordinationPort() {
		return this.coordinationPort;
	}

	public synchronized String getIPAddress() {
		return this.ipAddress;
	}

	public synchronized ArrayList<String> getRoomIDs() {
		return this.roomIDs;
	}

	public synchronized String getServerID() {
		return this.serverID;
	}

	public ServerSocket getServerServerSocket() throws IOException {
		if (serverServerSocket == null) {
			this.serverServerSocket = new ServerSocket(this.getCoordinationPort(), 50);
		}
		return serverServerSocket;
	}

	public Socket getSocket() throws IOException {
		this.socket = new Socket(this.ipAddress, this.getCoordinationPort());
		return socket;
	}
}
