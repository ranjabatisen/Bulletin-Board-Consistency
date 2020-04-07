package com.server.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.model.Article;
import com.utility.ServerExecutor;

public class CoordinatorServer implements Runnable {

	public static int coordinatorPort = 5057;
	public static ServerSocket coordinatorSock;

	public static int synchronizerPort = 5058;
	public static ServerSocket syncCoordinatorSock;
	
	public static int serverAtCoordinatoPort;

	public static volatile ConcurrentHashMap<String, Socket> socketsMap = new ConcurrentHashMap<String, Socket>();
	public static volatile ConcurrentHashMap<String, Integer> ackMap = new ConcurrentHashMap<String, Integer>();
	public static volatile ConcurrentHashMap<String, TreeMap<Integer, String>> articlesResponsesMap = new ConcurrentHashMap<String, TreeMap<Integer, String>>();
	public static volatile ConcurrentHashMap<Integer, Article> articleMapAtCoordinator = new ConcurrentHashMap<Integer, Article>();
	public static volatile Integer articleID;
	public static volatile List<ConcurrentHashMap<Integer, Article>> articleSyncMapList = 
			new ArrayList<ConcurrentHashMap<Integer, Article>>();
	
	public static volatile ConcurrentHashMap<String, ConcurrentHashMap<Integer, Article>> readResponsesMap = new ConcurrentHashMap<String, ConcurrentHashMap<Integer, Article>>();
	
	public static volatile ConcurrentHashMap<String, Socket> syncSocketsMap = new ConcurrentHashMap<String, Socket>();

	static Thread coordinatorThread;

	/**
	 * args 0 -> coordinator port
	 * args 1 -> synchronizer port
	 * args 2 -> server at coordinator port
	 * args 3 -> NR
	 * args 4 -> NW
	 * 
	 */
	
	public static void main(String args[]) {
		
		if(args.length < 5) {
			System.out.println("Not enough arguments, 5 arguments are needed");
		} else {
			coordinatorPort = Integer.parseInt(args[0]);
			synchronizerPort = Integer.parseInt(args[1]);
			serverAtCoordinatoPort = Integer.parseInt(args[2]);
			ServerExecutor.NR = Integer.parseInt(args[3]);
			ServerExecutor.NW = Integer.parseInt(args[4]);
			
			articleID = 0;
			try {
				coordinatorSock = new ServerSocket(coordinatorPort);
				syncCoordinatorSock = new ServerSocket(synchronizerPort);

				Runnable coordinatorServer = new CoordinatorServer();
				Runnable dummyServerAtCoord = new ServerExecutorAtCoord();
				Thread dummyServerThread = new Thread(dummyServerAtCoord);
				dummyServerThread.start();
				
				coordinatorThread = new Thread(coordinatorServer);
				coordinatorThread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}

		
	}

	@Override
	public void run() {
		System.out.println("CoordinatorServer is started on the port: " + coordinatorPort);

		System.out.println("SyncCoordinatorServer is started on the port: " + synchronizerPort);
		while (true) { // while loop to keep running for new servers that get added to the cluster

			acceptConnections(coordinatorSock, false);
			acceptConnections(syncCoordinatorSock, true);
		}
	}

	private void acceptConnections(ServerSocket coordinatorSock, boolean isSync) {
		Socket newClientConnSock = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		String addrPort = null;

		try {
			newClientConnSock = coordinatorSock.accept();
			addrPort = newClientConnSock.getInetAddress().toString() + newClientConnSock.getPort(); // gives IP &
																									// port of																					// server
			dis = new DataInputStream(newClientConnSock.getInputStream()); // from server to coordinator
			dos = new DataOutputStream(newClientConnSock.getOutputStream());

			if (!isSync) {
				System.out.println("New Server is connected to coord " + newClientConnSock + "addrPort is " + addrPort);
				socketsMap.put(addrPort, newClientConnSock);
				// new thread to handle each server
				Thread serverConnHandler = new Thread(new ServerConnectionsHandler(dis, dos, addrPort));
				serverConnHandler.start();
			} else {
				System.out.println(
						"New Server is connected to sync coord " + newClientConnSock + "addrPort is " + addrPort);
				syncSocketsMap.put(addrPort, newClientConnSock);
				// new thread to handle each server's sync communication
				Thread serverSyncHandler = new Thread(new ServerSyncCoordinatorThread(dis, dos, addrPort));
				serverSyncHandler.start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static synchronized void updateArticleMapAtCoordinator(ConcurrentHashMap<Integer, Article> updatedMap) {
		final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
		rwl.writeLock().lock();
		articleMapAtCoordinator = updatedMap;
		rwl.writeLock().unlock();
	}

}
