package com.server.communication;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.model.Article;


public class ServerSyncCoordinatorThread extends CoordinatorServer implements Runnable {
	private DataOutputStream dos; // dos from synccoordinator to server
	private DataInputStream dis; // dis from server to synccoordinator
	private String currentServerId;

	public ServerSyncCoordinatorThread(DataInputStream dis, DataOutputStream dos, String addrPort) {
		super();
		this.dis = dis;
		this.dos = dos;
		this.currentServerId = addrPort; // unique id about socket connection when running servers in same machine
	}

	@Override
	public void run() {

		Thread triggerSyncThread = new Thread() {

			@Override
			public void run() {
				ConcurrentHashMap<Integer, Article> triggerSync = new ConcurrentHashMap<Integer, Article>();

				try {

					while (true) {

						Thread.sleep(90000); // trigger sync operation from server every 90 seconds

						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						triggerSync.put(0, new Article(0, "QSYNC" + ";" + timestamp.getTime()));
						ObjectMapper objectMapper = new ObjectMapper();
						String json = objectMapper.writeValueAsString(triggerSync);

						System.out.println("Triggering Sync request by coordinator");
						BroadcastHandler bHandler = new BroadcastHandler();
						bHandler.broadcastPost(json, CoordinatorServer.syncSocketsMap, currentServerId, false);

					}
				} catch (JsonProcessingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		};
		triggerSyncThread.start();
		while (true) {
			try {
				String receivedJson = null;
				while (dis.available() < 1) { // message to coordinator from server
					Thread.sleep(200);
				}
				System.out.println(" Waiting for sync request from server");
				receivedJson = dis.readUTF();
				ObjectMapper objectMapper = new ObjectMapper();
				System.out.println(receivedJson);
				ConcurrentHashMap<Integer, Article> received = objectMapper.readValue(receivedJson, TypeFactory
						.defaultInstance().constructMapType(ConcurrentHashMap.class, Integer.class, Article.class));
				System.out.println(received);

				if (received.get(0).getContents().startsWith("COMPLETE;QSYNC")) {
					synchronized (this) {

						Article dummy = received.get(0);
						received.remove(0);
						articleSyncMapList.add(received); // add to the received list at coordinator
						ConcurrentHashMap<Integer, Article> updatedMap = new ConcurrentHashMap<Integer, Article>();

						if (articleSyncMapList.size() == CoordinatorServer.syncSocketsMap.size() + 1) {
							
							updatedMap = getLatestArticleMap(CoordinatorServer.articleSyncMapList);
							CoordinatorServer.updateArticleMapAtCoordinator(updatedMap); // sync map at coordinator

							updatedMap.put(0, dummy); // dummy contents for server to recognise the update
							System.out.println("Processed QSYNC request at coordinator");
							BroadcastHandler bHandler = new BroadcastHandler();
							// broadcast updated map to all participant servers
							bHandler.broadcastPost(objectMapper.writeValueAsString(updatedMap),
									CoordinatorServer.syncSocketsMap, currentServerId, false);
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

	private synchronized ConcurrentHashMap<Integer, Article> getLatestArticleMap(
			List<ConcurrentHashMap<Integer, Article>> articleSyncMapList) {
		ConcurrentHashMap<Integer, Article> updatedMap = new ConcurrentHashMap<Integer, Article>();

		for (ConcurrentHashMap<Integer, Article> articleMap : articleSyncMapList) {
			for (Entry<Integer, Article> entry : articleMap.entrySet()) {
				int key = entry.getKey();
				Article article = entry.getValue();

				if (updatedMap.get(key) == null) {
					updatedMap.put(key, article);

				} else if (article.getVersion() > updatedMap.get(key).getVersion()) {

					updatedMap.put(key, article);
				} else if (article.getVersion() == updatedMap.get(key).getVersion() && article.getParentId()> updatedMap.get(key).getParentId()) {
					updatedMap.put(key, article);
				}else if (article.getVersion() == updatedMap.get(key).getVersion() && article.getParentId()> updatedMap.get(key).getParentId()
						&& article.getReplies().size()> updatedMap.get(key).getReplies().size()) {
					updatedMap.put(key, article);
				}
			}
		}
		return updatedMap;
	}

}