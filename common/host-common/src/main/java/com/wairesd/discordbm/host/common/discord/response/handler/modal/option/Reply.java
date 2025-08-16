package com.wairesd.discordbm.host.common.discord.response.handler.modal.option;

import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.config.configurators.Settings;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.response.ResponseHandler;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class Reply {
    public static DiscordBotListener listener;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));

    public Reply(DiscordBotListener listener) {
        this.listener = listener;
    }

    public static void handleReplyModal(UUID requestId, ResponseMessage respMsg) {
        var event = listener.getRequestSender().getPendingRequests().remove(requestId);
        if (event != null) {
            boolean ephemeral = respMsg.flags() != null && respMsg.flags().isEphemeral();
            if (respMsg.response() != null && !respMsg.response().isEmpty()) {
                event.getHook().sendMessage(respMsg.response()).setEphemeral(ephemeral).queue(
                        success -> {
                            if (Settings.isDebugRequestProcessing()) {
                                logger.info("Reply sent for REPLY_MODAL, now sending form for requestId: {}", requestId);
                            }
                            ResponseHandler.handleModalResponse(requestId, respMsg);
                        },
                        failure -> logger.error("Failed to send reply for REPLY_MODAL: {}", failure.getMessage())
                );
            } else {
                ResponseHandler.handleModalResponse(requestId, respMsg);
            }
        } else {
            logger.error("No event found for REPLY_MODAL requestId: {}", requestId);
        }
    }
}
