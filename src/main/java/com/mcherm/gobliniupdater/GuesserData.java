package com.mcherm.gobliniupdater;

import java.util.ArrayList;
import java.util.HashMap;

public class GuesserData{
    public String startingInstruction;
    public ArrayList<Question> questions;
    public ArrayList<Guess> guessArray;
    transient public HashMap<String, Short> guessLookups;
    transient public HashMap<String, Integer> questionLookups;

    public void createLookups () {
        guessLookups = new HashMap<String, Short>();
        questionLookups = new HashMap<String, Integer>();
        int i =0;
        for (Question question: questions){
            questionLookups.put(question.question,i);
            i++;
        }
        short s =0;
        for (Guess guess:guessArray) {
            guessLookups.put(guess.name, s);
            s++;
        }
    }

}


