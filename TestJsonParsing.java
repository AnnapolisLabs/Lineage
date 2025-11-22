import com.annapolislabs.lineage.dto.request.ImportProjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJsonParsing {
    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            
            String json = new String(Files.readAllBytes(Paths.get("frontend/requirements.json")));
            
            System.out.println("JSON length: " + json.length());
            System.out.println("First 200 chars: " + json.substring(0, Math.min(200, json.length())));
            
            ImportProjectRequest request = mapper.readValue(json, ImportProjectRequest.class);
            
            System.out.println("SUCCESS! Parsed project: " + request.getProject().getName());
            System.out.println("Requirements count: " + (request.getRequirements() != null ? request.getRequirements().size() : 0));
            
        } catch (Exception ex) {
            System.out.println("FAILED! Error type: " + ex.getClass().getSimpleName());
            System.out.println("Error message: " + ex.getMessage());
            if (ex.getCause() != null) {
                System.out.println("Root cause: " + ex.getCause().getMessage());
            }
            ex.printStackTrace();
        }
    }
}