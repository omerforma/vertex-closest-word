package test.project1;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import io.vertx.core.*;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.BodyHandler;

public class Server extends AbstractVerticle {

    private Router router;
    TreeMap<String,Integer> words;
    
    public Server() {
        this.words = new TreeMap<String,Integer>();
    }

    /**
     * handle requests to the right handler
     */
    @Override
    public void start(Future<Void> fut) throws Exception {
        router = Router.router(vertx);
        router.route("/api/words*").handler(BodyHandler.create());
        router.route("/api/words").handler(this::handleWord);
        vertx.createHttpServer().requestHandler(router::accept)
            .listen(
                config().getInteger("http.port", 8080),
                result -> {
                    if (result.succeeded()) {
                        fut.complete();
                    } else {
                        fut.fail(result.cause());
                    }
                });
    }
    /**
     * Response json object with the closest lexical and value word
     * @param routingContext
     */
    private void handleWord(RoutingContext routingContext) {
        JsonObject json = routingContext.getBodyAsJson();
        final String word = (String) json.getValue("text");
        int val = findWordValue(word);
        String closestLexical;
        String closestValue;
        //first word
        if(words.isEmpty()) {
            closestLexical = null;
            closestValue = null;
        }
        else {
            closestLexical = findClosestLexicalOrder(word);
            closestValue = findClosestValueOrder(val);
        }
        if(!words.containsKey(word)) {
            words.put(word, val);
        }
        JsonObject jsonAns = new JsonObject();
        jsonAns.put("value", closestValue);
        jsonAns.put("lexical", closestLexical);
        routingContext.response()
            .setStatusCode(201)
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encodePrettily(jsonAns));
      }
    
    /**
     * 
     * @param wordVal numeric value of the word
     * @return closest word by value
     */
    private String findClosestValueOrder(Integer wordVal) {
        int min = Integer.MAX_VALUE;
        String closest = null;
        for(Map.Entry<String,Integer> entry : words.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            final int diff = Math.abs(value - wordVal);
            if (diff < min) {
                min = diff;
                closest = key;
            }
        }
        return closest;     
    }
    
    /**
     * 
     * @param word the input word
     * @return closest word by lexical order
     */
    private String findClosestLexicalOrder(String word) {
        //get the 2 closest words
        Entry<String, Integer> low = words.floorEntry(word);
        Entry<String, Integer> high = words.ceilingEntry(word);
        
        if(low == null) {
            return high.getKey();
        }
        else if(high == null) {
            return low.getKey();
        }
        String lowKey = low.getKey();
        String highKey = high.getKey();
        //check which word is the closest
        for(int i=0; i<word.length(); i++) {
            if(highKey.length() < i) {
                return lowKey;
            }
            else if(lowKey.length() < i) {
                return highKey;
            }
            if(word.charAt(i) - lowKey.charAt(i) < word.charAt(i) - highKey.charAt(i)) {
                return lowKey;
            }
            else if(word.charAt(i) - lowKey.charAt(i) > word.charAt(i) - highKey.charAt(i)) {
                return highKey;
            }
        }
        //randomly chosen. there is 2 answers 
        return lowKey;
    }
    
    /**
     * 
     * @param word input word
     * @return numeric value of the word
     */
    private int findWordValue(String word) {
        Integer sum = 0;
        for(Character c : word.toCharArray()) {
            sum += (int) Character.toUpperCase(c) - (int) 'A' + 1;
        }
        return sum;
    }

}