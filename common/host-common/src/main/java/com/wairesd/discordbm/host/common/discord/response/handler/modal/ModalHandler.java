package com.wairesd.discordbm.host.common.discord.response.handler.modal;

import com.wairesd.discordbm.common.models.response.ResponseMessage;
import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.host.common.discord.DiscordBotListener;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.option.Modal;
import com.wairesd.discordbm.host.common.discord.response.handler.modal.option.Reply;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ModalHandler {
    public static DiscordBotListener listener;
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBM"));

    public ModalHandler(DiscordBotListener listener) {
        this.listener = listener;
    }

    public static void handleReplyModal(UUID requestId, ResponseMessage respMsg) {
        Reply.handleReplyModal(requestId, respMsg);
    }

    public static void handleModalResponse(UUID requestId, ResponseMessage respMsg) {
        Modal.handleModalResponse(requestId, respMsg);
    }
}
