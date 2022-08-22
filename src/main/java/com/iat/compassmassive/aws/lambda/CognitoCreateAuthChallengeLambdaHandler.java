package com.iat.compassmassive.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;
import com.sun.xml.internal.org.jvnet.fastinfoset.EncodingAlgorithmException;

import java.util.*;

public class CognitoCreateAuthChallengeLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    public ObjectMapper mapper = new ObjectMapper();
    private static final String POINTS_TEST_PROXY = System.getenv("points_test_proxy");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        System.out.println("CREATE AUTH LAMBDA TRIGGERED");
        LambdaLogger log = context.getLogger();

        Map<String, Object> request = (Map<String, Object>) event.get("request");
        Map<String, Object> response = (Map<String, Object>) event.get("response");
        List<Object> session = (List<Object>) request.get("session");
        Map<String, Object> userAttributes = (Map<String, Object>) request.get("userAttributes");


        int sessionLength = mapper.convertValue(session, ArrayList.class).size();
        String encryptedCode = null;
        if (sessionLength == 0) {
            String ct = String.valueOf(new Date().getTime());

            int l = ct.length();
            encryptedCode = ct.substring(l - 4);
            try {
                encryptedCode =  AES.encrypt(encryptedCode).replaceAll("/", "");
            }
            catch (Exception exception) {
                throw new IllegalArgumentException("ERROR DURING ENCRYPTION: "+exception.getMessage());
            }

        } else {
            Map<String, Object> previousChallenge = (Map<String, Object>) session.get(sessionLength - 1);
            encryptedCode = String.valueOf(previousChallenge.get("challengeMetadata").toString().matches("CODE-" + "(\\d*)"));
        }
        Map<String, String> privateChallengeParameters = Collections.singletonMap("ANSWER", encryptedCode);
        Map<String, String> publicChallengeParameters = Collections.singletonMap("code", encryptedCode);
        response.put("privateChallengeParameters", privateChallengeParameters);
        response.put("publicChallengeParameters", publicChallengeParameters);
        response.put("challengeMetadata", "CODE-" + encryptedCode);
        return event;
    }



}
