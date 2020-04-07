package com.services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Article;
import com.model.MessagesFromServer;
import com.server.communication.MessageParser;
import com.utility.ServerExecutor;

public class ReadYourWriteConService implements Service {

	private DataOutputStream serverCoordinatorDos;
	private String serverIpPort;
	private String clientIP;

	public ReadYourWriteConService(Socket serverToCoordinatorSock, String clientIP) {
		
		this.clientIP = clientIP;
		try {
			this.serverCoordinatorDos = new DataOutputStream(serverToCoordinatorSock.getOutputStream());
			this.serverIpPort = serverToCoordinatorSock.getInetAddress().toString()
					+ serverToCoordinatorSock.getLocalPort();
			trimLocalHostFromServerIpPort();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void read() {
		System.out.println("ReadYorWrite consistency - read opeation client is "+clientIP);

		String response = MessagesFromServer.NO_ARTICLE_TO_DISPLAY;
		
		ConcurrentHashMap<Integer, Article> inputMessageToServerMap = new ConcurrentHashMap<Integer, Article>();
		inputMessageToServerMap.putAll(ServerExecutor.getArticleMap());
		ObjectMapper objectMapper = new ObjectMapper();
		try {
		if(!inputMessageToServerMap.isEmpty()) {
				
				response = objectMapper.writeValueAsString(inputMessageToServerMap);
			} 
	
		}
		catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("ReadYorWrite consistency - writing response to client "+response);
		writeToClient(response);
	}

	@Override
	public void post(String article) {
		try {
			
			String messageToCoordinator = "RPOST"+";articleid;"+";;"+article+";"+";"+";"+serverIpPort+";"+clientIP;
			writeToClient(messageToCoordinator);
			serverCoordinatorDos.writeUTF(messageToCoordinator);
			System.out.println("writing article to the coordinator "+messageToCoordinator);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void choose(String articleId) {
		int articleID = Integer.parseInt(articleId);
		String response =  "RCHOOSE;" + articleId + ";" +   ";" +   ";" + ";" + ";" + ";" + serverIpPort + ";"
				+ clientIP;
		
		String res;
		if(ServerExecutor.getArticleMap().containsKey(articleID)) {
			res = ServerExecutor.getArticleMap().get(articleID).getContents();
		}else {
			res = MessagesFromServer.ARTICLE_DOES_NOT_EXIST;
		}
		
		response = MessageParser.addArticleIdToMessage(response, articleID);
		response = MessageParser.setContents(response, res);
		
		writeToClient("COMPLETE;" +response);
	}

	@Override
	public void reply(String article, int parentID) {
		try {
			
			String message = "RREPLY;" + "articleid;" + ";"  + ";" + article + ";" + parentID+";" + ";"
					+ serverIpPort + ";" + clientIP;
			writeToClient(message);
			serverCoordinatorDos.writeUTF(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void trimLocalHostFromServerIpPort() {
		if(this.serverIpPort.contains("/")) {
			this.serverIpPort = this.serverIpPort.substring(this.serverIpPort.indexOf("/"));
		}
	}
	
	private void writeToClient(String response) {
		Socket clientSocket = ServerExecutor.serverClientSocketMap.get(clientIP);
		System.out.println("Writing response to client : "+clientIP+"Response is "+response);
		DataOutputStream dos;
		try {
			dos = new DataOutputStream(clientSocket.getOutputStream());
			dos.writeUTF(response);

		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}