package com.server.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.model.Article;
import com.utility.ServerExecutor;


public class ServerSynchronizationThread extends ServerExecutor implements Runnable {

	private Socket serverToCoordinatorSock;

	public ServerSynchronizationThread(Socket serverToCoordinatorSock) {
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

		while (true)

		{
			try {
				// check if no article by client or no broadcast by coordinator
				while (dis.available() < 1) {
					Thread.sleep(800);

				}
				String inputMessageToServerJson = dis.readUTF();

				ObjectMapper objectMapper = new ObjectMapper();
				ConcurrentHashMap<Integer, Article> inputMessageToServer = objectMapper
						.readValue(inputMessageToServerJson, new TypeReference<ConcurrentHashMap<Integer, Article>>() {
						});

				if (inputMessageToServer.get(0).getContents().startsWith("QSYNC")) {
						
						System.out.println("Received QSYNC at the server. Timestamp: "+ inputMessageToServer.get(0).getContents());
						inputMessageToServer.putAll(ServerExecutor.getArticleMap());
						System.out.println("Servers curr Article Map:"
								+ objectMapper.writeValueAsString(ServerExecutor.getArticleMap()));
						inputMessageToServer.put(0, new Article(0, "COMPLETE;QSYNC"));

						dos.writeUTF(objectMapper.writeValueAsString(inputMessageToServer));

				} else if (inputMessageToServer.get(0).getContents().startsWith("COMPLETE;QSYNC")) {
					inputMessageToServer.remove(0);
					// update article map at server based on the input message
					// possible race condition hence used ReadWrite Lock
					updateArticleMap(inputMessageToServer);
					System.out.println("Updated Article Map at server:" + ServerExecutor.getArticleMap());
				}

			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void updateArticleMap(ConcurrentHashMap<Integer, Article> articleMap) {

		for (Entry<Integer, Article> entry : articleMap.entrySet()) {
			ServerExecutor.updateArticleMap(entry.getKey(), entry.getValue());
		}
		return;

	}

}