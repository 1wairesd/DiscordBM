package com.wairesd.discordbm.velocity.commandbuilder.utils;

import com.wairesd.discordbm.common.utils.logging.PluginLogger;
import com.wairesd.discordbm.common.utils.logging.Slf4jPluginLogger;
import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class EmbedFactoryUtils {
    private static final PluginLogger logger = new Slf4jPluginLogger(LoggerFactory.getLogger("DiscordBMV"));

    public static CompletableFuture<MessageEmbed> create(Map<String, Object> embedMap, Interaction event, Context context) {
        EmbedBuilder builder = new EmbedBuilder();

        List<CompletableFuture<?>> futures = new ArrayList<>();

        if (embedMap.containsKey("title")) {
            CompletableFuture<Void> f = MessageFormatterUtils.format((String) embedMap.get("title"), event, context, false)
                    .thenAccept(builder::setTitle);
            futures.add(f);
        }

        if (embedMap.containsKey("description")) {
            CompletableFuture<Void> f = MessageFormatterUtils.format((String) embedMap.get("description"), event, context, false)
                    .thenAccept(builder::setDescription);
            futures.add(f);
        }

        if (embedMap.containsKey("color")) {
            try {
                int color = parseColor(embedMap.get("color"));
                builder.setColor(color);
            } catch (NumberFormatException e) {
                logger.warn("Invalid color format: {}", embedMap.get("color"));
            }
        }

        if (embedMap.containsKey("fields")) {
            List<Map<String, Object>> fields = (List<Map<String, Object>>) embedMap.get("fields");

            for (Map<String, Object> field : fields) {
                CompletableFuture<String> nameFuture = MessageFormatterUtils.format((String) field.get("name"), event, context, false);
                CompletableFuture<String> valueFuture = MessageFormatterUtils.format((String) field.get("value"), event, context, false);
                boolean inline = (Boolean) field.getOrDefault("inline", false);

                CompletableFuture<Void> fieldFuture = nameFuture.thenCombine(valueFuture, (name, value) -> {
                    builder.addField(name, value, inline);
                    return null;
                });
                futures.add(fieldFuture);
            }
        }

        if (embedMap.containsKey("author")) {
            Map<String, Object> author = (Map<String, Object>) embedMap.get("author");

            CompletableFuture<Void> f = MessageFormatterUtils.format((String) author.get("name"), event, context, false)
                    .thenCompose(name -> getSafeUrl(author.get("url"), event, context)
                            .thenCompose(url -> getSafeUrl(author.get("icon_url"), event, context)
                                    .thenAccept(icon -> builder.setAuthor(name, url, icon))));
            futures.add(f);
        }

        if (embedMap.containsKey("footer")) {
            Map<String, Object> footer = (Map<String, Object>) embedMap.get("footer");

            CompletableFuture<Void> f = MessageFormatterUtils.format((String) footer.get("text"), event, context, false)
                    .thenCompose(text -> getSafeUrl(footer.get("icon_url"), event, context)
                            .thenAccept(icon -> builder.setFooter(text, icon)));
            futures.add(f);
        }

        if (embedMap.containsKey("thumbnail")) {
            CompletableFuture<Void> f = MessageFormatterUtils.format((String) embedMap.get("thumbnail"), event, context, false)
                    .thenAccept(thumb -> {
                        if (isValidUrl(thumb)) builder.setThumbnail(thumb);
                    });
            futures.add(f);
        }

        if (embedMap.containsKey("image")) {
            CompletableFuture<Void> f = MessageFormatterUtils.format((String) embedMap.get("image"), event, context, false)
                    .thenAccept(image -> {
                        if (isValidUrl(image)) builder.setImage(image);
                    });
            futures.add(f);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> builder.build());
    }

    private static boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private static CompletableFuture<String> getSafeUrl(Object raw, Interaction event, Context context) {
        if (raw == null) return CompletableFuture.completedFuture(null);
        return MessageFormatterUtils.format(raw.toString(), event, context, false)
                .thenApply(formatted -> isValidUrl(formatted) ? formatted : null);
    }

    private static int parseColor(Object color) throws NumberFormatException {
        if (color instanceof Integer) return (Integer) color;
        String str = color.toString().trim();
        if (str.startsWith("#")) str = str.substring(1);
        return Integer.parseInt(str, 16);
    }
}
