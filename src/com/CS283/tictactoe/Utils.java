package com.CS283.tictactoe;

import java.net.DatagramSocket;
import java.net.InetSocketAddress;

final public class Utils {

	final String serverAddress_ = "54.186.216.144";
	final int serverPort_ = 20000;
			
	
	final static int MAX_PACKET_SIZE_ = 512;
	
	final InetSocketAddress serverSocketAddress_ = new InetSocketAddress(
			serverAddress_, serverPort_);
	
	public DatagramSocket socket_ = null;
}
