package com.tterrag.k9.commands;

import com.google.gson.Gson;
import com.tterrag.k9.commands.api.Command;
import com.tterrag.k9.commands.api.CommandBase;
import com.tterrag.k9.commands.api.CommandContext;
import com.tterrag.k9.listeners.CommandListener;
import com.tterrag.k9.util.EmbedCreator;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Settings manager command for suggestions
 * @author Su5eD
 */
@Command
public class CommandSettings extends CommandBase {
    public CommandSettings() {
        super("settings", true);
    }

    @Override
    public Mono<?> process(CommandContext ctx) {
        try {
            Gson gson = new Gson();
            Path jsonPath = Paths.get(ClassLoader.getSystemClassLoader().getResource("suggestion_config.json").getPath().substring(1));
            Reader reader = Files.newBufferedReader(jsonPath);
            Map<?, ?> map = gson.fromJson(reader, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        EmbedCreator settingsEmbed = EmbedCreator.builder()
                .title("**Suggestion settings**")
                .description(String.format("Configure your bot. \nChange a setting using `%ssettings <underlined word in category name> <value>`", CommandListener.getPrefix(ctx.getGuildId())))
                .color(0xff8d2e)
                .field(":hash:Suggestions __channel__", "Currently #\"+(config.channel ? message.guild.channels.cache.get(config.channel).name : \"not set\")+\". \\nAccepted values: _channel mention_", true)
                .field("\uD83D\uDDE8️Suggestions __discussion__ channel", "Currently \"+(config.channel ? message.guild.channels.cache.get(config.channel).name : \"not set\")+\". \\nAccepted values: _channel mention_", true)
                .field("❓__Roles__ that can suggest", "Currently (roles).\nEdit using _add/remove (role mention)_.", true)
                .field("\uD83C\uDFF7️Suggestion __managers__ roles", "Currently (roles).\nEdit using _add/remove (role mention)_.", true)
                .field("⬆️__Upvote__ emoji", "Currently (emoji). \nAccepted values: `\\:emoji:`", true)
                .field("⬇️__Downvote__ emoji", "Currently (emoji). \nAccepted values: `\\:emoji:`", true)
                .field(":left_right_arrow:Members can vote for __both__ options", "Now (true/false). \nAccepted values: _true/false_", true)
                .field("\uD83D\uDEE1️__Restrict__ suggestion command usage to suggestion channel", "Now (true/false). \nAccepted values: _true/false_", true)
                .build();
        return ctx.getMessage().delete()
                .then(ctx.getChannel().block().createEmbed(settingsEmbed).block().addReaction(ReactionEmoji.unicode("\u2705")));
    }

    @Override
    public String getDescription(CommandContext ctx) {
        return "Settings manager command for suggestions";
    }
}
