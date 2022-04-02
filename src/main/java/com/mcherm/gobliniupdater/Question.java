package com.mcherm.gobliniupdater;


import java.util.*;
import java.util.HashMap;


public class Question{

    public String question;
    public final static float GOOD_PERCENT = 80;  // if more than GOOD_PERCENT percent of the responses are a yes or no it goes in the yes or no set else it goes in the maybeGuesses

    public HashSet<Short> yesGuesses;
    public HashSet<Short> noGuesses;
    public HashSet<Short> maybeGuesses;

    HashMap<Short, Integer> numYeses;  //for each guess how many times a user has entered yes for this question
    HashMap<Short, Integer> numNos;  //for each guess how many times a user has entered no for this question
    HashMap<Short, Integer> numResponses;  //total number of times a user has entered a response to this question either yes, no, or unclear

    boolean verified;  //has question been verified

    Question (String question){
        this.question = question;

        yesGuesses = new HashSet<Short>();
        noGuesses = new HashSet<Short>();
        maybeGuesses = new HashSet<Short>();

        numYeses = new HashMap<Short, Integer>();
        numNos = new HashMap<Short, Integer>();
        numResponses = new HashMap<Short, Integer>();
        verified = false;
    }


    Question (String question, HashSet<Short> yesGuesses, HashSet<Short> noGuesses, HashSet<Short> maybeGuesses){
        this(question);
        this.yesGuesses.addAll(yesGuesses);
        this.noGuesses.addAll(noGuesses);
        this.maybeGuesses.addAll(maybeGuesses);
        addToCount(yesGuesses,numYeses);
        addToCount(noGuesses,numNos);
        addToCount(yesGuesses,numResponses);
        addToCount(noGuesses, numResponses);
        addToCount(maybeGuesses, numResponses);
        verified = false;

    }

    public int numGuessesKnown(){
        return (yesGuesses.size()+noGuesses.size()+maybeGuesses.size());
    }
    public void addToCount(HashSet<Short> keys, HashMap<Short, Integer> counts){
        for (Short key: keys){
            Integer count = counts.getOrDefault(key, 0);
            count++;
            counts.put(key,count);
        }
    }

    public String toString(){
        return ("Question: " + question + " \\n yesGuesses:" + yesGuesses + "//n noGuesses:" +noGuesses +maybeGuesses);
    }


    public boolean equals(Question q){
        if (this.question.equals(q.question) ){
            return true;
        } else {
            return false;
        }

    }

    public int hashCode(){
        return question.hashCode();
    }

    //takes a guess and and an answer, and updates counts
    //answer must be "y","n","m"
    public void add(Short guess, String answer){
        // System.out.println ("In add numYeses:" +numYeses);
        // System.out.println ("guess " + guess);
        // System.out.println ("answer " + answer);
        // System.out.println ("question:" + question);
        Integer yeses = numYeses.getOrDefault(guess, 0);
        Integer nos = numNos.getOrDefault(guess,0);
        Integer total = numResponses.getOrDefault(guess,0);

        if (answer.equals("y")){
            yeses++;
            numYeses.put(guess,yeses);
        }  else if (answer.equals("n")){
            nos++;
            numNos.put(guess,nos);
        } else if (!answer.equals("m")){
            System.out.println ("Invalid input to Question.add");
            return;
        }
        total++;
        numResponses.put(guess,total);

        updateGuessesSets(guess);

    }
    //takes a guess and and an answer, and updates counts
    //answer must be "y","n","m"
    public void add(Short guess, Answer answer){
        // System.out.println ("In add numYeses:" +numYeses);
        // System.out.println ("guess " + guess);
        // System.out.println ("answer " + answer);
        // System.out.println ("question:" + question);
        Integer yeses = numYeses.getOrDefault(guess, 0);
        Integer nos = numNos.getOrDefault(guess,0);
        Integer total = numResponses.getOrDefault(guess,0);

        if (answer == Answer.YES){
            yeses++;
            numYeses.put(guess,yeses);
        }  else if (answer== Answer.NO){
            nos++;
            numNos.put(guess,nos);
        }
        total++;
        numResponses.put(guess,total);

        updateGuessesSets(guess);
    }


    /**
     * Call this to completely replace the counts of yeses, nos, and responses for a
     * specific guess with this question.
     *
     * @param guessId the guess for which counts should be updated
     * @param yeses the new number of yeses
     * @param nos the new number of nos
     * @param responses the new number of responses
     */
    public void setGuessCounts(Short guessId, int yeses, int nos, int responses) {
        numYeses.put(guessId, yeses);
        numNos.put(guessId, nos);
        numResponses.put(guessId, responses);
        updateGuessesSets(guessId);
    }

    /**
     * Call this when the counts for a specific guess have been updated in order
     * to update the guess sets.
     *
     * @param guessId the guess for which sets should be updated
     */
    protected void updateGuessesSets(Short guessId) {
        final int yeses = numYeses.getOrDefault(guessId, 0);
        final int nos = numNos.getOrDefault(guessId, 0);
        final int responses = numResponses.getOrDefault(guessId, 0);

        if (responses > 0) {
            float yesPercent = ((float) yeses / (float) responses) * 100;
            float noPercent = ((float) nos / (float) responses) * 100;

            if (yesPercent > GOOD_PERCENT){
                yesGuesses.add(guessId);
                noGuesses.remove(guessId);
                maybeGuesses.remove(guessId);
            } else if (noPercent > GOOD_PERCENT){
                noGuesses.add(guessId);
                yesGuesses.remove(guessId);
                maybeGuesses.remove(guessId);
            } else {
                maybeGuesses.add(guessId);
                noGuesses.remove(guessId);
                yesGuesses.remove(guessId);
            }
        }
    }

}

