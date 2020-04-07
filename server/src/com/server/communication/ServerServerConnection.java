package com.server.communication;

/**
 * 
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Article;
import com.model.MessagesFromServer;
import com.utility.ServerExecutor;

/**
 * 
 *         Connection from server to coordinator
 *
 */
public class ServerServerConnection extends ServerExecutor implements Runnable {

	private Socket serverToCoordinatorSock;
	private static final String GET_ALL_SERVERS = "Get All Servers";

	public ServerServerConnection(Socket serverToCoordinatorSock) {
		super();
		this.serverToCoordinatorSock = serverToCoordinatorSock;
	}

	public Socket getServerCoordSocket() {
		return this.serverToCoordinatorSock;
	}

	@Override
	public void run() {

		DataInputStream dis = null;
		DataOutputStream dos = null;

		try {
			dis = new DataInputStream(this.serverToCoordinatorSock.getInputStream());
			dos = new DataOutputStream(this.serverToCoordinatorSock.getOutputStream());

		} catch (IOException e2) {
			e2.printStackTrace();
		}
		ObjectMapper objectMapper = new ObjectMapper();
		while (true) {
			try {
				// check if no article by client or no broadcast by coordinator
				System.out.println("Waiting for article from coordinator");

				while (dis.available() < 1) {
					Thread.sleep(200);
				}
				String inputMessageToServer = dis.readUTF();
				System.out.println("response received from coordinator:" + inputMessageToServer);

				if (inputMessageToServer.startsWith(GET_ALL_SERVERS)) {
					// received =
					// GET_ALL_SERVERS+";"+reqSenderServerIpPort+";"+clientIP+";"+serverList;

					String clientIp = inputMessageToServer.split(";")[2];
					System.out.println("response writing to client from get all servers" + inputMessageToServer);

					getDosSenderClient(clientIp).writeUTF(inputMessageToServer);
				} else if (inputMessageToServer.startsWith("QCHOOSE")) {
					synchronized (this) {
						System.out.println("Received QCHOOSE at the server");
						int articleId = MessageParser.getArticleID(inputMessageToServer);
						Article articleRead = ServerExecutor.getArticleMap().get(articleId);
						if (articleRead != null) {
							inputMessageToServer = MessageParser.getAppendArticle(inputMessageToServer,
									articleRead.getContents(), articleRead.getVersion());
							dos.writeUTF("COMPLETE;" + inputMessageToServer); // server to coordinator
						} else {
							String clientIp = MessageParser.getClientIpPort(inputMessageToServer);
							String op = MessageParser.setContents(inputMessageToServer, MessagesFromServer.ARTICLE_DOES_NOT_EXIST);
							getDosSenderClient(clientIp).writeUTF("COMPLETE;" +op);
							dos.writeUTF(MessagesFromServer.ARTICLE_DOES_NOT_EXIST);
						}
					}

				} else if (inputMessageToServer.startsWith("QREAD")) {
					System.out.println("Received QREAD at the server");
					ConcurrentHashMap<Integer, Article> inputMessageToServerMap = new ConcurrentHashMap<Integer, Article>();
					inputMessageToServerMap.putAll(ServerExecutor.getArticleMap());

					if (!inputMessageToServerMap.isEmpty()) {
						String allArticles = objectMapper.writeValueAsString(inputMessageToServerMap);
						System.out.println("Servers curr Article Map:" + allArticles);
						inputMessageToServer = MessageParser.getAppendArticle(inputMessageToServer, allArticles, 0);
						System.out.println("Response to coordinator:" + "COMPLETE;" + inputMessageToServer);
						dos.writeUTF("COMPLETE;" + inputMessageToServer);
					} else {
						System.out.println("Servers curr Article Map is empty");
						dos.writeUTF("COMPLETE;" + inputMessageToServer);
					}

				} else if (inputMessageToServer.startsWith("COMPLETE;QREAD;")) {

					System.out.println("inputMessageToServer:" + inputMessageToServer);
					String trimmed = MessageParser.trimComplete(inputMessageToServer);
					String articleMap = MessageParser.getContents(trimmed);
					String clientIp = MessageParser.getClientIpPort(trimmed);
					getDosSenderClient(clientIp).writeUTF(articleMap); // message to client who initiated the
																		// request
				}

				else if (inputMessageToServer.startsWith("QPOST") || inputMessageToServer.startsWith("SPOST")
						|| inputMessageToServer.startsWith("RPOST")) {

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

				else if (inputMessageToServer.startsWith("QREPLY") || inputMessageToServer.startsWith("SREPLY")
						|| inputMessageToServer.startsWith("RREPLY")) {

					System.out.println("Received QREPLY at the server");

					int articleId = MessageParser.getArticleID(inputMessageToServer);
					String contents = MessageParser.getContents(inputMessageToServer);
					int parentArticleId = MessageParser.getParentArticleId(inputMessageToServer);

					synchronized (this) {
						final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
						rwl.readLock().lock();
						if (ServerExecutor.getArticleMap().get(parentArticleId) != null) {
							rwl.readLock().unlock();
							Article articleToPost = new Article(articleId, contents);
							articleToPost.setParentId(parentArticleId);

							System.out.println("parentArticleId:" + parentArticleId);

							rwl.writeLock().lock();
							ServerExecutor.updateArticleMap(articleId, articleToPost);
							rwl.writeLock().unlock();

							rwl.readLock().lock();
							Article parentArticle = ServerExecutor.getArticleMap().get(parentArticleId);
							rwl.readLock().unlock();
							rwl.writeLock().lock();

							parentArticle.addReply(articleToPost);

							ServerExecutor.updateArticleMap(parentArticleId, parentArticle);
							rwl.writeLock().unlock();

							System.out
									.println("sending this msg to coordinator: " + "COMPLETE;" + inputMessageToServer);
							dos.writeUTF("COMPLETE;" + inputMessageToServer);// message to coordinator
						} else {
							String clientIp = MessageParser.getClientIpPort(inputMessageToServer);
							String message = MessageParser.setContents(inputMessageToServer,
									MessagesFromServer.ARTICLE_DOES_NOT_EXIST);
							getDosSenderClient(clientIp).writeUTF("COMPLETE;" +message);
							dos.writeUTF(MessagesFromServer.ARTICLE_DOES_NOT_EXIST);
						}
					}

				} else if (inputMessageToServer.startsWith("COMPLETE")) {

					System.out.println("Server received complete message from Coordinator:" + inputMessageToServer);

					inputMessageToServer = MessageParser.trimComplete(inputMessageToServer);
					int articleId = MessageParser.getArticleID(inputMessageToServer);
					String contents = MessageParser.getContents(inputMessageToServer);
					com.model.Article articleToPost = new Article(articleId, contents);

					synchronized (this) {
						ServerExecutor.updateArticleMap(articleId, articleToPost);
						// check if operation is Reply, then add to the parent article list.
						checkReplyOperation(inputMessageToServer, articleToPost);

						if (!inputMessageToServer.startsWith("RPOST") && !inputMessageToServer.startsWith("RREPLY")) {

							String clientIp = MessageParser.getClientIpPort(inputMessageToServer);
							getDosSenderClient(clientIp).writeUTF("COMPLETE;" + inputMessageToServer); // message to
																										// client
																										// who initiated
																										// the
							// request
							System.out.println("Wrote response to client socket " + inputMessageToServer);
						}
					}

				} else if (inputMessageToServer.startsWith(MessagesFromServer.INVALID_QUORUM)) {
					System.out.println("inputMessageToServer:" + inputMessageToServer);
					String trimmed = MessageParser.trimInvalid(inputMessageToServer);
					String clientIp = MessageParser.getClientIpPort(trimmed);
					trimmed = MessageParser.setContents(trimmed, MessagesFromServer.INVALID_QUORUM);

					getDosSenderClient(clientIp).writeUTF("COMPLETE;" + trimmed); // message to client who initiated the
																					// request
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

	private void checkReplyOperation(String inputMessageToServer, Article articleToPost) {

		if (inputMessageToServer.startsWith("SREPLY") || inputMessageToServer.startsWith("QREPLY")
				|| inputMessageToServer.startsWith("RREPLY")) {
			int parentArticleId = MessageParser.getParentArticleId(inputMessageToServer);
			final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
			rwl.readLock().lock();
			Article parentArticle = ServerExecutor.getArticleMap().get(parentArticleId);
			rwl.readLock().unlock();
			parentArticle.addReply(articleToPost);
			System.out
					.println("Article " + articleToPost + "added to the replies of parent Article " + parentArticleId);
			rwl.writeLock().lock();
			ServerExecutor.updateArticleMap(parentArticleId, parentArticle);
			rwl.writeLock().unlock();
		}

	}

	private DataOutputStream getDosSenderClient(String clientIp) throws IOException {
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.readLock().lock();
		Socket clientSocket = ServerExecutor.getServerClientSocketMap().get(clientIp);
		
		System.out.println("printing client map: "+ clientIp);
		for(Entry<String, Socket> entry: ServerExecutor.getServerClientSocketMap().entrySet()) {
			System.out.println(entry.getKey()+ ":"+ entry.getValue());
		}
		rwl.readLock().unlock();
		System.out.println("clientIp:" + clientIp);
		return new DataOutputStream(clientSocket.getOutputStream());
	}

}