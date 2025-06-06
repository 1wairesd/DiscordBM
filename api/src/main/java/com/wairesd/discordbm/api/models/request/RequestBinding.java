package com.wairesd.discordbm.api.models.request;

import java.util.Map;

public interface RequestBinding {
    String getCommandName();
    String getRequestType();
    Map<String, String> getRequestData(String[] params);
    String buildButtonCustomId(String serverName, String[] params);
}