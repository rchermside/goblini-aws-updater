package com.mcherm.gobliniupdater;

public class UpdaterResponse{
    int qID;//questionID
    String q;  //questionString only to be filled in if the question is new and no id has been assigned to it in which case the id is negative
    short gID; //guessID
    String g; // guessName only to be filled in if the question is new and no id has been assigned to it in which case the id is negative
    Answer a; //user's answer to this question and this guess

    public UpdaterResponse(int qID, String q, short gID, String g, Answer a) {
        this.qID = qID;
        this.q = q;
        this.gID = gID;
        this.g = g;
        this.a = a;
    }
}
