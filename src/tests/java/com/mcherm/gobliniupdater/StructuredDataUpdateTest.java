package com.mcherm.gobliniupdater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import com.google.gson.Gson;

import java.util.Arrays;


/**
 * Tests for StructuredDataUpdate.
 */
public class StructuredDataUpdateTest {
    private Gson gson = new Gson();

    @Test
    void emptyFile() {
        String inputJson = "{'version':1,'guesserType':'animal','questions':[],'guesses':[],'answers':[]}";
        StructuredDataUpdate bind = gson.fromJson(inputJson, StructuredDataUpdate.class);
        Assertions.assertEquals(1, bind.version);
        Assertions.assertEquals("animal", bind.guesserType);
        Assertions.assertEquals(0, bind.questions.size());
        Assertions.assertEquals(0, bind.guesses.size());
        Assertions.assertEquals(0, bind.answers.size());
    }

    @Test
    void oneOfEach() {
        String inputJson =
                "{'version':1,'guesserType':'animal','questions':[" +
                "{'qID':3,'question':'Does it have hair?','verified':true}" +
                "],'guesses':[" +
                "{'gID':4,'guess':'dog','verified':false}" +
                "],'answers':[" +
                "{'qID':3,'gID':4,'counts':[6,1,2]}" +
                "]}";
        StructuredDataUpdate bind = gson.fromJson(inputJson, StructuredDataUpdate.class);
        Assertions.assertEquals(1, bind.version);
        Assertions.assertEquals("animal", bind.guesserType);
        Assertions.assertEquals(1, bind.questions.size());
        Assertions.assertEquals(3, bind.questions.get(0).qID);
        Assertions.assertEquals("Does it have hair?", bind.questions.get(0).question);
        Assertions.assertEquals(true, bind.questions.get(0).verified);
        Assertions.assertEquals(1, bind.guesses.size());
        Assertions.assertEquals(4, bind.guesses.get(0).gID);
        Assertions.assertEquals("dog", bind.guesses.get(0).guess);
        Assertions.assertEquals(false, bind.guesses.get(0).verified);
        Assertions.assertEquals(1, bind.answers.size());
        Assertions.assertEquals(3, bind.answers.get(0).qID);
        Assertions.assertEquals(4, bind.answers.get(0).gID);
        Assertions.assertEquals(Arrays.asList(6,1,2), bind.answers.get(0).counts);
        Assertions.assertEquals(null, bind.answers.get(0).increments);
    }
}