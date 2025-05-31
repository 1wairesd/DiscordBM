package com.wairesd.discordbm.velocity.commandbuilder.listeners.buttons;

import com.wairesd.discordbm.velocity.DiscordBMV;
import com.wairesd.discordbm.velocity.commandbuilder.actions.buttons.ButtonActionService;
import com.wairesd.discordbm.velocity.commandbuilder.builder.FormModalBuilder;
import com.wairesd.discordbm.velocity.commandbuilder.checker.RoleChecker;
import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.ButtonConfig;
import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.FormButtonData;
import com.wairesd.discordbm.velocity.commandbuilder.handler.buttons.ButtonResponseHandler;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.models.pages.Page;
import com.wairesd.discordbm.velocity.commandbuilder.utils.EmbedFactoryUtils;
import com.wairesd.discordbm.velocity.commandbuilder.utils.MessageFormatterUtils;
import com.wairesd.discordbm.velocity.config.configurators.Forms;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.wairesd.discordbm.velocity.DiscordBMV.pageMap;

public class ButtonInteractionListener extends ListenerAdapter {
    private final ButtonActionService actionService = new ButtonActionService();
    private final RoleChecker permissionChecker = new RoleChecker();
    private final FormModalBuilder modalBuilder = new FormModalBuilder();
    private final ButtonResponseHandler responseHandler = new ButtonResponseHandler();

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (buttonId.startsWith("goto:")) {
            String targetPageId = buttonId.substring(5);
            Page page = pageMap.get(targetPageId);

            if (page == null) {
                event.reply("Страница не найдена.").setEphemeral(true).queue();
                return;
            }

            event.deferEdit().queue();

            if (page.getEmbedConfig() != null) {
                EmbedFactoryUtils.create(page.getEmbedConfig(), event, new Context(event))
                        .thenAccept(embed -> {
                            List<Button> buttons = new ArrayList<>();
                            for (ButtonConfig buttonConfig : page.getButtons()) {
                                String label = buttonConfig.getLabel();
                                String targetPage = buttonConfig.getTargetPage();
                                String newButtonId = "goto:" + targetPage;
                                buttons.add(Button.primary(newButtonId, label));
                            }
                            event.getHook().editOriginalEmbeds(embed)
                                    .setComponents(ActionRow.of(buttons))
                                    .queue();
                        })
                        .exceptionally(e -> {
                            event.getHook().editOriginal("Ошибка создания embed").queue();
                            return null;
                        });
            } else {
                String content = page.getContent();
                List<Button> buttons = new ArrayList<>();
                for (ButtonConfig buttonConfig : page.getButtons()) {
                    String label = buttonConfig.getLabel();
                    String targetPage = buttonConfig.getTargetPage();
                    String newButtonId = "goto:" + targetPage;
                    buttons.add(Button.primary(newButtonId, label));
                }
                event.getHook().editOriginal(content)
                        .setComponents(ActionRow.of(buttons))
                        .queue();
            }
            return;
        }

        FormButtonData formData = actionService.getFormButtonData(buttonId);
        if (formData != null) {
            if (!permissionChecker.hasPermission(event, formData.getRequiredRoleId())) {
                responseHandler.replyNoPermission(event);
                return;
            }
            var form = Forms.getForms().get(formData.getFormName());
            if (form == null) {
                responseHandler.replyNoForm(event);
                return;
            }
            var modal = modalBuilder.buildModal(form);
            DiscordBMV.plugin.getFormHandlers().put(modal.getId(), formData.getMessageTemplate());
            event.replyModal(modal).queue();
            return;
        }

        String messageTemplate = actionService.getMessage(buttonId);
        if (messageTemplate != null) {
            Context context = new Context(event);
            MessageFormatterUtils.format(messageTemplate, event, context, false)
                    .thenAccept(formattedMessage -> responseHandler.replyMessageOrExpired(event, formattedMessage));
        } else {
            responseHandler.replyMessageOrExpired(event, null);
        }
    }
}