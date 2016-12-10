import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Client {
	private RouterTable rt;
	private String myIp;
	private int serverPort;
	private Map<Thread,Socket> pool;
	private ServerSocket ss;
	private Thread serverThread;
	private Thread rtThread;
	private boolean exit;

	TextArea textArea;
	JFrame frame;
	JPanel panel;
	
	public Client(String ip, int port) {
		try {
			myIp = ip;
			serverPort = port;
			
			ss = new ServerSocket(serverPort);
			rt = new RouterTable();
			rt.table.add(new Record(myIp, myIp, 0, 0));
			pool = new HashMap<Thread, Socket>();
			
			exit = false;
			
			serverThread = new ConnectThread();
			serverThread.start();
			
			rtThread = new RtThread();
			rtThread.start();

			frame = new JFrame("messages received");
			JFrame.setDefaultLookAndFeelDecorated(true);
			frame.setSize(500, 500);
			
			panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
			textArea = new TextArea();
			textArea.setEditable(false);
			textArea.setSize(400, 250);
			panel.add(textArea);
			
			frame.add(panel);
			frame.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public int connect(String ip, int port) {
		for (Map.Entry<Thread, Socket> entry: pool.entrySet()) {
			Socket socket = entry.getValue();
			if (socket.getInetAddress().getHostAddress().toString().equals(ip)) return 0;
		}
		try {
			Socket socket = new Socket(ip, port);
			Thread t = new RecvThread(socket);
			pool.put(t, socket);
			t.start();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
		
		return 1;
	}

	public int disconnect(String ip) {
		try {
			for (Map.Entry<Thread, Socket> entry: pool.entrySet()) {
				Socket socket = entry.getValue();
				if (socket.getInetAddress().getHostAddress().toString().equals(ip)) {
					rt.removeRecord(ip);
					entry.getKey().interrupt();
					socket.close();
					pool.remove(entry);
					return 1;
				}
			}
			return 0;
		} catch(Exception e) {
			e.printStackTrace();
			return -1;
		}
		
	}
	
	
	public int send(String ip, String msg) {
		String next = rt.getShortcut(ip);

		if (next.equals("invalid")) return 0;

		System.out.println(next);

		for (Map.Entry<Thread, Socket> entry: pool.entrySet()) {
			Socket socket = entry.getValue();
			if (socket.getInetAddress().getHostAddress().toString().equals(next)) {
		        try {
		           	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
		            out.println(msg);
				} catch (Exception e) {
					e.printStackTrace();
					return -1;
				}
				break;
			}
		}
		
		return 1;
	}
	
	
	public void shutdown() {
		try {
			exit = true;
			frame.dispose();
			Socket socket = new Socket(myIp,serverPort);
			socket.close();
			for (Map.Entry<Thread, Socket> entry: pool.entrySet()) {
				entry.getValue().close();
				entry.getKey().join();
			}
			serverThread.join();
			rtThread.join();
			System.out.println("all the threads of client have been closed");
			ss.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	public Boolean isSocketClosed(Socket socket) {
		try{  
    		socket.sendUrgentData(0); 
    		return false;  
   		} catch(Exception se){  
    		return true;  
   		}
	}
	
	
	class ConnectThread extends Thread {
		public ConnectThread() {}
		public void run() {
			while(!exit) {
				try {
					Socket socket = ss.accept();
					Thread t = new RecvThread(socket);
					pool.put(t, socket);
					t.start();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}
	
	
	class RecvThread extends Thread {
		Socket socket;

		public RecvThread(Socket s) {
			socket = s;
		}

		public void run() {
			try {
				BufferedReader in;
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				String msg;
	            while (!exit) {
	            	if ((msg = in.readLine()) != null) {
	            		if (msg.charAt(0) == 'r') {
	            			rt.update(socket.getInetAddress().getHostAddress().toString(), msg);
	            			textArea.append(socket.getInetAddress().getHostAddress().toString()+" send "+msg+"\n");
	            		} else {
	            			String[] strs = msg.split("#");
	            			if (strs[1].equals(myIp)) textArea.append("\n"+strs[2]+":\n"+strs[3]+"\n\n");
	            			else {
	            				strs[2] += ("->"+myIp);
	            				send(strs[1],strs[0]+"#"+strs[1]+"#"+strs[2]+"#"+strs[3]);
	            			}
	            		}
	            	}
	            }
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	class RtThread extends Thread {
		public RtThread() {}

		public void run() {
			try {
				while (!exit) {
					for (Map.Entry<Thread, Socket> entry: pool.entrySet()) {
						Socket socket = entry.getValue();
			            String s = "r#";
			            for (Record r: rt.table) {
			            	s += r.dst+" "+r.cost.toString()+"#";
				        }

				        if (isSocketClosed(socket)) {
				        	rt.removeRecord(socket.getInetAddress().getHostAddress().toString());
				        	entry.getKey().interrupt();
				        	socket.close();
							pool.remove(entry);
							textArea.append(socket.getInetAddress().getHostAddress().toString()+" lost connection\n");
				        } else {
				        	PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
				        	out.println(s);
				        	textArea.append("send to "+socket.getInetAddress().getHostAddress().toString()+" "+s+"\n");
				        }
					}
				}
				sleep(10000);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	
	public static void main(String[] args) {
		try {
			InetAddress address = InetAddress.getLocalHost();
			String ip = address.getHostAddress().toString();
			Client client = new Client(ip, 8080);
			
			Scanner scanner = new Scanner(System.in);

			System.out.println("Client started");

			while (true) {
				System.out.print(ip+">");
				String op = scanner.nextLine();
				String[] arg = op.split(" ");
				if (arg[0].equals("connect") && arg.length == 2) {
					int status = client.connect(arg[1], 8080);
					System.out.println("Status:" + status);
				} else if (arg[0].equals("send") && arg.length == 3) {
					String msg = "m#"+arg[1]+"#"+address+"#"+arg[2];
					int status = client.send(arg[1], msg);
					System.out.println("Status:" + status);
				} else if (arg[0].equals("shutdown") && arg.length == 1) {
					client.shutdown();
					break;
				} else if (arg[0].equals("disconnect") && arg.length == 2) {
					int status = client.disconnect(arg[1]);
					System.out.println("Status:" + status);
				}
			}
			System.out.println("client has exited");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
