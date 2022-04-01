package com.mcherm.gobliniupdater;

import java.util.List;


/**
 * This is the bind object for messages of the format STRUCTURED_DATA_UPDATE.
 *
 * <pre>
 * STRUCTURED_DATA_UPDATE is a JSON format used for sending updates. It always consists
 * of a JSON object with five fields:
 *   "version" - if using this documentation, this field always has the value 1.
 *   "guesserType" - this field always contains a JSON string with the identifier of a particular
 *           guesser to be updated.
 *   "questions" - this field always contains a JSON list of zero or more question updates.
 *       question update: this is a JSON object which has 3 possible fields. No two entries will have
 *               the same qID.
 *           "qID" - this is a JSON number identifying the question. If it is a non-negative number
 *               then it refers to an existing question (which will be updated). If it is a
 *               negative number then this is a new question (which will be assigned some
 *               positive number as a question ID). Each qID in the list will be unique.
 *           "question" - this is a JSON string containing the text of the question. It must
 *               always be present if qID is negative; if qID is non-negative and this is omitted
 *               then that means to leave the question string unchanged.
 *           "verified" - this is a JSON boolean which indicates whether the question should
 *               be marked as verified or not. If this is omitted then for non-negative qID it means
 *               to leave the verification status as-is; for negative qIDs if it is omitted then
 *               the verified status defaults to False.
 *   "guesses" - this field always contains a JSON list of zero or more guess updates. No two entries
 *           will have the same gID.
 *       guess update: this is a JSON object which has 3 possible fields:
 *           "gID" - this is a JSON number identifying the guess. If it is a non-negative number
 *               then it refers to an existing guess (which will be updated). If it is a
 *               negative number then this is a new guess (which will be assigned some
 *               positive number as a guess ID). Each gID in the list will be unique.
 *           "guess" - this is a JSON string containing the text of the guess. It must
 *               always be present if gID is negative; if gID is non-negative and this is omitted
 *               then that means to leave the guess string unchanged.
 *           "verified" - this is a JSON boolean which indicates whether the guess should
 *               be marked as verified or not. If this is omitted then for non-negative gID it means
 *               to leave the verification status as-is; for negative gIDs if it is omitted then
 *               the verified status defaults to False.
 *   "answers" - this field always contains a JSON list of answer updates.
 *       answer update: this is a JSON object which has four possible fields. No two entries will
 *               have the same qID and gID.:
 *           "qID" - this is the qID of a question. If negative, it must match a negative qID used
 *               in the "questions" list above. This field will always be present.
 *           "gID" - this is the gID of a guess. If negative, it must match a negative gID used
 *               in the "guesses" list above. This field will always be present.
 *           "counts" - this contains a JSON list of three non-negative integers: the new values for the
 *               counts of "yes", "no", and "maybe" responses (in that order). Either this field or the
 *               "increments" field will be present, but not both.
 *           "increments" - this contains a JSON list of three non-negative integers: the amounts to add
 *               to the current counts of "yes", "no", and "maybe" responses (in that order). Either
 *               this field or the "counts" field will be present, but not both.
 * </pre>
 */
public class StructuredDataUpdate {
    public int version;
    public String guesserType;
    public List<QuestionUpdate> questions;
    public List<GuessUpdate> guesses;
    public List<AnswerUpdate> answers;


    /**
     * Bind object for a question update.
     */
    public static class QuestionUpdate {
        public short qID;
        public String question;
        public Boolean verified;
    }

    /**
     * Bind object for a question update.
     */
    public static class GuessUpdate {
        public short gID;
        public String guess;
        public Boolean verified;
    }

    /**
     * Bind object for a question update.
     */
    public static class AnswerUpdate {
        public short qID;
        public short gID;
        public List<Integer> counts;
        public List<Integer> increments;
    }

}
