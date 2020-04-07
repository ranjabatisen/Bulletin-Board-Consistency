package com.utility;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.model.Article;
import com.server.communication.ServerClientConnectionThread;
import com.server.communication.ServerServerConnection;
import com.server.communication.ServerSynchronizationThread;

public class ServerExecutor {
	// TODO rename to ParticipantServerExecutor

	int serverPort;
	private static ServerSocket serverSocket;

	private static String COORDINATOR_IP = "localhost";
	private static int COORDINATOR_PORT = 5057;
	public static final int READ_LENGTH = 2;
	private static String SYNC_COORDINATOR_IP = COORDINATOR_IP;
	private static int SYNC_COORDINATOR_PORT = 5058;
	public static int NR = 0;
	public static int NW = 0;

	public static ConcurrentHashMap<String, Socket> serverClientSocketMap = new ConcurrentHashMap<String, Socket>();
	// making it private so that read and write happens synchronously via get and
	// update methods below
	private static volatile ConcurrentHashMap<Integer, Article> articleMap = new ConcurrentHashMap<Integer, Article>();

	public ServerExecutor(int serverPort) {
		this.serverPort = serverPort;

	}

	public ServerExecutor() {

	}

	/**
	 * 
	 *  args 0 -> coordinator IP
	 *  args 1 -> coordinator port
	 *  args 2 -> sync port
	 *  args 3 -> server Port
	 *  args 4 -> NR
	 *  args 5 -> NW
	 *  
	 */
	public static void main(String[] args) {
		
		if(args.length < 6) {
			System.out.println("Not enough arguments, 6 arguments required");
		} else {
			COORDINATOR_IP = args[0];
			COORDINATOR_PORT = Integer.parseInt(args[1]);
			SYNC_COORDINATOR_PORT = Integer.parseInt(args[2]);
			int serverPort = Integer.parseInt(args[3]); // 5056 - original
			NR = Integer.parseInt(args[4]);
			NW = Integer.parseInt(args[5]);
			new ServerExecutor(serverPort).startServer();
		}
		
	}

	public static synchronized ConcurrentHashMap<Integer, Article> getArticleMap() {

		return articleMap;

	}

	public static synchronized void updateArticleMap(int articleId, Article article) {
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.writeLock().lock();
		articleMap.put(articleId, article);
		rwl.writeLock().unlock();
	}

	public void startServer() {
		try {
			serverSocket = new ServerSocket(serverPort);
			System.out.println("Starting server on the port: " + serverPort);
			
			Socket serverToCoordinatorSock = new Socket(COORDINATOR_IP, COORDINATOR_PORT);
			ServerServerConnection serverCordinatorConn = new ServerServerConnection(serverToCoordinatorSock);
			new Thread(serverCordinatorConn).start();
			
			// start thread on server to listen to sync coordinator
			Socket serverToSyncCoordinatorSock =  new Socket(SYNC_COORDINATOR_IP, SYNC_COORDINATOR_PORT); 
			ServerSynchronizationThread serverSyncCordinatorConn = new ServerSynchronizationThread(serverToSyncCoordinatorSock);
			new Thread(serverSyncCordinatorConn).start();

			while (true) { // loop to accept multiple client connections for a single server
				Socket socket = serverSocket.accept();
				System.out.println("Accepted client socket:"+ socket.getInetAddress().toString()+ "on port:"+ socket.getPort());
				
				serverClientSocketMap.put(socket.getInetAddress().toString()+socket.getPort() , socket);
				ServerClientConnectionThread serverClientConnectionThread = new ServerClientConnectionThread(socket, serverToCoordinatorSock);
				serverClientConnectionThread.start();
			}

		} catch (IOException e) {
			System.out.println("Exception in starting server");
			e.printStackTrace();
		}
	}

	public static ConcurrentHashMap<String, Socket> getServerClientSocketMap() {
		return serverClientSocketMap;
	}

}
