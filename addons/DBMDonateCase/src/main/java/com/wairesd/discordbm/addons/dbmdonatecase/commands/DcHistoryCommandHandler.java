package com.wairesd.discordbm.addons.dbmdonatecase.commands;

import com.jodexindustries.donatecase.api.DCAPI;
import com.jodexindustries.donatecase.api.data.casedata.CaseData;
import com.wairesd.discordbm.api.DBMAPI;
import com.wairesd.discordbm.api.command.CommandHandler;
import com.wairesd.discordbm.api.message.MessageSender;
import com.wairesd.discordbm.addons.dbmdonatecase.configurators.Messages;
import com.wairesd.discordbm.api.message.ResponseType;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

public class DcHistoryCommandHandler implements CommandHandler {
    private final DCAPI api;
    private final DBMAPI dbmApi;
    private final Messages messages;

    public DcHistoryCommandHandler(DCAPI api, DBMAPI dbmApi, Messages messages) {
        this.api = api;
        this.dbmApi = dbmApi;
        this.messages = messages;
    }

    @Override
    public void handleCommand(String command, Map<String, String> opts, String reqId) {
        dbmApi.setResponseType(ResponseType.REPLY);
        try {
            MessageSender sender = dbmApi.getMessageSender();
            String player = opts.getOrDefault("player", "");
            if (player.isEmpty()) {
                sender.sendResponse(reqId, messages.get("dcstats_no_player"));
                return;
            }
            api.getDatabase().getHistoryData().thenAccept(historyList -> {
                if (historyList == null || historyList.isEmpty()) {
                    sender.sendResponse(reqId, messages.get("dchistory_no_data"));
                    dbmApi.clearResponseType();
                    return;
                }
                SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (CaseData.History h : historyList.stream()
                        .filter(h -> h.playerName().equalsIgnoreCase(player))
                        .sorted(Comparator.comparingLong((CaseData.History h) -> h.time()).reversed())
                        .limit(10)
                        .toList()) {
                    String date = sdf.format(new Date(h.time()));
                    String displayName = api.getCaseManager().getByType(h.caseType())
                        .map(def -> def.settings().displayName())
                        .orElse(h.caseType());
                    sb.append("• ")
                      .append(h.item()).append(" (кейс: ")
                      .append(displayName).append(", ")
                      .append(date).append(")\n");
                    count++;
                }
                if (count == 0) {
                    sender.sendResponse(reqId, messages.get("dchistory_no_data"));
                    dbmApi.clearResponseType();
                    return;
                }
                var embed = dbmApi.createEmbedBuilder()
                    .setTitle(messages.get("dchistory_title").replace("%player%", player))
                    .setDescription(sb.toString())
                    .build();
                sender.sendResponse(reqId, embed);
                dbmApi.clearResponseType();
            });
        } catch (Exception e) {
            dbmApi.clearResponseType();
            throw e;
        }
    }
} 