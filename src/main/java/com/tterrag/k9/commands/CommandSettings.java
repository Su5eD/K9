package com.tterrag.k9.commands;

import com.tterrag.k9.commands.api.Command;
import com.tterrag.k9.commands.api.CommandBase;
import com.tterrag.k9.commands.api.CommandContext;
import com.tterrag.k9.listeners.CommandListener;
import com.tterrag.k9.util.EmbedCreator;
import com.tterrag.k9.util.annotation.Nullable;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import reactor.core.publisher.Mono;


/**
 * Settings manager command for suggestions
 *
 * @author Su5eD
 */
@Command
public class CommandSettings extends CommandBase {
    private EmbedDeleter deleter; //Having an instance let's you send the embed multiple times (dunno who'd do this but it's better than getting an error)

    public CommandSettings() {
        super("settings", true);
    }

    @Override
    public Mono<?> process(CommandContext ctx) {
        EmbedCreator settingsEmbed = EmbedCreator.builder()
                .title("**Suggestion settings**")
                .description(String.format("Configure your bot. \nChange a setting using `%ssettings <underlined word in category name> <value>`", CommandListener.getPrefix(ctx.getGuildId())))
                .color(0xff8d2e)
                .field(":hash:Suggestions __channel__", "Currently #\"+(config.channel ? message.guild.channels.cache.get(config.channel).name : \"not set\")+\". \\nAccepted values: _channel mention_", true)
                .field("\uD83D\uDDE8️Suggestions __discussion__ channel", "Currently \"+(config.channel ? message.guild.channels.cache.get(config.channel).name : \"not set\")+\". \\nAccepted values: _channel mention_", true)
                .field("\uD83C\uDFF7️Suggestion __managers__ roles", "Currently (roles).\nEdit using _add/remove (role mention)_.", true)
                .field("⬆️__Upvote__ emoji", "Currently (emoji). \nAccepted values: `\\:emoji:`", true)
                .field("⬇️__Downvote__ emoji", "Currently (emoji). \nAccepted values: `\\:emoji:`", true)
                .field(":left_right_arrow:Members can vote for __both__ options", "Now (true/false). \nAccepted values: _true/false_", true)
                .field("\uD83D\uDEE1️__Restrict__ suggestion command usage to suggestion channel", "Now (true/false). \nAccepted values: _true/false_", true)
                .build();
        /*try {
            Gson gson = new Gson();
            Path jsonPath = Paths.get(ClassLoader.getSystemClassLoader().getResource("suggestion_config.json").getPath().substring(1));
            Reader reader = Files.newBufferedReader(jsonPath);
            Map<?, ?> map = gson.fromJson(reader, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        return ctx.getMessage().delete()
                .then(ctx.getChannel()
                        .flatMap(channel -> channel.createEmbed(settingsEmbed)
                                .flatMap(msg -> {
                                    deleter = new EmbedDeleter(msg, 10, ctx);
                                    return msg.addReaction(ReactionEmoji.unicode("\u2705"));
                                })))
                .then(ctx.getChannel().doOnNext(x -> deleter.start()));

    }

    @Override
    public String getDescription(CommandContext ctx) {
        return "Settings manager command for suggestions";
    }

    /**
     * Handles expiration of the settings embed
     */
    private class EmbedDeleter extends Thread {
        private final int timeout;
        public Message msg;
        private boolean shouldExit;

        /**
         * @param msgIn   The message to expire
         * @param timeout Timeout before expiration in seconds
         */
        private EmbedDeleter(@Nullable Message msgIn, int timeout, CommandContext ctx) {
            if (msgIn != null) {
                msg = msgIn;
            }
            this.timeout = timeout;
            ctx.getClient().getEventDispatcher()
                    .on(ReactionAddEvent.class)
                    .map(this::onMessageReactionAdd)
                    .subscribe();
        }

        public void run() {
            try {
                for (int i = 0; i < timeout; i++) {
                    if (shouldExit) {
                        shouldExit = false;
                        msg.delete().subscribe();
                        return;
                    }
                    Thread.sleep(1000);
                }
                msg.delete().subscribe();
            } catch (InterruptedException e) {
                e.printStackTrace();
                msg.delete().subscribe();
            }
        }

        public Mono<?> onMessageReactionAdd(ReactionAddEvent event) {
            if (msg == null || event.getUserId().equals(event.getClient().getSelfId().get())) {
                return event.getChannel().then();
            }
            if (msg.getId().equals(event.getMessageId())) {
                shouldExit = true;
            }
            return event.getChannel().then();
        }
    }
}
