package de.noel.whitelist.pages;

import de.noel.whitelist.WhitelistPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.accesscontrol.provider.HytaleWhitelistProvider;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.AuthUtil;
import javax.annotation.Nonnull;

public class AddPlayerPage extends InteractiveCustomUIPage<AddPlayerPage.AddEventData> {

    public static class AddEventData {
        public String action;
        public String playerName;

        public static final BuilderCodec<AddEventData> CODEC = ((BuilderCodec.Builder<AddEventData>) ((BuilderCodec.Builder<AddEventData>)
            BuilderCodec.builder(AddEventData.class, AddEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (AddEventData o, String v) -> o.action = v, (AddEventData o) -> o.action)
                .add())
                .append(new KeyedCodec<>("@PlayerName", Codec.STRING), (AddEventData o, String v) -> o.playerName = v, (AddEventData o) -> o.playerName)
                .add())
            .build();
    }

    public AddPlayerPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, AddEventData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/AddPlayerPage.ui");

        // Bind confirm button - capture input value
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ConfirmButton",
            new EventData()
                .append("Action", "Confirm")
                .append("@PlayerName", "#NameInput.Value")
        );

        // Bind cancel button
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelButton",
            new EventData().append("Action", "Cancel")
        );
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull AddEventData data
    ) {
        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if ("Cancel".equals(data.action)) {
            // Go back to whitelist page
            WhitelistPage whitelistPage = new WhitelistPage(playerRef);
            player.getPageManager().openCustomPage(ref, store, whitelistPage);
            return;
        }

        if ("Confirm".equals(data.action)) {
            String username = data.playerName;

            if (username == null || username.trim().isEmpty()) {
                showError("Please enter a player name");
                return;
            }

            username = username.trim();
            final String finalUsername = username;

            // Look up UUID and add to whitelist
            AuthUtil.lookupUuid(username).thenAccept(uuid -> {
                HytaleWhitelistProvider provider = WhitelistPlugin.get().getWhitelistProvider();

                if (provider.modify(list -> list.add(uuid))) {
                    playerRef.sendMessage(Message.raw("Added " + finalUsername + " to whitelist"));
                } else {
                    playerRef.sendMessage(Message.raw(finalUsername + " is already whitelisted"));
                }

                // Note: Can't easily go back to WhitelistPage from async context
                // User will need to close and reopen
            }).exceptionally(ex -> {
                playerRef.sendMessage(Message.raw("Failed to find player: " + finalUsername));
                return null;
            });

            // Close the page
            player.getPageManager().setPage(ref, store, Page.None);
        }
    }

    private void showError(String message) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#Error.Visible", true);
        commandBuilder.set("#Error.Text", message);
        sendUpdate(commandBuilder);
    }
}
