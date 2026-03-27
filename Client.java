import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Client {
    
    private static BufferedReader in;
    private static PrintWriter out;
    private static final String regex = "$#@&76!:;";

    private static final String serverAddress = "localhost";
    private static final int port = 5000;
    private static Map<String,String> user_Color_map;

    // A helper array of vibrant colors to assign to users
    private static final String[] UI_COLORS = {
        Colors.CYAN, Colors.GREEN, Colors.YELLOW, Colors.BLUE, Colors.PURPLE
    };

    public static void main(String[] args) throws IOException{

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        println(Colors.CYAN + "=== Welcome to JavaChat ===" + Colors.RESET);
        System.out.print(Colors.BLUE + "Enter your name: " + Colors.RESET);
        String nameInput = br.readLine();
        if(nameInput == null || nameInput.trim().equals("")){
            println(Colors.RED + "No name inserted!" + Colors.RESET);
            return;
        }

        System.out.print(Colors.BRIGHT_RED + "Enter password: " + Colors.RESET);
        String passwordInput = br.readLine();
        if(passwordInput == null || passwordInput.trim().equals("")){
            println(Colors.RED + "No password inserted!" + Colors.RESET);
            return;
        }

        InetAddress hostAddr = InetAddress.getByName(serverAddress);
        Socket socket = new Socket(hostAddr,port);
        
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);
        
        out.println(nameInput + ":" + passwordInput);
        
        String recieved = in.readLine();
        println(Colors.YELLOW + "[System] " + recieved + Colors.RESET);

        if(recieved.startsWith("Error")){
            socket.close();
            return;
        }

        user_Color_map = new HashMap<>();

        SendMessage();
        RecieveMessage();
    }

    private static void SendMessage(){
        Thread thread = new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while(true){
                try{
                    String line = br.readLine();
                    SendToServer(AddRegex(line));
                }
                catch(Exception e){
                    println(Colors.RED + "Invalid input!" + Colors.RESET);
                }
            }
        });
        thread.start();
    }

    private static String AddRegex(String line){
        if(line.startsWith("/msg ")){
            String[] tokens = line.split(" ", 3);
            if(tokens.length == 3){
                return tokens[0] + regex + tokens[1] + regex + tokens[2];
            }
        }
        return line;
    }

    private static void RecieveMessage(){
        Thread thread = new Thread(() -> {
            out.println("/users");

            while(true){
                try{
                    String message = in.readLine();
                    if (message == null) {
                        println(Colors.RED + "\n[System] Server closed connection." + Colors.RESET);
                        System.exit(0);
                    }
                    ParseStringRecieved(message);
                }
                catch(Exception e){
                    println(Colors.RED + "[System] Disconnected." + Colors.RESET);
                    System.exit(0);
                }
            }
        });
        thread.start();
    }

    // Helper to get a clean [HH:mm:ss] timestamp
    private static String getTime() {
        return new SimpleDateFormat("HH:mm:ss").format(new Date());
    }

    // Helper to consistently assign a color to a user based on their name
    private static String getUserColor(String username) {
        if (!user_Color_map.containsKey(username)) {
            int colorIndex = Math.abs(username.hashCode()) % UI_COLORS.length;
            user_Color_map.put(username, UI_COLORS[colorIndex]);
        }
        return user_Color_map.get(username);
    }

    private static void ParseStringRecieved(String line){
        // FIXED: Using Pattern.quote() to safely read the server's tokens
        String[] tokens = line.trim().split(Pattern.quote(regex));
        
        // This is the clean, grey timestamp that goes before EVERY message
        String timePrefix = Colors.GRAY + "[" + getTime() + "] " + Colors.RESET;
        
        if (tokens[0].equals("/msg") && tokens.length >= 3) {
            String sender = tokens[1];
            String msgBody = tokens[2];
            String c = getUserColor(sender);
            
            println(timePrefix + Colors.PURPLE + "[private] " + c + sender + ": " + Colors.RESET + msgBody);
        }
        else if (tokens[0].equals("/broadcast") && tokens.length >= 3) {
            String sender = tokens[1];
            String msgBody = tokens[2];
            String c = getUserColor(sender);
            
            println(timePrefix + c + sender + ": " + Colors.RESET + msgBody);
        }
        else if(tokens[0].equals("/users")){
            String active = tokens.length > 1 && !tokens[1].isEmpty() ? tokens[1] : "None";
            String inactive = tokens.length > 2 && !tokens[2].isEmpty() ? tokens[2] : "None";
            
            println(""); // Spacing
            println(timePrefix + Colors.YELLOW + "--- Current Users ---" + Colors.RESET);
            println("         Online : " + Colors.GREEN + active + Colors.RESET);
            println("         Offline: " + Colors.GRAY + inactive + Colors.RESET);
            println(Colors.YELLOW + "--------------------------" + Colors.RESET);
        }
        else if (tokens[0].equals("/system") && tokens.length >= 2) {
            println(timePrefix + Colors.RED + "[System] " + tokens[1] + Colors.RESET);
        }
        else {
            // Failsafe for unexpected raw messages
            println(timePrefix + line);
        }
    }

    private static void SendToServer(String line) {
        out.println(line);
    }

    private static void println(String line){
        System.out.println(line);
    }
}