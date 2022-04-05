package com.cybersquad.chatserver.services;

import static com.cybersquad.chatserver.protocols.LeaderElectionProtocol.checkMessage;

import java.io.IOException;

import com.cybersquad.chatserver.ChatServer;

public class LeaderElectionTimeoutThread implements Runnable {

	@Override
	public void run() {
		try {
			ChatServer mServer = ServerManager.getServer();
			if (!ChatServer.isLeader()) {
				if (mServer.getLeader() == null) {
					mServer.setIsLeaderAvailableMessageReceived(true);
					return;
				}
				if (!mServer.getisLeaderAvailableMessageReceived()) {
					System.out.println("Leader server " + mServer.getLeader() + " is down.");
					ServerManager.initiateLeaderElection();
				} else {
					try {
						ServerMessageThread.send(checkMessage(mServer.getServerID()), mServer.getLeader());
						mServer.setIsLeaderAvailableMessageReceived(false);
					} catch (Exception e) {
						ServerManager.initiateLeaderElection();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
