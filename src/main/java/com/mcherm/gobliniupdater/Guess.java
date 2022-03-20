package com.mcherm.gobliniupdater;

public class Guess implements Comparable<Guess>{
    public short guessId;
    public String name;
    public boolean verified;

    public Guess(short guessId, String name){
        this.guessId = guessId;
        this.name = name;
        verified = false;
    }

    void verify(){
        verified = true;
    }

    public String toString(){
        return name;
    }

    public int compareTo(Guess guess){
        return (this.name.compareTo(guess.name));
    }


    //so here I have to figure out what I will accept
    //right now it allows only a-z, 0-9, and spaces
    //I may want to rule out "bad words"  guess I would need a bad word dictionary
    public static boolean isValidGuessName(String guessName){
        if (guessName.length() >2 && guessName.matches("^[a-z0-9 ]*$")) {
            return true;
        }
        return false;
    }
}
