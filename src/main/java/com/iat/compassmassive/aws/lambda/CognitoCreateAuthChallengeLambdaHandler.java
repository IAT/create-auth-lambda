package com.iat.compassmassive.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.*;

import java.util.*;

public class CognitoCreateAuthChallengeLambdaHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    public ObjectMapper mapper = new ObjectMapper();
    private static final String POINTS_TEST_PROXY = System.getenv("points_test_proxy");

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> event, Context context) {
        System.out.println("CREATE AUTH LAMBDA TRIGGERED");
        LambdaLogger log = context.getLogger();
        System.out.println("Initial event to be returned from Lambda for create auth is : " + event.get("request"));
        System.out.println("Initial event to be returned from Lambda for create auth is : " + event);

        log.log("Initial event to be returned from Lambda is : " + event);
        Map<String, Object> request = (Map<String, Object>) event.get("request");
        Map<String, Object> response = (Map<String, Object>) event.get("response");
        List<Object> session = (List<Object>) request.get("session");
        Map<String, Object> userAttributes = (Map<String, Object>) request.get("userAttributes");


        int sessionLength = mapper.convertValue(session, ArrayList.class).size();
        String code = null;
        System.out.println("Before checking session length:");
        log.log("Before checking session length:");
        if (sessionLength == 0) {
//            code = (String) request.get("code");
            String ct = String.valueOf(new Date().getTime());

            int l = ct.length();
            code = ct.substring(l - 4);
            // send email with authentication code
            try {
                log.log("Sending mail:" + code);
//                String authenticationCode = AES.decrypt(code);
                System.out.println("decryped code:" + code);
                sendMagicLinkEmail(userAttributes.get("email").toString(), code);
            } catch (Exception e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        } else {
            Map<String, Object> previousChallenge = (Map<String, Object>) session.get(sessionLength - 1);
            code = String.valueOf(previousChallenge.get("challengeMetadata").toString().matches("CODE-" + "(\\d*)"));
        }
        Map<String, String> privateChallengeParameters = Collections.singletonMap("ANSWER", code);
        Map<String, String> publicChallengeParameters = Collections.singletonMap("email", userAttributes.get("email").toString());
        response.put("privateChallengeParameters", privateChallengeParameters);
        response.put("publicChallengeParameters", publicChallengeParameters);
        response.put("challengeMetadata", "CODE-" + code);
        System.out.println("response:"+event);
        return event;
    }


    private boolean sendMagicLinkEmail(String email, String verificationCode) {

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        EmailSender sender = new EmailSender();
        sender.setEmail(email);
        sender.setVerificationCode(verificationCode);
        RequestBody body = null;
        try {
            body = RequestBody.create(mediaType, mapper.writeValueAsString(sender));
            Request clientRequest = new Request.Builder()
                    .url(POINTS_TEST_PROXY + "/user/send-link")
                    .method("POST", body)
                    .addHeader("Content-Type", "application/json")
                    .build();
            System.out.println("Client request:" + clientRequest);
            Response response = client.newCall(clientRequest).execute();
            System.out.println("Response code:" + response.code());
            if (response.code() == 200) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
