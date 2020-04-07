/**
 * 
 */
package com.server.communication;

/**
 * 
 */


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import com.model.Article;
import com.model.MessagesFromServer;
import com.utility.ServerExecutor;



/*
 *         Connection from server to coordinator
 *
 */
public class ServerAtCoordinator extends ServerExecutor implements Runnable{

	private Socket serverToCoordinatorSock;
	private static final String GET_ALL_SERVERS = "Get All Servers";


	public ServerAtCoordinator(Socket serverToCoordinatorSock) {
		super();
		this.serverToCoordinatorSock = serverToCoordinatorSock;
	}

	public Socket getServerCoordSocket() {
		return this.serverToCoordinatorSock;
	}

	public void run() {

		DataInputStream dis = null;
		DataOutputStream dos = null;

		try {
			dis = new DataInputStream(this.serverToCoordinatorSock.getInputStream());
			dos = new DataOutputStream(this.serverToCoordinatorSock.getOutputStream());

		} catch (IOException e2) {
			e2.printStackTrace();
		}

		while (true) {
			try {
				// check if no article by client or no broadcast by coordinator
				System.out.println("Waiting for article from coordinator");

				while (dis.available() < 1) {
					Thread.sleep(200);
				}
				String inputMessageToServer = dis.readUTF();
				System.out.println("response received from coordinator:" + inputMessageToServer);

				
				if(inputMessageToServer.startsWith(GET_ALL_SERVERS)) {
					//	received = GET_ALL_SERVERS+";"+reqSenderServerIpPort+";"+clientIP+";"+serverList;

					String clientIp = inputMessageToServer.split(";")[2];
					System.out.println("response writing to client from get all servers" + inputMessageToServer);

					getDosSenderClient(clientIp).writeUTF(inputMessageToServer); 
				}
				
				else if (inputMessageToServer.startsWith("QCHOOSE")) {

					System.out.println("Received QCHOOSE at the server");
					int articleId = MessageParser.getArticleID(inputMessageToServer);
					Article articleRead = ServerExecutor.getArticleMap().get(articleId);
					if (articleRead != null) {
						inputMessageToServer = MessageParser.getAppendArticle(inputMessageToServer,
								articleRead.getContents(), articleRead.getVersion());
						dos.writeUTF("COMPLETE;" + inputMessageToServer); // server to coordinator
					} else {
						dos.writeUTF("COMPLETE;" + inputMessageToServer + MessagesFromServer.ARTICLE_DOES_NOT_EXIST);
					}

				} else if (inputMessageToServer.startsWith("QPOST")|| inputMessageToServer.startsWith("SPOST") || inputMessageToServer.startsWith("RPOST")) {

					System.out.println("Received POST at the server");
					int articleId = MessageParser.getArticleID(inputMessageToServer);
					String contents = MessageParser.getContents(inputMessageToServer);
					Article articleToPost = new Article(articleId, contents);
					synchronized (this) {
						ServerExecutor.updateArticleMap(articleId, articleToPost);
					}
					System.out.println("sending this msg to coordinator: " + "COMPLETE;" + inputMessageToServer);
					dos.writeUTF("COMPLETE;" + inputMessageToServer);// message to coordinator
				}

				else if (inputMessageToServer.startsWith("QREPLY") || inputMessageToServer.startsWith("SREPLY") || inputMessageToServer.startsWith("RREPLY")) {
					System.out.println("Received QREPLY at the server");
					int articleId = MessageParser.getArticleID(inputMessageToServer);
					String contents = MessageParser.getContents(inputMessageToServer);
					int parentArticleId = MessageParser.getParentArticleId(inputMessageToServer);

					synchronized (this) {
						Article articleToPost = new Article(articleId, contents);
						articleToPost.setParentId(parentArticleId);
						ServerExecutor.updateArticleMap(articleId, articleToPost);
						Article parentArticle = ServerExecutor.getArticleMap().get(parentArticleId);
						parentArticle.addReply(articleToPost);
						ServerExecutor.updateArticleMap(parentArticleId, parentArticle);
					}

					System.out.println("sending this msg to coordinator: " + "COMPLETE;" + inputMessageToServer);
					dos.writeUTF("COMPLETE;" + inputMessageToServer);// message to coordinator

				} else if (inputMessageToServer.startsWith("COMPLETE")
						|| inputMessageToServer.startsWith(MessagesFromServer.INVALID_QUORUM)) {

					System.out.println(
							"Server received complete/invalid message from Coordinator:" + inputMessageToServer);
					
					inputMessageToServer = MessageParser.trimComplete(inputMessageToServer);
					int articleId = MessageParser.getArticleID(inputMessageToServer);
					String contents = MessageParser.getContents(inputMessageToServer);
					com.model.Article articleToPost = new Article(articleId, contents);
					ServerExecutor.updateArticleMap(articleId, articleToPost);
					// check if operation is Reply, then add to the parent article list.
					checkReplyOperation(inputMessageToServer, articleToPost);
					
					if(!inputMessageToServer.startsWith("RPOST") && !inputMessageToServer.startsWith("RREPLY")) {
					//	DataOutputStream dosServerClient = new DataOutputStream(clientSocket.getOutputStream());
						//dosServerClient.writeUTF(inputMessageToServer);
						String clientIp = MessageParser.getClientIpPort(inputMessageToServer);
						getDosSenderClient(clientIp).writeUTF("COMPLETE;"+inputMessageToServer); // message to client who initiated the
																						// request
						
						System.out.println("Wrote response to client socket "+inputMessageToServer);
					}
					
				} 

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void checkReplyOperation(String inputMessageToServer, Article articleToPost) {
		
		if(inputMessageToServer.startsWith("SREPLY") ||  inputMessageToServer.startsWith("QREPLY") || inputMessageToServer.startsWith("RREPLY")) {
			int parentArticleId = MessageParser.getParentArticleId(inputMessageToServer);
			Article parentArticle = ServerExecutor.getArticleMap().get(parentArticleId);
			parentArticle.addReply(articleToPost);
			System.out.println("Article "+articleToPost+"added to the replies of parent Article "+parentArticleId);
			ServerExecutor.updateArticleMap(parentArticleId, parentArticle);
		}
		
	}

	private DataOutputStream getDosSenderClient(String clientIp) throws IOException {
		Socket clientSocket = ServerExecutor.serverClientSocketMap.get(clientIp);
		System.out.println("clientIp:" + clientIp);
		return new DataOutputStream(clientSocket.getOutputStream());
	}



}