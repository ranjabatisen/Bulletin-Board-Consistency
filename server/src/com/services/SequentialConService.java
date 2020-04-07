package com.services;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Article;
import com.model.MessagesFromServer;
import com.server.communication.MessageParser;
import com.utility.ServerExecutor;

// can extend thread to create separate thread for each client
public class SequentialConService  implements Service {
	
    public static ConcurrentHashMap<String, String>articlesAckMap;
    private String clientIP;
	private Socket serverToCoordinatorSock;
	private DataOutputStream serverCoordinatorDos;
	private String serverIpPort;
	private ObjectMapper objectMapper = new ObjectMapper();

    
    public SequentialConService(Socket sock, String clientIP) {
    	

    	this.clientIP = clientIP;
    	articlesAckMap = new ConcurrentHashMap<String, String>();
    	this.serverToCoordinatorSock = sock;
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
		// return top 5 articles
			System.out.println("Sequential consistency - read opeation");

		String response = MessagesFromServer.NO_ARTICLE_TO_DISPLAY;
		
		ConcurrentHashMap<Integer, Article> inputMessageToServerMap = new ConcurrentHashMap<Integer, Article>();
		inputMessageToServerMap.putAll(ServerExecutor.getArticleMap());
		try {
		if(!inputMessageToServerMap.isEmpty()) {
				response = objectMapper.writeValueAsString(inputMessageToServerMap);
			} 
	
		}
		catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("Sequential consistency - writing response to client "+response);
		writeToClient(response);
	}
	
	@Override
	public void post(String article) {
		// SPOST;articleId;nr;nw;articlecontents;articleIdreply;version;serverIpPort;clientIpPort

		try {	
			String messageToCoordinator = "SPOST"+";articleid;"+";;"+article+";"+";"+";"+serverIpPort+";"+clientIP;
			
			serverCoordinatorDos.writeUTF(messageToCoordinator);
			System.out.println("writing article to the coordinator "+messageToCoordinator);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void choose(String articleId) {
		int articleID = Integer.parseInt(articleId);
		String response =  "SCHOOSE;" + articleId + ";" +   ";" +   ";" + ";" + ";" + ";" + serverIpPort + ";"
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

	private void writeToClient(String response) {
		Socket clientSocket = ServerExecutor.serverClientSocketMap.get(clientIP);
		DataOutputStream dos;
		try {
			dos = new DataOutputStream(clientSocket.getOutputStream());
			dos.writeUTF(response);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// QREPLY;articleId;nR;nW:articlecontents;articleIDtoreplyTo;version;serverIpPort;clientIpPort

	@Override
	public void reply(String article, int parentID) {
		try {
			
			String message = "SREPLY;" + "articleid;" + ";"  + ";" + article + ";" + parentID+";" + ";"
					+ serverIpPort + ";" + clientIP;
			serverCoordinatorDos.writeUTF(message);
			//Thread.sleep(100); // for coordinator to broadcast the message to server and server to respond back
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void trimLocalHostFromServerIpPort() {
		if(this.serverIpPort.contains("/")) {
			this.serverIpPort = this.serverIpPort.substring(this.serverIpPort.indexOf("/"));
		}
	}
}
