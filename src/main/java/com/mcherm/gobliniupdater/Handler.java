package com.mcherm.gobliniupdater;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.DeleteMessageResult;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;

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
        ReceiveMessageRequest request = new ReceiveMessageRequest(queueUrl).withMessageAttributeNames("guesserType","updateType").withMaxNumberOfMessages(10);
        ReceiveMessageResult result = sqs.receiveMessage(request);

        Gson gson = new Gson();
        List<Message> messages = result.getMessages();
        while (messages.size() >0){
            for (Message message: messages){
                logger.log ("Message" + message);
                String body= message.getBody();

                Map<String, MessageAttributeValue> attributes = message.getMessageAttributes();
                String guesserTypeAttr = attributes.get("guesserType").getStringValue();
                String updateTypeAttr = attributes.get("updateType").getStringValue();
                logger.log(
                    "Attributes on this message: guesserType='" + guesserTypeAttr +
                    "'; updateType ='" + updateTypeAttr + "'."
                );

                boolean processedSuccessfully;
                if (updateTypeAttr.equals("RESPONSES")) {
                    // Read the message
                    UpdaterData update = gson.fromJson(body, UpdaterData.class);
                    String guesserType = update.guesserType;
                    GuesserData guesser = getGuesser(guesserType, logger);

                    // Perform the update
                    processUpdate(guesser, update, logger);
                    processedSuccessfully = true;
                } else if (updateTypeAttr.equals("STRUCTURED_DATA_UPDATE")) {
                    // Read the message
                    StructuredDataUpdate update = gson.fromJson(body, StructuredDataUpdate.class);
                    String guesserType = guesserTypeAttr;
                    GuesserData guesser = getGuesser(guesserType, logger);

                    // Perform the update
                    try {
                        processUpdate(guesser, update, logger);
                        processedSuccessfully = true;
                    } catch(InvalidStructuredUpdate err) {
                        logger.log("Error processing update: " + err);
                        processedSuccessfully = false;
                    }
                } else {
                    logger.log("ERROR: updateTypeAttr of '" + updateTypeAttr + "' is not supported. Ignoring this update.");
                    processedSuccessfully = false;
                }

                if (!processedSuccessfully) {
                    logger.log("Error" + body);
                }

                // Whether successfully processed or not, go ahead and delete it from SQS
                DeleteMessageResult deleteResult = sqs.deleteMessage(queueUrl, message.getReceiptHandle());
                logger.log ("sdkresponse:" +deleteResult.getSdkResponseMetadata().getRequestId());
                logger.log ("delete http status code:" + deleteResult.getSdkHttpMetadata().getHttpStatusCode());
            }
            logger.log ("rechecking queue");
            result = sqs.receiveMessage(request);
            messages = result.getMessages();
        }
        //write updated models each to its own file
        for (String guesserType: guessers.keySet()){
            logger.log("Saving updates to guesser '" + guesserType + "'.");
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


    /**
     * This applies a set of updates to a guesser. If assumptions are violated, it raises a
     * InvalidStructuredUpdate exception.
     *
     * @param guesser the guesser to be updated
     * @param update the update to apply
     * @param logger a logger to write to for debugging
     * @throws InvalidStructuredUpdate
     */
    void processUpdate(GuesserData guesser, StructuredDataUpdate update, LambdaLogger logger)
        throws InvalidStructuredUpdate
    {
        final Gson gson = new Gson();

        // --- Verify top-level stuff ---
        if (update.version != 1) {
            throw new InvalidStructuredUpdate("Update version number must be 1.");
        }

        // --- Variables to store the true IDs of new questions and guesses ---
        final Map<Short,Short> newQuestionIds = new TreeMap<>(); // map from negative-placeholder to true id
        final Map<Short,Short> newGuessIds = new TreeMap<>(); // map from negative-placeholder to true id

        // --- Process the questions ---
        for (final StructuredDataUpdate.QuestionUpdate questionUpdate: update.questions) {
            logger.log("Applying update: " + gson.toJson(questionUpdate));
            if (questionUpdate.qID < 0) {
                // --- New Question ---
                final short newQuestionId = (short) guesser.questions.size();
                newQuestionIds.put(questionUpdate.qID, newQuestionId);
                if (questionUpdate.question == null) {
                    throw new InvalidStructuredUpdate("New question must have question text.");
                }
                final Question newQuestion = new Question(questionUpdate.question);
                if (questionUpdate.verified != null) {
                    newQuestion.verified = questionUpdate.verified;
                }
                guesser.questions.add(newQuestion);
            } else {
                // --- Updated Question ---
                if (questionUpdate.qID >= guesser.questions.size()) {
                    throw new InvalidStructuredUpdate("Question ID " + questionUpdate.qID + " does not exist.");
                }
                final Question question = guesser.questions.get(questionUpdate.qID);
                if (questionUpdate.question != null) {
                    question.question = questionUpdate.question;
                }
                if (questionUpdate.verified != null) {
                    question.verified = questionUpdate.verified;
                }
            }
        }

        // --- Process the guesses ---
        for (final StructuredDataUpdate.GuessUpdate guessUpdate: update.guesses) {
            logger.log("Applying update: " + gson.toJson(guessUpdate));
            if (guessUpdate.gID < 0) {
                // --- New Guess ---
                final short newGuessId = (short) guesser.guessArray.size();
                newGuessIds.put(guessUpdate.gID, newGuessId);
                if (guessUpdate.guess == null) {
                    throw new InvalidStructuredUpdate("New guess must have guess text.");
                }
                final Guess newGuess = new Guess(newGuessId, guessUpdate.guess);
                if (guessUpdate.verified != null) {
                    newGuess.verified = guessUpdate.verified;
                }
                guesser.guessArray.add(newGuess);
            } else {
                // --- Updated Guess ---
                if (guessUpdate.gID >= guesser.guessArray.size()) {
                    throw new InvalidStructuredUpdate("Guess ID " + guessUpdate.gID + " does not exist.");
                }
                final Guess guess = guesser.guessArray.get(guessUpdate.gID);
                if (guessUpdate.guess != null) {
                    guess.name = guessUpdate.guess;
                }
                if (guessUpdate.verified != null) {
                    guess.verified = guessUpdate.verified;
                }
            }
        }

        // --- Process the Answers ---
        for (final StructuredDataUpdate.AnswerUpdate answerUpdate: update.answers) {
            logger.log("Applying update: " + gson.toJson(answerUpdate));

            // --- Find the qID ---
            if (answerUpdate.qID >= guesser.questions.size()) {
                throw new InvalidStructuredUpdate("Answer has qID " + answerUpdate.qID + " that does not exist.");
            }
            final short qID;
            if (answerUpdate.qID < 0) {
                if (!newQuestionIds.containsKey(answerUpdate.qID)) {
                    throw new InvalidStructuredUpdate("Answer has qID " + answerUpdate.qID + " which was undefined.");
                }
                qID = newQuestionIds.get(answerUpdate.qID);
            } else {
                qID = answerUpdate.qID;
            }

            // --- Find the gID ---
            if (answerUpdate.gID >= guesser.guessArray.size()) {
                throw new InvalidStructuredUpdate("Answer has gID " + answerUpdate.gID + " that does not exist.");
            }
            final short gID;
            if (answerUpdate.gID < 0) {
                if (!newGuessIds.containsKey(answerUpdate.gID)) {
                    throw new InvalidStructuredUpdate("Answer has gID " + answerUpdate.gID + " which was undefined.");
                }
                gID = newGuessIds.get(answerUpdate.gID);
            } else {
                gID = answerUpdate.gID;
            }

            // --- Determine new counts of yeses, nos, and responses ---
            final int newNumYeses;
            final int newNumNos;
            final int newNumResponses;
            final Question question = guesser.questions.get(qID);
            if (answerUpdate.counts != null) {
                if (answerUpdate.counts.size() != 3) {
                    throw new InvalidStructuredUpdate("Answer has counts that isn't 3 items long.");
                }
                newNumYeses = answerUpdate.counts.get(0);
                newNumNos = answerUpdate.counts.get(1);
                newNumResponses = newNumYeses + newNumNos + answerUpdate.counts.get(2);
            } else {
                if (answerUpdate.increments == null) {
                    throw new InvalidStructuredUpdate("Answer must have counts or increments.");
                }
                if (answerUpdate.increments.size() != 3) {
                    throw new InvalidStructuredUpdate("Answer has increments that isn't 3 items long.");
                }
                final int oldNumYeses = question.numYeses.getOrDefault(gID, 0);
                final int oldNumNos = question.numNos.getOrDefault(gID, 0);
                final int oldNumResponses = question.numResponses.getOrDefault(gID, 0);
                final int incrYeses = answerUpdate.increments.get(0);
                final int incrNos = answerUpdate.increments.get(1);
                final int incrResponses = incrYeses + incrNos + answerUpdate.increments.get(2);
                newNumYeses = oldNumYeses + incrYeses;
                newNumNos = oldNumNos + incrNos;
                newNumResponses = oldNumResponses + incrResponses;
            }

            // --- Set new counts ---
            question.setGuessCounts(gID, newNumYeses, newNumNos, newNumResponses);
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


    /** An exception thrown if the structured update was invalid. */
    private static class InvalidStructuredUpdate extends Exception {
        public InvalidStructuredUpdate(String msg) {
            super(msg);
        }
    }
}