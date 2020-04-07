/**
 * 
 */
package com.server.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.model.Article;
import com.model.MessagesFromServer;

/*
 *
 *         coordinator server receives input and broadcasts to other servers
 */
public class ServerConnectionsHandler extends CoordinatorServer implements Runnable {

	private DataOutputStream dos; // dos from coordinator to server
	private DataInputStream dis; // dis from server to coordinator
	private String currentServerId;
	private static final String GET_ALL_SERVERS = "Get All Servers";

	public ServerConnectionsHandler(DataInputStream dis, DataOutputStream dos, String addrPort) {
		super();
		this.dis = dis;
		this.dos = dos;
		this.currentServerId = addrPort; // unique id about socket connection when running servers in same machine

	}

	public void run() {
		String received;
		ObjectMapper objectMapper = new ObjectMapper();
		while (true) {
			try {
				while (dis.available() < 1) { // message to coordinator from server
					Thread.sleep(500);
				}
				if (dis.available() > 1) {
					received = dis.readUTF();
					System.out.println(
							"coordinator recieved article from the server " + currentServerId + ": " + received);
					BroadcastHandler bHandler = new BroadcastHandler();
					/**
					 * POST;articleId;nR;nW:articlecontents;;version;serverIpPort;clientIpPort
					 * QREPLY;articleId;nR;nW:articlecontents;articleIDtoreplyTo;version;
					 * serverIpPort;clientIpPort
					 */
					if (received.startsWith(GET_ALL_SERVERS)) {
						Set<String> serverIPSet = CoordinatorServer.socketsMap.keySet();

						String serverList = MessageParser.serializeServersSet(serverIPSet);
						String reqSenderServerIpPort = MessageParser.getServerIpPort(received);
						String clientIP = MessageParser.getClientIpPort(received);
						System.out.println("Get all servers COMPLETE by Coordinator. Sending response to Server:"
								+ reqSenderServerIpPort + " message: " + serverList);
						Socket reqSenderServerSocket = CoordinatorServer.socketsMap.get(reqSenderServerIpPort);
						received = GET_ALL_SERVERS + ";" + reqSenderServerIpPort + ";" + clientIP + ";" + serverList;
						System.out.println("Sending to server message is " + received);
						new DataOutputStream(reqSenderServerSocket.getOutputStream()).writeUTF(received);

					} else if (received.startsWith("QCHOOSE")) {
						if (isQuorumValid(received)) {
							int nR = MessageParser.getReadQuorum(received);
							ConcurrentHashMap<String, Socket> readServersMap = getBroadCastListeners(received, nR,
									"CHOOSE");
							// coordinator broadcasting to servers
							bHandler.broadcastPost(received, readServersMap, currentServerId, false);
						} else {
							System.out.println("Given Read Quorum is invalid");
							getDosSenderServer(received).writeUTF(MessagesFromServer.INVALID_QUORUM + ";" + received);
						}

					} else if (received.startsWith("QREAD")) {
						if (isQuorumValid(received)) {
							int nR = MessageParser.getReadQuorum(received);
							ConcurrentHashMap<String, Socket> readServersMap = getBroadCastListeners(received, nR,
									"READ");

							bHandler.broadcastPost(received, readServersMap, currentServerId, false);
						} else {
							System.out.println("Given Read Quorum is invalid");
							getDosSenderServer(received).writeUTF(MessagesFromServer.INVALID_QUORUM + ";" + received);
						}

					} else if (received.startsWith("QPOST") || received.startsWith("QREPLY")) {

						int nW = MessageParser.getWriteQuorum(received);
						if (isQuorumValid(received)) {
							articleID = articleID + 1; /** increment article id at coordinator for new articles **/
							received = MessageParser.addArticleIdToMessage(received, articleID);
							ConcurrentHashMap<String, Socket> readServersMap;
							if (received.startsWith("QPOST")) {
								readServersMap = getBroadCastListeners(received, nW, "POST");
							} else {
								readServersMap = getBroadCastListeners(received, nW, "REPLY");
							}

							// coordinator broadcasting request to servers
							bHandler.broadcastPost(received, readServersMap, currentServerId, false);
						} else {
							System.out.println("Given Write Quorum is invalid");
							getDosSenderServer(received).writeUTF(MessagesFromServer.INVALID_QUORUM + ";" + received);
						}

					} else if (received.startsWith("COMPLETE;QPOST") || received.startsWith("COMPLETE;QREPLY")) {
						String ackMapkey = MessageParser.trimComplete(received);
						
						synchronized (this) {
							updateAckMap(ackMapkey);
							int nAcksReqd = MessageParser.getWriteQuorum(ackMapkey);
							if (isRequestSuccesful(ackMapkey, nAcksReqd)) {
								/**
								 * once the request is successful flush the ack entry from map to enable
								 * processing request on same article id by same client
								 **/
								flushKeyFromAckMap(received);
								String trimmedMessage = MessageParser.trimComplete(received); // removing COMPLETE
								String reqSenderServerIpPort = MessageParser.getServerIpPort(trimmedMessage);
								System.out.println("POST COMPLETE by Coordinator. Sending response to Server:"
										+ reqSenderServerIpPort + " message: " + received);
								Socket reqSenderServerSocket;
								synchronized (this) {
									reqSenderServerSocket = CoordinatorServer.socketsMap.get(reqSenderServerIpPort);
								}
								new DataOutputStream(reqSenderServerSocket.getOutputStream()).writeUTF(received);
							}

						}

					} else if (received.startsWith("SPOST") || received.startsWith("SREPLY")
							|| received.startsWith("RPOST") || received.startsWith("RREPLY")) {
						articleID = articleID + 1;
						received = MessageParser.addArticleIdToMessage(received, articleID);
						System.out.println("Coordinator: received is " + received);

						if (CoordinatorServer.socketsMap.size() == 1) {
							String reqSenderServerIpPort = MessageParser.getServerIpPort(received);
							System.out.println("POST COMPLETE by Coordinator. Sending response to Server:"
									+ reqSenderServerIpPort + " message: " + received);
							Socket reqSenderServerSocket = CoordinatorServer.socketsMap.get(reqSenderServerIpPort);
							received = "COMPLETE;" + received;
							new DataOutputStream(reqSenderServerSocket.getOutputStream()).writeUTF(received);
						} else {
							bHandler.broadcastPost(received, CoordinatorServer.socketsMap, currentServerId, true);
						}
					} else if (received.startsWith("COMPLETE;QCHOOSE")) {
						synchronized (this) {
							String trimmedMessage = MessageParser.trimComplete(received); // remove COMPLETE
							String articleResponseMapkey = MessageParser.getArticleResponseKey(trimmedMessage);

							updateAckMap(articleResponseMapkey); // update no of ACks received by coordinator
							updateArticleResponsesMap(trimmedMessage); // capture article responses received by
																		// coordinator

							int nAcksReqd = MessageParser.getReadQuorum(trimmedMessage);

							if (isRequestSuccesful(articleResponseMapkey, nAcksReqd)) {

								received = MessageParser.getCompleteMessage(getLatestArticle(trimmedMessage));

								flushKeyFromAckMap(articleResponseMapkey);
								flushKeyFromArticleResponses(articleResponseMapkey);

								getDosSenderServer(trimmedMessage).writeUTF(received);
							}
						}

					} else if (received.startsWith("COMPLETE;QREAD")) {
						synchronized (this) {
							System.out.println("Entering Complete QRead");
							String trimmedMessage = MessageParser.trimComplete(received); // remove COMPLETE

							System.out.println("trimmedMessage:" + trimmedMessage);
							// key - removes contents which is a json string of whole map and the dummy
							// version of the article
							String readResponseMapkey = MessageParser.getArticleResponseKey(trimmedMessage);
							System.out.println("readResponseMapkey:" + readResponseMapkey);
							updateAckMap(readResponseMapkey); // update no of ACks received by coordinator
							updateReadResponsesMap(trimmedMessage); // capture read responses received by coordinator

							int nAcksReqd = MessageParser.getReadQuorum(trimmedMessage);
							
							if (isRequestSuccesful(readResponseMapkey, nAcksReqd)) {
								String articleMapStr = objectMapper
										.writeValueAsString(readResponsesMap.get(readResponseMapkey));
								// update the output message with updated articlemap json
								trimmedMessage = MessageParser.setContents(trimmedMessage, articleMapStr);
								received = MessageParser.getCompleteMessage(trimmedMessage);

								flushKeyFromAckMap(readResponseMapkey);
								flushKeyFromReadResponses(readResponseMapkey);

								System.out.println("writing response to server:" + received);

								getDosSenderServer(trimmedMessage).writeUTF(received);
							}
						}
					}

					else if (received.startsWith("COMPLETE;SPOST") || received.startsWith("COMPLETE;SREPLY")
							|| received.startsWith("COMPLETE;RPOST") || received.startsWith("COMPLETE;RREPLY")) {

						updateAckMap(received);
						int nAcksReqdSpost = CoordinatorServer.socketsMap.size() - 1;
						if (isRequestSuccesful(received, nAcksReqdSpost)) {
							System.out.println("POST COMPLETE by Coordinator. Sending response to Server");
							flushKeyFromAckMap(received);
							String trimmedMessage = MessageParser.trimComplete(received); // remove COMPLETE
							String reqSenderServerIpPort = MessageParser.getServerIpPort(trimmedMessage);
							System.out.println("POST COMPLETE by Coordinator. Sending response to Server:"
									+ reqSenderServerIpPort + " message: " + received);
							Socket reqSenderServerSocket = CoordinatorServer.socketsMap.get(reqSenderServerIpPort);
							new DataOutputStream(reqSenderServerSocket.getOutputStream()).writeUTF(received);

						} else {
							System.out.println("POST waiting for ack " + nAcksReqdSpost);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {

				e.printStackTrace();
			}
		}
	}

	private synchronized void updateReadResponsesMap(String trimmedMessage) {
		String requestKey = MessageParser.getArticleResponseKey(trimmedMessage); // key corresponds to article id of the
																					// request
		String json = MessageParser.getContents(trimmedMessage);
		ObjectMapper objectMapper = new ObjectMapper();
		// read servers article map from json string
		ConcurrentHashMap<Integer, Article> articleMap;
		try {
			articleMap = objectMapper.readValue(json, TypeFactory.defaultInstance()
					.constructMapType(ConcurrentHashMap.class, Integer.class, Article.class));

			final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
			rwl.readLock().lock();
			if (readResponsesMap.get(requestKey) == null) {
				rwl.readLock().unlock();

				rwl.writeLock().lock();
				// update the response map with the article map
				readResponsesMap.put(requestKey, articleMap);
				rwl.writeLock().unlock();

			} else if (readResponsesMap.get(requestKey) != null) {
				rwl.readLock().unlock();
				for (Entry<Integer, Article> entry : articleMap.entrySet()) {
					int articleId = entry.getKey();
					Article article = entry.getValue();
					// add the article if the public map does not have the article
					rwl.readLock().lock();
					ConcurrentHashMap<Integer, Article> currPublicMap = readResponsesMap.get(requestKey);
					
					if (currPublicMap.get(articleId) != null) {
						rwl.readLock().unlock();
						
						rwl.writeLock().lock();
						currPublicMap.put(articleId, article);

						readResponsesMap.put(requestKey, currPublicMap);
						rwl.writeLock().unlock();

					} else {
						// update the article in public map if the curr article version is higher

						int pubArtVersion = 0;
						if(currPublicMap.get(articleId)!=null) {
							pubArtVersion = currPublicMap.get(articleId).getVersion();
						}
						rwl.readLock().unlock();
						int currVersion = article.getVersion();
						if (currVersion > pubArtVersion) {
							rwl.writeLock().lock();
							
							currPublicMap.put(articleId, article);
							readResponsesMap.put(requestKey, currPublicMap);
							rwl.writeLock().unlock();
						} 
					}
				}

			} else {
				rwl.readLock().unlock();
				// this version already exists in the article responses map. Do nothing
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private synchronized DataOutputStream getDosSenderServer(String trimmedMessage) throws IOException {
		String reqSenderServerIpPort = MessageParser.getServerIpPort(trimmedMessage);
		Socket reqSenderServerSocket;
		synchronized (this) {
			reqSenderServerSocket = CoordinatorServer.socketsMap.get(reqSenderServerIpPort);
		}
		return new DataOutputStream(reqSenderServerSocket.getOutputStream());

	}

	private synchronized void updateArticleResponsesMap(String received) {
		String key = MessageParser.getArticleResponseKey(received); // key corresponds to article id of the request
		int version = MessageParser.getArticleVersion(received);
		String contents = MessageParser.getContents(received);

		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.readLock().lock();
		if (articlesResponsesMap.get(key) == null) {
			rwl.readLock().unlock();
			TreeMap<Integer, String> newMap = new TreeMap<Integer, String>();
			newMap.put(version, contents);
			rwl.writeLock().lock();
			articlesResponsesMap.put(key, newMap);
			rwl.writeLock().unlock();
		} else if (articlesResponsesMap.get(key).get(version) == null) {
			TreeMap<Integer, String> versionMap = articlesResponsesMap.get(key);
			rwl.readLock().unlock();
			versionMap.put(version, contents);
			rwl.writeLock().lock();
			articlesResponsesMap.put(key, versionMap);
			rwl.writeLock().unlock();
		} else {
			rwl.readLock().unlock();
			// this version already exists in the article responses map. Do nothing
		}
	}

	// gets latest article from the tree map of responses received from servers
	private synchronized String getLatestArticle(String received) {
		String key = MessageParser.getArticleResponseKey(received);
		int latestVersion = articlesResponsesMap.get(key).lastEntry().getKey();
		String latestContents = articlesResponsesMap.get(key).lastEntry().getValue();
		return MessageParser.getAppendArticle(received, latestContents, latestVersion);
	}

	private ConcurrentHashMap<String, Socket> getBroadCastListeners(String received, int nBroadcast, String reqType) {
		ConcurrentHashMap<String, Socket> readServersMap = new ConcurrentHashMap<String, Socket>();
		ArrayList<String> serverIds;
		synchronized (this) {
			serverIds = new ArrayList<String>(CoordinatorServer.socketsMap.keySet());
		}
		System.out.println("nBroadcast=" + nBroadcast);
		boolean[] visited = new boolean[nBroadcast];

		for (int i = 0; i < nBroadcast; i++) {

			int randomInt = ThreadLocalRandom.current().nextInt(0, nBroadcast);
			while (visited[randomInt]) {
				randomInt = ThreadLocalRandom.current().nextInt(0, nBroadcast);
			}
			visited[randomInt] = true;
			System.out.println("Random number generated is : " + randomInt);
			System.out.println("CoordinatorServer.socketsMap.size():" + socketsMap.size());

//			if (randomInt == CoordinatorServer.socketsMap.size()) {
//				// POST: coordinator is part of write quorum list post to coordinator
//				if (reqType.equals("POST")) {
//					postToCoordinatorSelf(received, articleID);
//				} else if (reqType.equals("REPLY")) {
//					int parentArticleId = MessageParser.getParentArticleId(received);
//					replyToCoordinatorSelf(received, articleID, parentArticleId);
//				}
//				// CHOOSE: coordinator is part of read quorum list Choose from coordinator
//				else if (reqType.equals("CHOOSE")) {
//					chooseFromCoordinator(received, articleID);
//				} else if (reqType.equals("READ")) {
//					readFromCoordinator(received);
//				}
//			} else {
			readServersMap.put(serverIds.get(randomInt), CoordinatorServer.socketsMap.get(serverIds.get(randomInt)));
//			}
		}
		return readServersMap;
	}

	private synchronized void updateAckMap(String ackMapkey) {
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.readLock().lock();
		if (ackMap.get(ackMapkey) != null) {
			rwl.readLock().unlock();
			rwl.writeLock().lock();
			ackMap.put(ackMapkey, ackMap.get(ackMapkey) + 1);
			rwl.writeLock().unlock();
		} else {
			rwl.readLock().unlock();
			rwl.writeLock().lock();
			ackMap.put(ackMapkey, 1);
			rwl.writeLock().unlock();
		}
		System.out.println("updated ackmap," + ackMapkey + ":" + ackMap.get(ackMapkey));
	}

	private synchronized void flushKeyFromAckMap(String ackMapkey) {
		if (ackMap.get(ackMapkey) != null) {
			ackMap.remove(ackMapkey);
		}
		System.out.println("flushed key from ackmap," + ackMapkey);
	}

	private synchronized void flushKeyFromArticleResponses(String ackMapkey) {
		if (articlesResponsesMap.get(ackMapkey) != null) {
			articlesResponsesMap.remove(ackMapkey);
		}
		System.out.println("flushed key from article response map," + ackMapkey);
	}

	private synchronized void flushKeyFromReadResponses(String ackMapkey) {
		if (articlesResponsesMap.get(ackMapkey) != null) {
			articlesResponsesMap.remove(ackMapkey);
		}
		System.out.println("flushed key from read response map," + ackMapkey);
	}

	private boolean isQuorumValid(String received) {

		int nW = MessageParser.getWriteQuorum(received);
		int nR = MessageParser.getReadQuorum(received);
		int nServers;
		synchronized (this) {
			nServers = socketsMap.size(); // including coordinator server
		}
		System.out.println("nW:" + nW + " nR:" + nR + " nservers:" + nServers);
		if (nW > nServers || nR > nServers) {
			System.out.println("Write Quorum is greater than the no of available servers. Use a diff nW value");
		} else if (nW < nServers / 2) {
			System.out.println("Write Quorum cannot be satisfied. No of servers: " + nServers + " . Nw: " + nW);
		} else if (nR + nW > nServers) {
			return true;
		}
		System.out.println("Quorum cannot be satisfied. No of servers: " + nServers + " . Nw: " + nW);

		return false;
	}

	private synchronized boolean isRequestSuccesful(String ackKey, int nAcksReqd) {
		int nAcks = ackMap.get(ackKey);
		System.out.println("nAcks:" + nAcks + " nAcksReqd:" + nAcksReqd);
		return nAcks == nAcksReqd;
	}

//	private synchronized void chooseFromCoordinator(String received, int articleId) {
//		// Action by coordinator as participant server
//		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//		lock.readLock().lock();
//		Article articleRead = articleMapAtCoordinator.get(articleId);
//		lock.readLock().unlock();
//		String chooseAckMapkey = MessageParser.getArticleResponseKey(received);
//		System.out.println("AckMapKey at Coordinator:" + chooseAckMapkey);
//		updateAckMap(chooseAckMapkey);
//
//		received = MessageParser.getCompleteMessage(received);
//		received = MessageParser.getAppendArticle(received, articleRead.getContents(), articleRead.getVersion());
//		// updating ack maps action as a coordinator
//		updateArticleResponsesMap(received);
//
//	}
//
//	private synchronized void readFromCoordinator(String received) {
//		// Action by coordinator as participant server
//
//		ConcurrentHashMap<Integer, Article> inputMessageToServerMap = new ConcurrentHashMap<Integer, Article>();
//		inputMessageToServerMap.putAll(articleMapAtCoordinator);
//		ObjectMapper objectMapper = new ObjectMapper();
//		try {
//			if (!inputMessageToServerMap.isEmpty()) {
//				String allArticles;
//
//				allArticles = objectMapper.writeValueAsString(inputMessageToServerMap);
//
//				System.out.println("Servers curr Article Map:" + allArticles);
//
//				received = MessageParser.getAppendArticle(received, allArticles, 0);
//				System.out.println("Response to coordinator:" + received);
//				// dos.writeUTF("COMPLETE;" + inputMessageToServer);
//			} else {
//				System.out.println("Servers curr Article Map is empty");
//
//			}
//		} catch (JsonProcessingException e) {
//			
//			e.printStackTrace();
//		}
//		String readResponseMapkey = MessageParser.getArticleResponseKey(received);
//		System.out.println("readResponseMapkey:" + readResponseMapkey);
//		updateAckMap(readResponseMapkey); // update no of ACks received by coordinator
//		updateReadResponsesMap(received); // capture read responses received by coordinator
//
//	}
//
//	private synchronized void postToCoordinatorSelf(String received, int articleID) {
//		System.out.println("Processing POST by coordinator as participant");
//
//		Article articleToPost = new Article(articleID, MessageParser.getContents(received));
//
//		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
//		rwl.writeLock().lock();
//
//		articleMapAtCoordinator.put(articleID, articleToPost);
//
//		rwl.writeLock().unlock();
//
//		String ackMapkey = received;
//		updateAckMap(ackMapkey);
//
//	}
//
//	private synchronized void replyToCoordinatorSelf(String received, int articleID, int parentArticleId) {
//		System.out.println("Processing Reply by coordinator as participant");
//
//		Article articleToPost = new Article(articleID, MessageParser.getContents(received));
//		articleToPost.setParentId(parentArticleId);
//
//		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
//		rwl.readLock().lock();
//		Article parentArticle = articleMapAtCoordinator.get(parentArticleId);
//		parentArticle.addReply(articleToPost);
//		rwl.readLock().unlock();
//
//		rwl.writeLock().lock();
//
//		articleMapAtCoordinator.put(articleID, articleToPost);
//		articleMapAtCoordinator.put(parentArticleId, parentArticle);
//
//		rwl.writeLock().unlock();
//
//		String ackMapkey = received;
//		updateAckMap(ackMapkey);
//
//	}
}