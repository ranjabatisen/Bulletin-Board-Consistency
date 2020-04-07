/**
 * 
 */
package com.server.communication;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import com.utility.ServerExecutor;

/*
 */
public class ServerExecutorAtCoord extends ServerExecutor implements Runnable {
	
	private static String COORDINATOR_IP = "localhost";
	private static int COORDINATOR_PORT = 5057;
	private static String SYNC_COORDINATOR_IP = "localhost";
	private static int SYNC_COORDINATOR_PORT = 5058;
	private static ServerSocket serverAtCoordinatorSock;
	private static int serverPort = 5075;

	@Override
	public void run() {
		COORDINATOR_PORT = CoordinatorServer.coordinatorPort;
		SYNC_COORDINATOR_PORT = CoordinatorServer.synchronizerPort;
		serverPort = CoordinatorServer.serverAtCoordinatoPort;
		
		try {
		serverAtCoordinatorSock = new ServerSocket(serverPort);
		System.out.println("Starting server on the port: " + serverPort);
		System.out.println("Entering Server Executor at coord");
		Socket serverToCoordinatorSock;
		
			serverToCoordinatorSock = new Socket(COORDINATOR_IP, COORDINATOR_PORT);
		
			ServerServerConnection serverCordinatorConn = new ServerServerConnection(serverToCoordinatorSock);
			new Thread(serverCordinatorConn).start();
		
		// start thread on server to listen to sync coordinator
		Socket serverToSyncCoordinatorSock =  new Socket(SYNC_COORDINATOR_IP, SYNC_COORDINATOR_PORT); 
		ServerSynchronizationThread serverSyncCordinatorConn = new ServerSynchronizationThread(serverToSyncCoordinatorSock);
		new Thread(serverSyncCordinatorConn).start();
		
		while(true) {
			
			Socket socket;
			try {
				socket = serverAtCoordinatorSock.accept();
				System.out.println("Accepted client socket:"+ socket.getInetAddress().toString()+ "on port:"+ socket.getPort());
				
				ServerExecutor.serverClientSocketMap.put(socket.getInetAddress().toString()+socket.getPort() , socket);
				ServerClientConnectionThread serverClientConnectionThread = new ServerClientConnectionThread(socket, serverToCoordinatorSock);
				serverClientConnectionThread.start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}
	
}
