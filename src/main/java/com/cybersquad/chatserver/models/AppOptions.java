package com.cybersquad.chatserver.models;

import org.kohsuke.args4j.Option;

public class AppOptions {
	@Option(required = true, name = "-s", aliases = "--serverID", usage = "Server ID")
	private String serverID;

	@Option(required = true, name = "-p", aliases = "--configFilePath", usage = "Server Configuration File")
	private String configFilePath;

	@Option(required = false, name = "-d", aliases = "--debugLeaderCheck", usage = "Debug Leader Availability Check")
	private boolean debugLeaderCheck = false;

	public String getServerID() {
		return serverID;
	}

	public String getConfigFilePath() {
		return configFilePath;
	}
	
	public boolean isDebugLeaderCheck() {
		return debugLeaderCheck;
	}
}
