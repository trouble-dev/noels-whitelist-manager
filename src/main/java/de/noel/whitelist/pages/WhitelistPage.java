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
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class WhitelistPage extends InteractiveCustomUIPage<WhitelistPage.WhitelistEventData> {

    public static class WhitelistEventData {
        public String action;
        public String uuid;

        public static final BuilderCodec<WhitelistEventData> CODEC = ((BuilderCodec.Builder<WhitelistEventData>) ((BuilderCodec.Builder<WhitelistEventData>)
            BuilderCodec.builder(WhitelistEventData.class, WhitelistEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (WhitelistEventData o, String v) -> o.action = v, (WhitelistEventData o) -> o.action)
                .add())
                .append(new KeyedCodec<>("UUID", Codec.STRING), (WhitelistEventData o, String v) -> o.uuid = v, (WhitelistEventData o) -> o.uuid)
                .add())
            .build();
    }

    public WhitelistPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, WhitelistEventData.CODEC);
    }

    @Override
    public void build(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull UICommandBuilder commandBuilder,
        @Nonnull UIEventBuilder eventBuilder,
        @Nonnull Store<EntityStore> store
    ) {
        commandBuilder.append("Pages/WhitelistPage.ui");

        HytaleWhitelistProvider provider = WhitelistPlugin.get().getWhitelistProvider();
        boolean isEnabled = provider.isEnabled();
        Set<UUID> whitelist = provider.getList();

        // Set status
        commandBuilder.set("#StatusLabel.Text", isEnabled ? "ENABLED" : "DISABLED");
        commandBuilder.set("#StatusLabel.Style.TextColor", isEnabled ? "#4aff7f" : "#ff6b6b");
        commandBuilder.set("#ToggleButton.Text", isEnabled ? "DISABLE" : "ENABLE");
        commandBuilder.set("#PlayerCount.Text", "PLAYERS (" + whitelist.size() + ")");

        // Build player list
        buildPlayerList(commandBuilder, eventBuilder, whitelist);

        // Bind action buttons
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ToggleButton",
            new EventData().append("Action", "Toggle")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#AddButton",
            new EventData().append("Action", "Add")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#RefreshButton",
            new EventData().append("Action", "Refresh")
        );

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CloseButton",
            new EventData().append("Action", "Close")
        );
    }

    private void buildPlayerList(UICommandBuilder commandBuilder, UIEventBuilder eventBuilder, Set<UUID> whitelist) {
        commandBuilder.clear("#PlayerList");

        if (whitelist.isEmpty()) {
            commandBuilder.appendInline("#PlayerList", "Label { Text: \"No players whitelisted\"; Anchor: (Height: 40); Style: (FontSize: 14, TextColor: #6e7da1, HorizontalAlignment: Center, VerticalAlignment: Center); }");
            return;
        }

        // Try to get usernames for all whitelisted players
        Map<UUID, String> usernames = new HashMap<>();
        for (UUID uuid : whitelist) {
            PlayerRef onlinePlayer = Universe.get().getPlayer(uuid);
            if (onlinePlayer != null) {
                usernames.put(uuid, onlinePlayer.getUsername());
            }
        }

        int i = 0;
        for (UUID uuid : whitelist) {
            String selector = "#PlayerList[" + i + "]";
            commandBuilder.append("#PlayerList", "Pages/WhitelistEntry.ui");

            // Show username if available, otherwise show shortened UUID
            String displayName = usernames.getOrDefault(uuid, uuid.toString().substring(0, 8) + "...");
            String statusText = usernames.containsKey(uuid) ? "(online)" : uuid.toString();

            commandBuilder.set(selector + " #PlayerName.Text", displayName);
            commandBuilder.set(selector + " #PlayerUUID.Text", statusText);

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector + " #RemoveButton",
                new EventData().append("Action", "Remove").append("UUID", uuid.toString()),
                false
            );
            i++;
        }
    }

    @Override
    public void handleDataEvent(
        @Nonnull Ref<EntityStore> ref,
        @Nonnull Store<EntityStore> store,
        @Nonnull WhitelistEventData data
    ) {
        Player player = (Player) store.getComponent(ref, Player.getComponentType());
        HytaleWhitelistProvider provider = WhitelistPlugin.get().getWhitelistProvider();

        switch (data.action) {
            case "Toggle":
                boolean newState = !provider.isEnabled();
                provider.setEnabled(newState);
                provider.syncSave();
                playerRef.sendMessage(Message.raw("Whitelist " + (newState ? "enabled" : "disabled")));
                refreshPage(ref, store);
                break;

            case "Remove":
                if (data.uuid != null) {
                    UUID uuidToRemove = UUID.fromString(data.uuid);
                    provider.modify(list -> list.remove(uuidToRemove));
                    playerRef.sendMessage(Message.raw("Removed player from whitelist"));
                    refreshPage(ref, store);
                }
                break;

            case "Add":
                // Open the add player page
                AddPlayerPage addPage = new AddPlayerPage(playerRef);
                player.getPageManager().openCustomPage(ref, store, addPage);
                break;

            case "Refresh":
                refreshPage(ref, store);
                break;

            case "Close":
                player.getPageManager().setPage(ref, store, Page.None);
                break;

            default:
                break;
        }
    }

    private void refreshPage(Ref<EntityStore> ref, Store<EntityStore> store) {
        UICommandBuilder commandBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        HytaleWhitelistProvider provider = WhitelistPlugin.get().getWhitelistProvider();
        boolean isEnabled = provider.isEnabled();
        Set<UUID> whitelist = provider.getList();

        commandBuilder.set("#StatusLabel.Text", isEnabled ? "ENABLED" : "DISABLED");
        commandBuilder.set("#StatusLabel.Style.TextColor", isEnabled ? "#4aff7f" : "#ff6b6b");
        commandBuilder.set("#ToggleButton.Text", isEnabled ? "DISABLE" : "ENABLE");
        commandBuilder.set("#PlayerCount.Text", "PLAYERS (" + whitelist.size() + ")");

        buildPlayerList(commandBuilder, eventBuilder, whitelist);

        sendUpdate(commandBuilder, eventBuilder, false);
    }
}
