package com.server.communication;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class BroadcastHandler {

	public BroadcastHandler() {

	}
	// messageToBroadcast =
	// POST;articleId;nR;nW:articlecontents;;version;serverport;clientport
	// CHOOSE;articleId;nR;nW;nR:nW:articlecontents;;version;serverport;clientport
	// REPLY;articleId;nR;nW;articlecontents;replyToId;version;serverport;clientport

	public void broadcastPost(String messageToBroadcast, ConcurrentHashMap<String, Socket> readServersMap,
			String currentSenderId, boolean excludeCurrSvrBroadcastList) {

		System.out.println("Running broadcast initiated by" + currentSenderId);
		for (String currServerId : readServersMap.keySet()) {

			System.out.println("Serverhost is " + currServerId);

			if (excludeCurrSvrBroadcastList && currServerId.equalsIgnoreCase(currentSenderId)) {
				continue;
			} else {
				try {
					// coordinator socket => op to server
					DataOutputStream newDOstream = new DataOutputStream(
							readServersMap.get(currServerId).getOutputStream());

					System.out.println("sending message " + messageToBroadcast);

					newDOstream.writeUTF(messageToBroadcast); // message sent to server
					System.out.println(
							"message broadcasted to server " + currServerId + ". Message: " + messageToBroadcast);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}