import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class Server {

    private ServerSocket serverSocket;
    private int portNumber = 5000;
    private String hostAddress = "localhost";
    private int maxBacklogConnections = 50;   
    private static final String regex = "$#@&76!:;";

    private Map<Socket,String> Client_Name_Map;
    private Map<String,Socket> Name_Client_Map;
    private Object mapLock;
    private Authentication authentication;
    
    public Server() throws IOException {
        mapLock = new Object();
        InetAddress address = InetAddress.getByName(hostAddress);
        serverSocket = new ServerSocket(portNumber, maxBacklogConnections, address);

        println("Server created on " + hostAddress + ", port : " + portNumber);

        Client_Name_Map = new HashMap<>();
        Name_Client_Map = new HashMap<>();
        authentication = new Authentication();
    }

    public Socket AddClient() throws IOException{
        Socket client = serverSocket.accept();
        println("Client connected : " + client.getInetAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        PrintWriter out = new PrintWriter(client.getOutputStream(), true);

        String credentials = in.readLine();
        String[] namePass = credentials.trim().split(":");
        
        var result = authentication.Verify(namePass[0].trim(), namePass[1].trim());
        String response = "";
        boolean valid = true;

        switch (result) {

            case Verified:
                response = "Verified.";
                break;    
            case NewRegistered:
                response = "Registered with server successfully.";
                break;
            default:
                response = "Error: Failed to verify!";
                valid = false;
                break;
        }

        if(valid){
            synchronized(mapLock){

                Client_Name_Map.put(client, namePass[0].trim());
                Name_Client_Map.put(namePass[0].trim(), client);
            }
        }

        out.println(response);
        System.out.println(namePass[0] + " connection status: " + response);
        return valid ? client : null; 
    }

    private static void println(String line){
        System.out.println(line);
    }

    private void DispatchClientThreads(Server server){
        Thread thread = new Thread(() -> {
            while(true){
                try{
                    var client = server.AddClient();
                    if(client != null){
                        HandleMessagePassing(client);
                    }
                }
                catch(Exception e){
                    println("Failed to connect! \n" + e.getMessage());
                }
            }
        });
        thread.start();
    }

    private void HandleMessagePassing(Socket client) {
        Thread thread = new Thread(() -> {
            try{
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                while(true){
                    String recieved = in.readLine();
                    if (recieved == null) break; 
                    ParseMessage(recieved, client);
                }
            }
            catch(Exception e){
                println("Client disconnected.");
            }
            finally{
                synchronized(mapLock){

                    String name = Client_Name_Map.remove(client);
                    if (name != null) Name_Client_Map.remove(name);
                }
                try { client.close(); } catch (IOException e) { /* Eat 5 stars, do nothing */}
                println("Closed connection.");
            }
        });
        thread.start();
    }

    private void ParseMessage(String message, Socket sender) throws IOException{
        if(message == null || message.trim().length() == 0) return;

        // so we need to user Pattern.quote() so special characters don't break the split
        String[] tokens = message.trim().split(Pattern.quote(regex));
        String senderName = Client_Name_Map.get(sender);

        switch (tokens[0]) {
            case "/msg":
                if (tokens.length >= 3) {
                    String targetName = tokens[1];
                    String msgBody = tokens[2];
                    Socket targetClient = null;
                    
                    synchronized(mapLock){
                        targetClient = Name_Client_Map.get(targetName);
                    }

                    if (targetClient != null) {

                        // One who is alive and breathing:
                        PrintWriter out = new PrintWriter(targetClient.getOutputStream(), true);
                        out.println("/msg" + regex + senderName + regex + msgBody);
                    } else {

                        // Handling those who are dead or in comma:
                        PrintWriter out = new PrintWriter(sender.getOutputStream(), true);
                        out.println("/system" + regex + "User '" + targetName + "' is offline or doesn't exist.");
                    }
                }
                break;

            case "/users":
                String[] users = authentication.GetUsers();
                StringJoiner active = new StringJoiner(", ");
                StringJoiner inactive = new StringJoiner(", ");
                
                synchronized(mapLock){
                    for(String u : users){
                        if(Name_Client_Map.containsKey(u)) active.add(u);
                        else inactive.add(u);
                    }
                }

                PrintWriter out = new PrintWriter(sender.getOutputStream(), true);
                out.println("/users" + regex + active.toString() + regex + inactive.toString());
                break;
        
            default:

                // Let the clients format their own broadcast messages
                synchronized(mapLock){
                    for(Socket k : Client_Name_Map.keySet()){
                        PrintWriter bOut = new PrintWriter(k.getOutputStream(), true);
                        bOut.println("/broadcast" + regex + senderName + regex + message);
                    }
                }
                break;
        }
    }

    public static void main(String[] args) throws IOException{
        Server server = new Server();
        server.DispatchClientThreads(server);
    }
}