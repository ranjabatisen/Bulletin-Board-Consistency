package com.server.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.model.MessagesFromServer;
import com.services.QuorumConsistService;
import com.services.ReadYourWriteConService;
import com.services.SequentialConService;
import com.services.Service;
import com.utility.ServerExecutor;

public class ServerClientConnectionThread extends Thread {
	private static final int SEQUENTIAL_CONSISTENCY = 1;
	private static final int QUORUM_CONSISTENCY = 2;
	private static final int READ_YOUR_WRITE_CONSISTENCY = 3;
	private static final String GET_ALL_SERVERS = "Get All Servers";
	private static final int EXIT = 0;

	private Socket serverToCoordinatorSock; // server to coordinator socket
	private Socket socket = null; // server to client socket

	public ServerClientConnectionThread(Socket socket, Socket serverToCoordinatorSock) {
		// client server socket
		this.socket = socket;
		this.serverToCoordinatorSock = serverToCoordinatorSock;
	}

	@Override
	public void run() {
		int consiType = 0;
		String received = null;
		int articleID;

		DataInputStream dis = null;
		DataOutputStream dos = null;
		Service serviceObj = null;
		try {

			System.out.println("New client is connected " + socket);
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			String input = dis.readUTF();
			if (input.equalsIgnoreCase(GET_ALL_SERVERS)) {

				// SPOST;articleId;nr;nw;articlecontents;articleIdreply;version;serverIpPort;clientIpPort
				String serverIP = serverToCoordinatorSock.getInetAddress().toString()
						+ serverToCoordinatorSock.getLocalPort();
				serverIP = MessageParser.trimLocalHostFromServerIpPort(serverIP);
				String clientIP = socket.getInetAddress().toString() + socket.getPort();
				String messageToCoord = GET_ALL_SERVERS + ";-1;;;;;;" + serverIP + ";" + clientIP;
				System.out.println("Sending message to coordinator " + messageToCoord);
				new DataOutputStream(serverToCoordinatorSock.getOutputStream()).writeUTF(messageToCoord);
				String serversList = new DataInputStream(serverToCoordinatorSock.getInputStream()).readUTF();
				dos.writeUTF(serversList);

			}

			do {
				try {
					// dos.writeUTF(MessagesFromServer.MESSAGE_FOR_CONSISTENCY_TYPE);
					consiType = dis.readInt();

					System.out.println("consistency type is: " + consiType);

					switch (consiType) {
					case SEQUENTIAL_CONSISTENCY:
						serviceObj = new SequentialConService(serverToCoordinatorSock,
								socket.getInetAddress().toString() + socket.getPort());

						break;
					case QUORUM_CONSISTENCY:
						serviceObj = new QuorumConsistService(serverToCoordinatorSock,
								socket.getInetAddress().toString() + socket.getPort(), ServerExecutor.NR,
								ServerExecutor.NW);
						break;
					case READ_YOUR_WRITE_CONSISTENCY:
						serviceObj = new ReadYourWriteConService(serverToCoordinatorSock,
								socket.getInetAddress().toString() + socket.getPort());
						break;
					case EXIT:
						System.out.println("Received consistency as 0, client exiting server");
						dos.writeUTF("EXIT");
						break;
					default:
						break;
					}
					if (consiType != 0) {
						received = dis.readUTF();

						switch (received) {
						case MessagesFromServer.READ:

							serviceObj.read();
							break;

						// dos.writeU
						case MessagesFromServer.CHOOSE:
							// dos.writeUTF(MessagesFromServer.MESSAGE_FOR_ARTICLE_ID); // output to client
							articleID = dis.readInt(); // input from client
							serviceObj.choose(String.valueOf(articleID));

							// dos.writeUTF(article);
							break;
						case MessagesFromServer.POST:
							// dos.writeUTF(MessagesFromServer.MESSAGE_FOR_ARTICLE_CONTENTS);
							String articleContents = dis.readUTF();
							// To Do - return article instead of string
							serviceObj.post(articleContents);
							// dos.writeUTF(articlePostAck); // server writes acknowledgement to client
							break;
						case MessagesFromServer.REPLY:
							// dos.writeUTF(MessagesFromServer.MESSAGE_FOR_PARENT_ARTICLE_ID);
							articleID = dis.readInt();
							// dos.writeUTF(MessagesFromServer.MESSAGE_FOR_ARTICLE_CONTENTS);
							articleContents = dis.readUTF();
							// To Do - return article instead of string
							serviceObj.reply(articleContents, articleID);
							// dos.writeUTF(articlePostAck); // server writes acknowledgement to client
							break;
						case MessagesFromServer.EXIT:
							System.out.println("Client is exiting");
							socket.close();
							break;
						default:
							System.out.println("Invalid input at server " + received);
						}

					}
				} catch (IOException e) {
					try {
						System.out.println("closing server socket with client due to broken pipe at client:" + socket);
						socket.close();
						break;
					} catch (IOException e1) {
						e1.printStackTrace();
					}

				}

			} while (consiType != 0 && !received.equalsIgnoreCase(MessagesFromServer.EXIT));
			System.out.println("ServerClientConnectionThread exited");

		} catch (IOException e) {
			e.printStackTrace();
		}

		finally {
			try {
				System.out.println("Connection Closing..");
				if (dis != null) {
					dis.close();
					System.out.println("Socket Input Stream Closed: " + socket);
				}
				if (dos != null) {
					dos.close();
					System.out.println("Socket Out Closed: " + socket);
				}
				if (socket != null) {
					socket.close();
					System.out.println("Socket Closed: " + socket);
				}

			} catch (IOException ie) {
				System.out.println("Socket Close Error: " + socket);
			}
		}
		// end finally
	}

}