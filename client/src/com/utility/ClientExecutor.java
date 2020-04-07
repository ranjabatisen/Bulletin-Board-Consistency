package com.utility;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.client.communication.ClientConnector;

import java.io.*;
import java.net.*;
import java.sql.Timestamp;

public class ClientExecutor {

	public static int servletPort;
	
	private static final String READ = "Read";
	private static final String CHOOSE = "Choose";
	private static final String POST = "Post";
	private static final String REPLY = "Reply";
	private static final String EXIT = "Exit";
	private static final String GET_ALL_SERVERS = "Get All Servers";
	private static final int NUMBER_OF_ARTICLES_ON_PAGE = 2;
	private static String operation = null;
	private static int consType = 0;
	private static int articleID = 0;
	private static String postArticle = null;
	
	
	private List<String> allServersList = new ArrayList();

	public ClientExecutor() {

	}

	/*
	 * args 0 - > serverIP
	 * args 1 -> serverPort
	 * args 2 -> servlet port
	 */
	public static void main(String[] args) {
		if(args.length < 3) {
			System.out.println("Not enough arguments, 3 arguments required");
		} else {
			String serverIp = args[0];
			int serverPort = Integer.parseInt(args[1]);
			servletPort = Integer.parseInt(args[2]);
//			String serverIp = "127.0.0.1";
//			int serverPort = 5075;

			ClientExecutor executor = new ClientExecutor();
			ClientConnector connection = new ClientConnector(serverPort, serverIp);
			System.out.println("Established client's connection with server");
	                try {
	                    executor.executeClient(connection);
	                }
	                catch(Exception e) {
	                    System.out.println("erron in main method "+e);
	                }
			}
		
		}

	private int getInput(DatagramSocket ds) throws IOException {
		byte[] receive = new byte[65535];
		DatagramPacket DpReceive = null;
		DpReceive = new DatagramPacket(receive, receive.length);

		ds.receive(DpReceive);

		System.out.println("Message received from Java Servlet:-" + data(receive));

		String str = data(receive).toString();
		String arr[] = str.split(",");
		String inp = "";
		if (arr[0].equals("Sequential_consistency")) {
			consType = 1;
		}
		if (arr[0].equals("Quorum_consistency")) {
			consType = 2;
		}
		if (arr[0].equals("RW_consistency")) {
			consType = 3;
		}
		if (arr[1].equals("post")) {
			operation = "Post";
			postArticle = arr[2];
		}
		if (arr[1].equals("read")) {
			operation = "Read";
		}
		if (arr[1].equals("choose")) {
			operation = "Choose";
			articleID = Integer.parseInt(arr[2]);
		}
		if (arr[1].equals("reply")) {
			operation = "Reply";
			articleID = Integer.parseInt(arr[2]);
			postArticle = arr[3];
		}
		int port = DpReceive.getPort();
		return port;
	}

	private void sendOutput(DatagramSocket ds, String output, int port) throws IOException {
		InetAddress ip = InetAddress.getLocalHost();
		byte buf[] = null;
		buf = output.getBytes();
		DatagramPacket DpSend = new DatagramPacket(buf, buf.length, ip, port);
		ds.send(DpSend);
		System.out.println("Sent");
	}

	private void executeClient(ClientConnector connection) throws IOException {
		System.out.println("Started Executing client");
		connection.writeToSocket(GET_ALL_SERVERS);

		String messageFromServer = connection.readFromSocket();
		// received =
		// GET_ALL_SERVERS+";"+reqSenderServerIpPort+";"+clientIP+";"+serverList;
		System.out.println("Get all servers list recieved from server " + messageFromServer);
		deserializedServersList(messageFromServer.split(";")[3]);
		// Scanner scn = new Scanner(System.in);
		// String operation = null;
		String output = null;

		DatagramSocket ds = new DatagramSocket(servletPort);
		do {
			try {
				servletPort = getInput(ds);
				
			} catch (Exception e) {
				System.out.println("error in execute client " + e);
			}
			// System.out.println(connection.readFromSocket());
			// int consType = scn.nextInt();

			// get all available servers
			System.out.println("writing get all servers request to server");

			connection.writeToSocket(consType);
			System.out.println("writing consType to server" + consType);

			// System.out.println(connection.readFromSocket());

			// operation = scn.next();
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			long startTime = timestamp.getTime();
			System.out.println("Current timestamp is " + startTime);
			System.out.println("operation is " + operation);

			connection.writeToSocket(operation);

			if (operation.equalsIgnoreCase(EXIT)) {

				System.out.println("Closing the connection with server");
				connection.closeSocket();
				break;
			}
			switch (operation) {
			case READ:
				// System.out.println(connection.readFromSocket());
				// int articleID = scn.nextInt();
				// connection.writeToSocket(articleID);

				break;
			case CHOOSE:
				// System.out.println(connection.readFromSocket());
				// articleID = scn.nextInt();
				connection.writeToSocket(articleID);

				break;
			case POST:
				// connection.readFromSocket();
				// String postArticle = scn.next();
				connection.writeToSocket(postArticle);

				break;
			case REPLY:
				// System.out.println(connection.readFromSocket());
				// articleID = scn.nextInt(); //parent article id
				connection.writeToSocket(articleID);
				// System.out.println(connection.readFromSocket());
				// postArticle = scn.next();
				connection.writeToSocket(postArticle);
				// output = "Reply done";

				break;
			case EXIT:
				System.out.println("Closing the connection with server");
				connection.closeSocket();
				break;

			default:
				System.out.println("Invalid input at client " + operation);
				break;
			}

			String response = connection.readFromSocket();
			timestamp = new Timestamp(System.currentTimeMillis());
			long endTime = timestamp.getTime();
			System.out.println("Opeation ended. timestamp is " + endTime);
			long totalTimeTaken = endTime - startTime;
			System.out.println("Total time taken is " + totalTimeTaken);
			String totalTime = "Total time taken for this operation " + totalTimeTaken;
			sendOutput(ds, response + "\n" + totalTime, servletPort);
			System.out.println("response is " + response);

		} while (!operation.equalsIgnoreCase(EXIT));
		System.out.println("Connection ended");
	}

	private void deserializedServersList(String serversList) {

		String[] split = serversList.split("\\+");
		for (String server : split) {
			allServersList.add(server);
		}
	}

	public static StringBuilder data(byte[] a) {
		if (a == null)
			return null;
		StringBuilder ret = new StringBuilder();
		int i = 0;
		while (a[i] != 0) {
			ret.append((char) a[i]);
			i++;
		}
		return ret;
	}
}
