package de.noel.whitelist;

import de.noel.whitelist.commands.WhitelistUICommand;
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

    public WhitelistPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        instance = this;
    }

    @Override
    protected void setup() {
        getLogger().at(Level.INFO).log("WhitelistPlugin loading...");

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

        // Register commands
        getCommandRegistry().registerCommand(new WhitelistUICommand());

        getLogger().at(Level.INFO).log("WhitelistPlugin loaded - use /wl to open UI");
    }

    public static WhitelistPlugin get() {
        return instance;
    }

    public HytaleWhitelistProvider getWhitelistProvider() {
        return whitelistProvider;
    }
}
