package com.services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class QuorumConsistService implements Service {

	private Socket serverToCoordinatorSock;
	private int nR = 1;
	private int nW = 2;
	private String clientIpPort;
	private String serverIpPort;
	private DataOutputStream dosServerToCoord;

	public QuorumConsistService(Socket serverToCoordinatorSock, String clientIpPort, int nr, int nw) {
		this.serverToCoordinatorSock = serverToCoordinatorSock;
		this.clientIpPort = clientIpPort;
		this.serverIpPort = serverToCoordinatorSock.getInetAddress().toString()
				+ serverToCoordinatorSock.getLocalPort();
		trimLocalHostFromServerIpPort();
		this.nR = nr;
		this.nW = nw;
		try {
			this.dosServerToCoord = new DataOutputStream(this.serverToCoordinatorSock.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void trimLocalHostFromServerIpPort() {
		if(this.serverIpPort.contains("/")) {
			this.serverIpPort = this.serverIpPort.substring(this.serverIpPort.indexOf("/"));
		}
	}

	// CHOOSE;articleId;nR;nW;nR:nW:articlecontents;;serverport;clientport
	@Override
	public void choose(String articleId) {
		try {
			String message = "QCHOOSE;" + articleId + ";" + nR + ";" + nW + ";" + ";" + ";" + ";" + serverIpPort + ";"
					+ clientIpPort;
			dosServerToCoord.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
	}

	@Override
	public void read() {
		try {
			String message = "QREAD;" + ";" + nR + ";" + nW + ";" + ";" + ";" + ";" + serverIpPort + ";"
					+ clientIpPort;
			dosServerToCoord.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;

	}
	// POST;articleId;nR;nW:articlecontents;articleIdreply;version;serverIpPort;clientIpPort
	@Override
	public void post(String articleContents) {
		try {
			
			String message = "QPOST;" + "articleid;" + nR + ";" + nW + ";" + articleContents + ";" + ";" + ";"
					+ serverIpPort + ";" + clientIpPort;
			dosServerToCoord.writeUTF(message);
			Thread.sleep(100); // for coordinator to broadcast the message to server and server to respond back
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return;
	}

	// QREPLY;articleId;nR;nW:articlecontents;articleIDtoreplyTo;version;serverIpPort;clientIpPort
	@Override
	public void reply(String articleContents, int parentId) {
		try {
			String message = "QREPLY;" + "articleid;" + nR + ";" + nW + ";" + articleContents + ";" + parentId+";" + ";"
					+ serverIpPort + ";" + clientIpPort;
			dosServerToCoord.writeUTF(message);
			Thread.sleep(100); // for coordinator to broadcast the message to server and server to respond back
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return;
	}

}