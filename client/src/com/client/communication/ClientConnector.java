package com.client.communication;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Class for handling communication between client and server
 */
public class ClientConnector {

	private Socket socket = null;
	private InetAddress clientIP = null;
	private DataInputStream dis = null;
	private DataOutputStream dos = null;
	public boolean isAlive = false;

	public ClientConnector(int serverPort, String serverIp) {

		try {
			// TODO Remove hardcoding of client IP
			this.clientIP = InetAddress.getByName("localhost");
			this.socket = new Socket(this.clientIP, serverPort);
			System.out.println(this.socket);
			dis = new DataInputStream(this.socket.getInputStream());
			dos = new DataOutputStream(this.socket.getOutputStream());
			this.isAlive = true;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public  boolean isAlive() {
		return isAlive;
	}
	
	public String readFromSocket() {
		String response = null;
		System.out.println("Reading from socket:");
		try {
			
			response = dis.readUTF();
			System.out.println("Response of read from socket:");
			
			if(response.contentEquals("EXIT")) {
				this.isAlive = false;
				this.closeSocket();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}

	public void closeSocket() {
		try {
			System.out.println("Closing client socket");
			this.socket.close();
			this.dos.close();
			this.dis.close();
			this.isAlive = false;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToSocket(String operation) {
		try {
			dos.writeUTF(operation);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToSocket(int articleID) {
		try {
			System.out.println("writing to dos article ID " + articleID);
			dos.writeInt(articleID);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}