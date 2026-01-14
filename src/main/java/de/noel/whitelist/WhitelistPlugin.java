package de.noel.whitelist;

import de.noel.whitelist.commands.WhitelistUICommand;
import de.noel.whitelist.data.ConnectionAttempt;
import de.noel.whitelist.data.ConnectionAttemptManager;
import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.protocol.HostAddress;
import com.hypixel.hytale.server.core.event.events.player.PlayerSetupConnectEvent;
import com.hypixel.hytale.server.core.modules.accesscontrol.AccessControlModule;
import com.hypixel.hytale.server.core.modules.accesscontrol.provider.HytaleWhitelistProvider;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.logging.Level;

public class WhitelistPlugin extends JavaPlugin {

    private static WhitelistPlugin instance;
    private HytaleWhitelistProvider whitelistProvider;
    private ConnectionAttemptManager attemptManager;

    public WhitelistPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("WhitelistPlugin loading...");

        // Initialize connection attempt manager
        attemptManager = new ConnectionAttemptManager();

        // Get the whitelist provider from AccessControlModule via reflection
        try {
            AccessControlModule accessControl = AccessControlModule.get();
            Field providerField = AccessControlModule.class.getDeclaredField("whitelistProvider");
            providerField.setAccessible(true);
            whitelistProvider = (HytaleWhitelistProvider) providerField.get(accessControl);
            getLogger().at(Level.INFO).log("Successfully connected to whitelist provider");
        } catch (Exception e) {
            getLogger().at(Level.SEVERE).log("Failed to access whitelist provider: " + e.getMessage());
            return;
        }

        // Register event listener to capture rejected connection attempts (runs AFTER whitelist check)
        getEventRegistry().register(EventPriority.LAST, PlayerSetupConnectEvent.class, this::onPlayerSetupConnect);

        // Register commands
        getCommandRegistry().registerCommand(new WhitelistUICommand());

        getLogger().at(Level.INFO).log("WhitelistPlugin loaded - use /wl to open UI");
    }

    private void onPlayerSetupConnect(PlayerSetupConnectEvent event) {
        // Only log if the connection was rejected due to whitelist
        if (event.isCancelled() && event.getReason() != null && event.getReason().contains("not whitelisted")) {
            String ip = "unknown";
            HostAddress source = event.getReferralSource();
            if (source != null && source.host != null) {
                ip = source.host;
            }

            ConnectionAttempt attempt = new ConnectionAttempt(
                event.getUuid(),
                event.getUsername(),
                ip
            );

            attemptManager.addAttempt(attempt);

            getLogger().at(Level.INFO).log("Whitelist rejection logged: %s (%s) from %s",
                event.getUsername(), event.getUuid(), ip);
        }
    }

    public static WhitelistPlugin get() {
        return instance;
    }

    public HytaleWhitelistProvider getWhitelistProvider() {
        return whitelistProvider;
    }

    public ConnectionAttemptManager getAttemptManager() {
        return attemptManager;
    }
}
