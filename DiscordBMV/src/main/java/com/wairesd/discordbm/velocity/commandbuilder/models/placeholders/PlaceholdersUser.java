package com.wairesd.discordbm.velocity.commandbuilder.models.placeholders;

import com.wairesd.discordbm.velocity.commandbuilder.models.contexts.Context;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.Interaction;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class PlaceholdersUser implements Placeholder {
    @Override
    public CompletableFuture<String> replace(String template, Interaction event, Context context) {
        User user = context.getTargetUser() != null ? context.getTargetUser() : event.getUser();
        Member member = event.getGuild() != null ? event.getGuild().getMember(user) : null;

        String status = member != null && member.getOnlineStatus() != null
                ? member.getOnlineStatus().name()
                : "UNKNOWN";

        String displayName = member != null ? member.getEffectiveName() : user.getName();

        String joinedAt = member != null && member.getTimeJoined() != null
                ? member.getTimeJoined().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "UNKNOWN";

        String createdAt = user.getTimeCreated() != null
                ? user.getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : "UNKNOWN";

        String discriminator = user.getDiscriminator();
        String tag = user.getAsTag();
        String avatar = user.getEffectiveAvatarUrl();

        String result = template
                .replace("{user}", tag)
                .replace("{user_id}", user.getId())
                .replace("{user_name}", user.getName())
                .replace("{user_status}", status)
                .replace("{user_display_name}", displayName)
                .replace("{user_joined}", joinedAt)
                .replace("{user_create_at}", createdAt)
                .replace("{user_discriminator}", discriminator)
                .replace("{user_tag}", tag)
                .replace("{user_icon}", avatar);

        return CompletableFuture.completedFuture(result);
    }
}