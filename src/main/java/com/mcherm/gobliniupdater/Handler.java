package com.mcherm.gobliniupdater;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.File;

import com.amazonaws.services.sqs.model.*;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.google.gson.Gson;


public class Handler implements RequestHandler<ScheduledEvent, String>{
    final AmazonSQS sqs = AmazonSQSClientBuilder.defaultClient();
    final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    final static String QUEUE_NAME = "goblini-updates";
    final static String BUCKET_NAME = "goblini-public";



    HashMap<String, GuesserData> guessers = new HashMap<>();


    @Override
    public String handleRequest(ScheduledEvent event, Context context)
    {
        String queueUrl = sqs.getQueueUrl(QUEUE_NAME).getQueueUrl();
        LambdaLogger logger = context.getLogger();
        String response = "200 OK";
        // log execution details
        // process event
        logger.log("The handler that Rachel wrote is running.");
        // I officialy give up on retrieving attributes, I can see the attributes in the queue I can't seem
        //fetch them
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl).withMessageAttributeNames("guesserType","updateType").withMaxNumberOfMessages(10);
        //ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl);
        ReceiveMessageResult result = sqs.receiveMessage(request);

        List<Message> messages = result.getMessages();
        while (messages.size() >0){
            for (Message message: messages){
                logger.log ("Message" + message);
                String body= message.getBody();

                //DID i MENTION THAT I OFFICIALLY GIVE UP ON ATTRIBUTES????
                Map<String, MessageAttributeValue> attributes = message.getMessageAttributes();
                logger.log ("message attributes " + attributes);
    //            Map<String, String> att = message.getAttributes();
    //            logger.log ("stringtostring " +att);
    //            MessageAttributeValue value = attributes.get("guesserType");

                Gson gson = new Gson();
                UpdaterData update = gson.fromJson(body, UpdaterData.class);
                String guesserType = update.guesserType;
                DeleteMessageResult deleteResult =sqs.deleteMessage(queueUrl, message.getReceiptHandle());
                logger.log ("sdkresponse:" +deleteResult.getSdkResponseMetadata().getRequestId());
                logger.log ("delete http status code:" + deleteResult.getSdkHttpMetadata().getHttpStatusCode());
                GuesserData guesser= getGuesser(guesserType, logger);
                processUpdate(guesser, update, logger);
            }
            logger.log ("rechecking queue");
            result = sqs.receiveMessage(request);
            messages = result.getMessages();
        }
        //write updated models each to its own file
        for (String guesserType: guessers.keySet()){
            writeFile(guesserType,logger, guessers.get(guesserType));
        }
        return response;
    }

    void processUpdate(GuesserData guesser, UpdaterData update, LambdaLogger logger){
        for (UpdaterResponse response: update.responseHistory){
            Question question;

            //get the question
           //check to see if question is new
            if (response.qID < 0){
                Integer newQuestionID = guesser.questionLookups.get(response.q);
                if (newQuestionID == null) {
                    //create a new question and put it in the questions list, and question lookup
                    question = new Question(response.q);
                    newQuestionID = guesser.questions.size();
                    guesser.questions.add(question);
                    guesser.questionLookups.put(response.q, newQuestionID);
                    logger.log ("created new question \n" + "  question:" +question.question + "\n  q ID:" + newQuestionID);
                } else {
                    question = guesser.questions.get(newQuestionID);
                }
            } else {
                question = guesser.questions.get(response.qID);
                if (question == null){
                    logger.log("Error in processUpdate, Line 99, positive qID:"+response.qID+"not found");
                    return;
                }
            }

            //check to see if guess is new and if it is create new guess
            Short guessId = response.gID;
            if (guessId<0){
                //check to see if guess is a valid guess, if its not a valid guess exits and does not process
                //this Update
                if (!Guess.isValidGuessName(response.g)){
                    logger.log ("Received invalid guess id:" + guessId + "  name:" + response.g);
                    return;
                }
                guessId = guesser.guessLookups.get(response.g);
                if (guessId == null){
                    //create a new guessId & guess and put in the guess list, and guess lookup
                    guessId = (short)guesser.guessArray.size();
                    Guess newGuess = new Guess(guessId,response.g);
                    guesser.guessArray.add(newGuess);
                    guesser.guessLookups.put(newGuess.name, newGuess.guessId);
                    logger.log ("Created new guess:" + newGuess);
                }
            }
            //add the response to the question data
            question.add(guessId, response.a);
            logger.log ("Added to question:" + question.question +" guessId:" + guessId + "response:" + response.a);
        }
    }

    GuesserData getGuesser(String guesserType, LambdaLogger logger){
        GuesserData guesser = guessers.get(guesserType);
        if (guesser == null){
            guesser = (readFile(guesserType, logger));
            guesser.createLookups();
            guessers.put(guesserType, guesser);
        }
        return guesser;
    }

    GuesserData readFile(String guesserType, LambdaLogger logger){
        try {
            S3Object o = s3.getObject(BUCKET_NAME, "guessers/" +guesserType +"Guesser.json");
            S3ObjectInputStream s3is = o.getObjectContent();
            Gson gson = new Gson();
            InputStreamReader isw = new InputStreamReader(s3is);
            GuesserData guesser = gson.fromJson(isw, GuesserData.class);
            logger.log("Starting instruction" + guesser.startingInstruction);
            return guesser;
        } catch  (Exception err){
            logger.log(err.toString());
            throw err;
        }
    }

    void writeFile(String guesserType, LambdaLogger logger, GuesserData data) {
        try {
            Gson gson = new Gson();
            String contents = gson.toJson(data);
            s3.putObject(BUCKET_NAME, "guessers/" + guesserType + "Guesser.json", contents);
        }catch  (Exception err){
            logger.log(err.toString());
            throw err;
        }
    }
}