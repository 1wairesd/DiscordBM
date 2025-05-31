package com.wairesd.discordbm.velocity.commandbuilder.actions.page;

import com.wairesd.discordbm.velocity.commandbuilder.models.actions.CommandAction;
import com.wairesd.discordbm.velocity.commandbuilder.models.buttons.ButtonConfig;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import com.wairesd.discordbm.velocity.commandbuilder.models.pages.Page;
import com.wairesd.discordbm.velocity.commandbuilder.models.placeholders.PlaceholdersResolved;
import com.wairesd.discordbm.velocity.commandbuilder.utils.EmbedFactoryUtils;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.wairesd.discordbm.velocity.config.configurators.Pages.pageMap;

public class SendPageAction implements CommandAction {
    private final String pageIdTemplate;

    public SendPageAction(Map<String, Object> props) {
        this.pageIdTemplate = (String) props.get("page_id");
    }

    @Override
    public CompletableFuture<Void> execute(Context context) {
        String pageId = PlaceholdersResolved.replaceSync(pageIdTemplate, context);
        if (pageId == null || pageId.isBlank()) pageId = "1";

        Page page = pageMap.get(pageId);
        if (page == null) {
            context.setMessageText("Страница не найдена. (ID=" + pageId + ")");
            return CompletableFuture.completedFuture(null);
        }

        List<Button> buttons = new ArrayList<>();
        for (ButtonConfig cfg : page.getButtons()) {
            buttons.add(Button.primary("goto:" + cfg.getTargetPage(), cfg.getLabel()));
        }
        context.addActionRow(ActionRow.of(buttons));

        if (page.getEmbedConfig() != null) {
            return EmbedFactoryUtils
                    .create(page.getEmbedConfig(), context.getEvent(), context)
                    .thenAccept(context::setEmbed);
        }

        context.setMessageText(page.getContent());
        return CompletableFuture.completedFuture(null);
    }
}