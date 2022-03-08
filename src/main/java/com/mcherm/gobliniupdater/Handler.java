package com.mcherm.gobliniupdater;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;


public class Handler implements RequestHandler<ScheduledEvent, String>{
    @Override
    public String handleRequest(ScheduledEvent event, Context context)
    {
        LambdaLogger logger = context.getLogger();
        String response = "200 OK";
        // log execution details
        // process event
        logger.log("The handler that Rachel wrote is running.");
        return response;
    }
}