package net.essentialsx.discord.listeners;

import com.earth2me.essentials.Console;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.FormatUtil;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.essentialsx.api.v2.events.AsyncUserDataLoadEvent;
import net.essentialsx.api.v2.events.discord.DiscordMessageEvent;
import net.essentialsx.discord.EssentialsJDA;
import net.essentialsx.discord.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.text.MessageFormat;

public class BukkitListener implements Listener {
    private final static String AVATAR_URL = "https://crafatar.com/avatars/{uuid}?overlay=true";
    private final EssentialsJDA jda;

    public BukkitListener(EssentialsJDA jda) {
        this.jda = jda;
    }

    /**
     * Processes messages from all other events.
     * This way it allows other plugins to modify route/message or just cancel it.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDiscordMessage(DiscordMessageEvent event) {
        jda.sendMessage(event, event.getMessage(), event.isAllowGroupMentions());
    }

    // Bukkit Events

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMute(MuteStatusChangeEvent event) {
        if (!event.getValue()) {
            sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.MUTE,
                    MessageUtil.formatMessage(jda.getSettings().getUnmuteFormat(),
                        MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getName()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getDisplayName())));
        } else if (event.getTimestamp().isPresent()) {
            final boolean console = event.getController() == null;
            final MessageFormat msg = event.getReason() == null ? jda.getSettings().getTempMuteFormat() : jda.getSettings().getTempMuteReasonFormat();
            sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.MUTE,
                    MessageUtil.formatMessage(msg,
                            MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getDisplayName()),
                            MessageUtil.sanitizeDiscordMarkdown(console ? Console.NAME : event.getController().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(console ? Console.DISPLAY_NAME : event.getController().getDisplayName()),
                            DateUtil.formatDateDiff(event.getTimestamp().get()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getReason())));
        } else {
            final boolean console = event.getController() == null;
            final MessageFormat msg = event.getReason() == null ? jda.getSettings().getPermMuteFormat() : jda.getSettings().getPermMuteReasonFormat();
            sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.MUTE,
                    MessageUtil.formatMessage(msg,
                            MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getAffected().getDisplayName()),
                            MessageUtil.sanitizeDiscordMarkdown(console ? Console.NAME : event.getController().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(console ? Console.DISPLAY_NAME : event.getController().getDisplayName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getReason())));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(jda.getPlugin(), () ->
                sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.CHAT,
                        MessageUtil.formatMessage(jda.getSettings().getMcToDiscordFormat(player),
                            MessageUtil.sanitizeDiscordMarkdown(player.getName()),
                            MessageUtil.sanitizeDiscordMarkdown(player.getDisplayName()),
                            player.hasPermission("essentials.discord.markdown") ? event.getMessage() : MessageUtil.sanitizeDiscordMarkdown(event.getMessage()),
                            MessageUtil.sanitizeDiscordMarkdown(player.getWorld().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(FormatUtil.stripEssentialsFormat(jda.getPlugin().getEss().getPermissionsHandler().getPrefix(player))),
                            MessageUtil.sanitizeDiscordMarkdown(FormatUtil.stripEssentialsFormat(jda.getPlugin().getEss().getPermissionsHandler().getSuffix(player)))),
                        player.hasPermission("essentials.discord.ping"),
                        jda.getSettings().isShowAvatar() ? AVATAR_URL.replace("{uuid}", player.getUniqueId().toString()) : null,
                        jda.getSettings().isShowName() ? MessageUtil.sanitizeDiscordMarkdown(player.getName()) : null));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(AsyncUserDataLoadEvent event) {
        // Delay join to let nickname load
        if (event.getJoinMessage() != null) {
            sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.JOIN,
                    MessageUtil.formatMessage(jda.getSettings().getJoinFormat(event.getUser().getBase()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getUser().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getUser().getDisplayName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getJoinMessage())),
                    false,
                    jda.getSettings().isShowAvatar() ? AVATAR_URL.replace("{uuid}", event.getUser().getBase().getUniqueId().toString()) : null,
                    jda.getSettings().isShowName() ? MessageUtil.sanitizeDiscordMarkdown(event.getUser().getName()) : null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onQuit(PlayerQuitEvent event) {
        if (event.getQuitMessage() != null) {
            sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.LEAVE,
                    MessageUtil.formatMessage(jda.getSettings().getQuitFormat(event.getPlayer()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getPlayer().getName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getPlayer().getDisplayName()),
                            MessageUtil.sanitizeDiscordMarkdown(event.getQuitMessage())),
                    false,
                    jda.getSettings().isShowAvatar() ? AVATAR_URL.replace("{uuid}", event.getPlayer().getUniqueId().toString()) : null,
                    jda.getSettings().isShowName() ? MessageUtil.sanitizeDiscordMarkdown(event.getPlayer().getName()) : null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.DEATH,
                MessageUtil.formatMessage(jda.getSettings().getDeathFormat(event.getEntity()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getEntity().getName()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getEntity().getDisplayName()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getDeathMessage())),
                false,
                jda.getSettings().isShowAvatar() ? AVATAR_URL.replace("{uuid}", event.getEntity().getUniqueId().toString()) : null,
                jda.getSettings().isShowName() ? MessageUtil.sanitizeDiscordMarkdown(event.getEntity().getName()) : null);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKick(PlayerKickEvent event) {
        sendDiscordMessage(DiscordMessageEvent.MessageType.DefaultTypes.KICK,
                MessageUtil.formatMessage(jda.getSettings().getKickFormat(),
                        MessageUtil.sanitizeDiscordMarkdown(event.getPlayer().getName()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getPlayer().getDisplayName()),
                        MessageUtil.sanitizeDiscordMarkdown(event.getReason())));
    }

    private void sendDiscordMessage(final DiscordMessageEvent.MessageType messageType, final String message) {
        sendDiscordMessage(messageType, message, false, null, null);
    }

    private void sendDiscordMessage(final DiscordMessageEvent.MessageType messageType, final String message, final boolean allowPing, final String avatarUrl, final String name) {
        if (jda.getPlugin().getSettings().getMessageChannel(messageType.getKey()).equalsIgnoreCase("none")) {
            return;
        }

        final DiscordMessageEvent event = new DiscordMessageEvent(messageType, FormatUtil.stripFormat(message), allowPing, avatarUrl, name);
        if (Bukkit.getServer().isPrimaryThread()) {
            Bukkit.getPluginManager().callEvent(event);
        } else {
            Bukkit.getScheduler().runTask(jda.getPlugin(), () -> Bukkit.getPluginManager().callEvent(event));
        }
    }
}