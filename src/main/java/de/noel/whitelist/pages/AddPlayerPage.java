package de.noel.whitelist.pages;

import de.noel.whitelist.WhitelistPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.accesscontrol.provider.HytaleWhitelistProvider;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import java.util.UUID;

public class AddPlayerPage extends InteractiveCustomUIPage<AddPlayerPage.AddEventData> {

    // Store references for async callback navigation
    private Ref<EntityStore> currentRef;
    private Store<EntityStore> currentStore;

    public static class AddEventData {
        public String action;
        public String playerName;
        public String playerUuid;

        public static final BuilderCodec<AddEventData> CODEC = ((BuilderCodec.Builder<AddEventData>) ((BuilderCodec.Builder<AddEventData>) ((BuilderCodec.Builder<AddEventData>)
            BuilderCodec.builder(AddEventData.class, AddEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (AddEventData o, String v) -> o.action = v, (AddEventData o) -> o.action)
                .add())
                .append(new KeyedCodec<>("@PlayerName", Codec.STRING), (AddEventData o, String v) -> o.playerName = v, (AddEventData o) -> o.playerName)
                .add())
                .append(new KeyedCodec<>("@PlayerUUID", Codec.STRING), (AddEventData o, String v) -> o.playerUuid = v, (AddEventData o) -> o.playerUuid)
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

        // Bind confirm button - capture both input values
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ConfirmButton",
            new EventData()
                .append("Action", "Confirm")
                .append("@PlayerName", "#NameInput.Value")
                .append("@PlayerUUID", "#UuidInput.Value")
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
        // Store references for async navigation
        this.currentRef = ref;
        this.currentStore = store;

        Player player = (Player) store.getComponent(ref, Player.getComponentType());

        if ("Cancel".equals(data.action)) {
            // Go back to whitelist page
            navigateToWhitelistPage();
            return;
        }

        if ("Confirm".equals(data.action)) {
            String username = data.playerName != null ? data.playerName.trim() : "";
            String uuidStr = data.playerUuid != null ? data.playerUuid.trim() : "";

            UUID uuid = null;
            String displayName = null;

            // Option 1: UUID was provided directly
            if (!uuidStr.isEmpty()) {
                try {
                    uuid = UUID.fromString(uuidStr);
                    displayName = uuidStr.substring(0, 8) + "...";
                } catch (IllegalArgumentException e) {
                    showError("Invalid UUID format!");
                    return;
                }
            }
            // Option 2: Username provided - check if player is online
            else if (!username.isEmpty()) {
                PlayerRef targetPlayer = Universe.get().getPlayerByUsername(username, NameMatching.EXACT);

                if (targetPlayer != null) {
                    uuid = targetPlayer.getUuid();
                    displayName = username;
                } else {
                    showError("Player not online! Use UUID instead.");
                    playerRef.sendMessage(Message.raw("Tip: Player must be online OR enter their UUID directly."));
                    return;
                }
            }
            // Neither provided
            else {
                showError("Enter username OR UUID!");
                return;
            }

            // Add to whitelist
            HytaleWhitelistProvider provider = WhitelistPlugin.get().getWhitelistProvider();
            final UUID finalUuid = uuid;
            final String finalDisplayName = displayName;

            if (provider.modify(list -> list.add(finalUuid))) {
                playerRef.sendMessage(Message.raw("Added " + finalDisplayName + " (" + finalUuid + ") to whitelist"));
            } else {
                playerRef.sendMessage(Message.raw("Player is already whitelisted"));
            }

            // Navigate back to whitelist page
            navigateToWhitelistPage();
        }
    }

    private void navigateToWhitelistPage() {
        if (currentRef != null && currentStore != null) {
            Player player = (Player) currentStore.getComponent(currentRef, Player.getComponentType());
            if (player != null) {
                WhitelistPage whitelistPage = new WhitelistPage(playerRef);
                player.getPageManager().openCustomPage(currentRef, currentStore, whitelistPage);
            }
        }
    }

    private void showError(String message) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        commandBuilder.set("#Error.Visible", true);
        commandBuilder.set("#Error.Text", message);
        sendUpdate(commandBuilder);
    }
}
