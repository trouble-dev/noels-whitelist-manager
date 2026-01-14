package de.noel.whitelist.commands;

import de.noel.whitelist.pages.WhitelistPage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

public class WhitelistUICommand extends AbstractPlayerCommand {

    public WhitelistUICommand() {
        super("wl", "Opens the whitelist management UI");
    }

    @Override
    protected void execute(
        @Nonnull CommandContext context,
        @Nonnull Store<EntityStore> store,
        @Nonnull Ref<EntityStore> ref,
        @Nonnull PlayerRef playerRef,
        @Nonnull World world
    ) {
        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        WhitelistPage page = new WhitelistPage(playerRef);
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
