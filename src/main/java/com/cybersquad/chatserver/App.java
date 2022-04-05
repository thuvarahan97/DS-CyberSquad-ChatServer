package com.cybersquad.chatserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.cybersquad.chatserver.models.AppOptions;
import com.cybersquad.chatserver.models.Server;
import com.cybersquad.chatserver.protocols.LeaderElectionProtocol;
import com.cybersquad.chatserver.services.ClientManager;
import com.cybersquad.chatserver.services.ClientMessageThread;
import com.cybersquad.chatserver.services.LeaderElectionTimeoutThread;
import com.cybersquad.chatserver.services.RoomManager;
import com.cybersquad.chatserver.services.ServerManager;
import com.cybersquad.chatserver.services.ServerMessageThread;

public class App {

	public static void main(String[] args) {
		// All servers
		ArrayList<Server> servers = new ArrayList<>();
		
		ChatServer mServer = null;
		
		Socket clientSocket;
		ServerSocket clientServerSocket;
		ServerSocket serverServerSocket;

		AppOptions options = new AppOptions();
		CmdLineParser parser = new CmdLineParser(options);

		try {
			parser.parseArgument(args);

			String serverid = options.getServerID();
			String path = options.getConfigFilePath();

			System.out.println("Server ID: " + serverid);

			// Fetch server details from configuration file
			File file = new File(path);
			Scanner scanner = new Scanner(file);
			System.out.println("serverid	server_address	clients_port	coordination_port");
			while (scanner.hasNextLine()) {
				String data = scanner.nextLine();
				String[] split = data.split("\\s+");
				System.out.println(
						fixedLengthString(split[0], "serverid".length()) + 
						"	" + fixedLengthString(split[1], "server_address".length()) + 
						"	" + fixedLengthString(split[2], "clients_port".length()) + 
						"	" + fixedLengthString(split[3], "coordination_port".length())
						);
				if (split[0].equalsIgnoreCase(serverid)) {
					mServer = new ChatServer(split[0], split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3]));
					ChatServer.isDebugLeaderCheck = options.isDebugLeaderCheck();
				} else {
					servers.add(new Server(split[0], split[1], Integer.parseInt(split[2]), Integer.parseInt(split[3])));
				}
			}
			scanner.close();

			if (mServer == null) {
				System.err.println("Error has occured while parsing configuration file. Server not found.");
			} else {
				for (Server server : servers) {
					mServer.addServer(server);
					String roomid = "MainHall-" + server.getServerID();
					mServer.addRoom(server.getServerID(), roomid);
				}
			}
			System.out.println("Total servers: " + (servers.size() + 1));

		} catch (CmdLineException e) {
			System.err.println("Error has occured while parsing command line arguments.\n" + e.getLocalizedMessage());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		try {
			assert mServer != null;
			clientServerSocket = mServer.getClientServerSocket();
			clientServerSocket.setReuseAddress(true);
			
			serverServerSocket = mServer.getServerServerSocket();
			serverServerSocket.setReuseAddress(true);

			// Server Messages
			ServerManager serverManager = ServerManager.getInstance(mServer);
			Thread serverMessageThread = new Thread(new ServerMessageThread(serverServerSocket, serverManager));
			serverMessageThread.start();

			// Check the availability of Leader Server for every 5 seconds
			Runnable leaderElectionTimeoutThread = new LeaderElectionTimeoutThread();
			ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
			executor.scheduleAtFixedRate(leaderElectionTimeoutThread, 0, LeaderElectionProtocol.timeout_leadercheck, TimeUnit.SECONDS);
			
			RoomManager roomManager = new RoomManager(mServer);
			ClientManager clientManager = new ClientManager(roomManager);

			while (true) {
				clientSocket = clientServerSocket.accept();
				System.out.println("Connection has been received from " + clientSocket.getInetAddress().getHostName() + " to port " + clientSocket.getPort());
				Thread receiveThread = new Thread(new ClientMessageThread(clientSocket, clientManager, mServer));
				receiveThread.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static String fixedLengthString(String string, int length) {
	    return String.format("%-"+length+ "s", string);
	}
}
