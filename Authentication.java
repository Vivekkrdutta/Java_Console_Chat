import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class Authentication {
    
    private Properties credentials;
    private static final String fileName = "credentials.properties";
    private Object lockObject = new Object();

    public  enum VerificationResult{

        Verified,
        NewRegistered,
        Error,
    }

    public Authentication() throws IOException {
        credentials = new Properties();
        File file = new File(fileName);
        
        // Only try to load if the file actually exists
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                credentials.load(in);
            }
        } else {
            file.createNewFile(); // Create it empty if it's missing
        }
    }

    private boolean TryAddField(String username, String password) {
        if (!credentials.containsKey(username)) {
            credentials.put(username, password);
            
            // OPEN, SAVE, and CLOSE right here:
            try (FileOutputStream out = new FileOutputStream(fileName)) {
                credentials.store(out, "Updated credentials");
                return true;
            } catch (IOException e) {
                System.err.println("Could not save to file: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public VerificationResult Verify(String username, String password) throws IOException{

        synchronized(lockObject){

            if(credentials.containsKey(username) && credentials.get(username).equals(password)){

                return VerificationResult.Verified;
            }

            // add new
            if(TryAddField(username, password)){

                return VerificationResult.NewRegistered;
            }
        }

        return VerificationResult.Error;
    }

    public String[] GetUsers(){

        synchronized(lockObject){

            String[] result = new String[credentials.size()];
            int i = 0;
            for(var key : credentials.keySet()){

                result[i++] = (String) key;
            }
            return result;
        }
    }
}
