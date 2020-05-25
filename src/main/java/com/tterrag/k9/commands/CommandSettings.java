package com.tterrag.k9.commands;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.tterrag.k9.commands.api.Argument;
import com.tterrag.k9.commands.api.Command;
import com.tterrag.k9.commands.api.CommandBase;
import com.tterrag.k9.commands.api.CommandContext;
import com.tterrag.k9.listeners.CommandListener;
import com.tterrag.k9.util.EmbedCreator;
import discord4j.core.event.domain.message.ReactionAddEvent;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


/**
 * Settings manager command for suggestions
 *
 * @author Su5eD
 */
@Command
public class CommandSettings extends CommandBase {
    private EmbedDeleter deleter; //Having an instance let's you send the embed multiple times (dunno who'd do this but it's better than getting an error)
    private static final List<String> settingTypes = Arrays.asList("roles", "channel", "managers", "upvote", "downvote", "both", "restrict");
    private static final Argument<String> ARG_SETTING = new WordArgument("setting", "setting type", false, settingTypes);
    private static final Argument<String> ARG_VALUE = new WordArgument("value", "setting value", false);
    private static final Argument<String> ARG_VALUE_EXTRA = new WordArgument("extra value", "an extra setting value", false);

    public CommandSettings() {
        super("settings", true);
    }

    @Nullable
    public static File getJsonFile() {
        Path jsonPath = Paths.get("src", "main", "resources/suggestions/config.json"); //TODO: export suggestions folder
        File config;
        if ((config = new File(jsonPath.toAbsolutePath().toString())) == null && (config = new File(String.valueOf(CommandSettings.class.getClassLoader().getResource("suggestions/config.json")))) == null) { //just in case
            return null;
        }
        return config;
    }

    @Nullable
    public static Config readFromJson(File file) {
        Gson gson = new Gson();
        try {
            InputStream stream = new FileInputStream(file);
            Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_16);

            return gson.fromJson(reader, Config.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void writeToJson(String key, Object value, @Nullable String special) {
        File file = getJsonFile();
        if (file == null) return;
        Config config = readFromJson(file);
        if (config == null) return;
        try {
            //TODO: Is there a lombok annotation that lets me get a field by string, like in an enum?
            switch (key) {
                case "roles":
                    if (Objects.equals(special, "add") && !config.roles.contains(value)) config.roles.add((Long)value);
                    else if (Objects.equals(special, "remove")) config.roles.remove(value);
                    break;
                case "managers":
                    if (Objects.equals(special, "add") && !config.managers.contains(value)) config.managers.add((Long)value);
                    else if (Objects.equals(special, "remove")) config.managers.remove(value);
                    break;
                case "channel":
                    config.channel = (long) value;
                    break;
                case "discussion":
                    config.discussion = (long) value;
                    break;
                case "upvote":
                    config.upvote = (String) value;
                    break;
                case "downvote":
                    config.downvote = (String) value;
                    break;
                case "both":
                    config.both = Boolean.parseBoolean((String) value);
                    break;
                case "restrict":
                    config.restrict = Boolean.parseBoolean((String) value);
                    break;
            }
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut, StandardCharsets.UTF_16);
            myOutWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(config));
            myOutWriter.close();
            fOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Mono<?> process(CommandContext ctx) {
        if(!ctx.getArgs().isEmpty()) return processArgs(ctx);
        return ctx.getMessage().delete()
                .then(ctx.getChannel().flatMap(channel -> {
                    Config config = readFromJson(getJsonFile());
                    if (config == null) return channel.createMessage("Error loading settings. Please contact the bot admin");
                    EmbedCreator embed = EmbedCreator.builder()
                        .title("**Suggestion settings**")
                        .description(String.format("Configure your bot. \nChange a setting using `%ssettings <underlined word in category name> <value>`", CommandListener.getPrefix(ctx.getGuildId())))
                        .color(0xff8d2e)
                        .field(":hash:Suggestions __channel__", "Currently "+(config.channel != 0 ? "#"+ctx.getGuild().block().getChannelById(Snowflake.of(config.channel)).block().getName() : "not set")+". \nAccepted values: _channel mention_", true)
                        .field("\uD83D\uDDE8️Suggestions __discussion__ channel", "Currently "+(config.discussion != 0 ? "#"+ctx.getGuild().block().getChannelById(Snowflake.of(config.discussion)).block().getName() : "not set")+". \nAccepted values: _channel mention_", true)
                        .field(":question:Roles that can suggest", "Currently "+(!config.roles.isEmpty() ? Joiner.on(", ").join(config.roles) : "everyone")+". \nEdit using *add/remove + role mention*", true)
                        .field("\uD83C\uDFF7️Suggestion __managers__ roles", "Currently "+(!config.managers.isEmpty() ? Joiner.on(", ").join(config.managers) : "not set")+".\nEdit using _add/remove + role mention_.", true)
                        .field("⬆️__Upvote__ emoji", "Currently "+config.upvote+". \nAccepted values: `\\:emoji:`", true)
                        .field("⬇️__Downvote__ emoji", "Currently "+config.downvote+". \nAccepted values: `\\:emoji:`", true)
                        .field(":left_right_arrow:Members can vote for __both__ options", "Now "+config.both+". \nAccepted values: _true/false_", true)
                        .field("\uD83D\uDEE1️__Restrict__ suggestion command usage to suggestion channel", "Now "+config.restrict+". \nAccepted values: _true/false_", true)
                        .build();
                    return channel.createEmbed(embed)
                            .flatMap(msg -> {
                                deleter = new EmbedDeleter(msg, 60, ctx);
                                return msg.addReaction(ReactionEmoji.unicode("\u2705"));
                            });
                })
                .then(ctx.getChannel().doOnNext(x -> deleter.start())));
    }

    /**
     * Turn a mention into an id
     * @param input String to be parsed
     * @param type <code>role</code> or <code>channel</code>
     * @return id of input
     */
    public long smartParse(String input, String type) {
        if (input.length() < 21) return 0;
        else if (type.equals("role") && (input.contains("#") || !input.contains("&"))) return 0;
        else if (type.equals("channel") && input.contains("@")) return 0;
        long id = Long.parseLong(input.substring(type.equals("role") ? 3 : 2, input.length()-1));
        System.out.println("id: "+id);
        return id;
    }

    public Mono<?> processArgs(CommandContext ctx) {
        Map<Argument<?>, String> args = ctx.getArgs();
        if(args.size() > 1) {
            if (args.get(ARG_SETTING).matches("managers|roles") && !args.get(ARG_VALUE).matches("add|remove")) return ctx.reply("Invalid value provided for argument");
        }
        String arg = args.get(ARG_SETTING);
        String val = args.get(ARG_VALUE);
        String extra = args.get(ARG_VALUE_EXTRA);
        switch (arg) {
            case "channel":
            case "discussion":
                long channel = smartParse(val, "channel");
                if (channel == 0) return ctx.reply("You must mention a channel");
                writeToJson(arg, channel, null);
                break;
            case "roles":
            case "managers":
                long role = smartParse(extra, "role");
                if (role == 0) return ctx.reply("You must mention a role");
                writeToJson(arg, role, val);
                break;
            case "upvote":
            case "downvote":
            case "both":
            case "restrict":
                writeToJson(arg, val, null);
                break;
        }
        return ctx.reply("Successfully updated settings");
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

    /**
     * Holds all configuration
     */
    private static class Config {
        public long channel;
        public long discussion;
        public List<Long> roles = new LinkedList<>();
        public List<Long> managers = new LinkedList<>();
        public String upvote = "⬆";
        public String downvote = "⬇";
        public boolean both;
        public boolean restrict;
    }
}
