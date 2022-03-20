package com.mcherm.gobliniupdater;

import java.util.ArrayList;

public class UpdaterData {
    String guesserType;
    ArrayList<UpdaterResponse> responseHistory;

    public UpdaterData(String guesserType, ArrayList<UpdaterResponse> responseHistory) {
        this.guesserType = guesserType;
        this.responseHistory = responseHistory;
    }
}

