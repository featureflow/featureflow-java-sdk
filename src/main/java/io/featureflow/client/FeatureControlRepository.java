package io.featureflow.client;
import java.io.Closeable;
import java.util.Map;
/**
 * Created by oliver.oldfieldhodge on 25/07/2015.
 * This is the repository that will hold the runtime state for features, it gets the feature from the server or local dev file
 */
public interface FeatureControlRepository extends Closeable{

    void init(Map<String, FeatureControl> featureControls);
    FeatureControl get(String key);
    Map<String, FeatureControl> getAll();
    void update(String key, FeatureControl featureControl);


    /*private Map<String, FeatureControl> featureControlMap = new ConcurrentHashMap<String, FeatureControl>();
    AsyncRestTemplate restTemplate = new AsyncRestTemplate();
    private String apiKey;

    public FeatureControlRepository(String apiKey) {
            this.apiKey = apiKey;
    }

    public FeatureControl getFeatureControl(String featureId) {
        //get the feature state from  local or from the server
        FeatureControl state = featureControlMap.get(featureId);
        return state;
    }


    public void initialiseRepository(){
        //call featureflow and as for the FeatureToggles for this project
        System.out.println("Initialising Repository");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(new LinkedMultiValueMap(), headers);
        ListenableFuture<ResponseEntity<List<FeatureControl>>> stateBundleFuture = restTemplate.exchange("http://localhost:8080/api/featureConfigurations", HttpMethod.GET, request ,new ParameterizedTypeReference<List<FeatureControl>>(){});
        try {
            ResponseEntity<List<FeatureControl>> response = stateBundleFuture.get();
            System.out.println(response.getBody());
            List<FeatureControl> b = response.getBody();
            for (FeatureControl s : b) {
                featureControlMap.put(s.getFeature().getCode(), s);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        log.info("Repository Initialised with {} feature controls", featureControlMap.size());

        //set up a websocket connection and listeners for updates
        //todo - this into agent and then propagation (eureka or zookeeper style logic)
    }*/
}
