# Hytale Custom UI - Entwickler Guide

Dieses Dokument fasst alle Erkenntnisse zur Hytale Custom UI Entwicklung zusammen, basierend auf unserer Erfahrung mit dem Whitelist Manager Plugin.

## Inhaltsverzeichnis

1. [Grundlagen](#grundlagen)
2. [UI-Syntax](#ui-syntax)
3. [Schwierigkeitsstufen](#schwierigkeitsstufen)
4. [Was funktioniert / Was nicht](#was-funktioniert--was-nicht)
5. [Fallstricke & Lösungen](#fallstricke--lösungen)
6. [Whitelist Manager - Entwicklungsverlauf](#whitelist-manager---entwicklungsverlauf)

---

## Grundlagen

### Dateistruktur

```
src/main/resources/
└── Common/UI/Custom/
    └── Pages/
        ├── MyPage.ui          # UI Layout Definition
        └── MyEntry.ui         # Wiederverwendbare Komponenten
```

### Java-Seitige Integration

```java
public class MyPage extends InteractiveCustomUIPage<MyPage.EventData> {

    public static class EventData {
        public String action;

        public static final BuilderCodec<EventData> CODEC =
            BuilderCodec.builder(EventData.class, EventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                    (o, v) -> o.action = v, o -> o.action)
                .add()
            .build();
    }

    public MyPage(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, EventData.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder cmd, UIEventBuilder evt, Store<EntityStore> store) {
        cmd.append("Pages/MyPage.ui");

        evt.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#MyButton",
            new EventData().append("Action", "Click")
        );
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventData data) {
        if ("Click".equals(data.action)) {
            // Handle button click
        }
    }
}
```

---

## UI-Syntax

### Gültige Properties (aus offiziellen Assets)

| Property | Beispiel | Beschreibung |
|----------|----------|--------------|
| `Anchor` | `(Width: 100, Height: 50)` | Größe und Position |
| `Background` | `#141c26(0.98)` | Farbe mit Alpha |
| `LayoutMode` | `Top`, `Left`, `Right`, `Bottom` | Kind-Anordnung |
| `Padding` | `(Full: 20)` oder `(Horizontal: 10, Vertical: 5)` | Innenabstand |
| `FlexWeight` | `1` | Flex-Layout Gewichtung |
| `Text` | `"Hello"` | Text-Inhalt |
| `Style` | `(FontSize: 14, TextColor: #ffffff)` | Styling |
| `Visible` | `true` / `false` | Sichtbarkeit |

### UNGÜLTIGE Properties (verursachen Fehler!)

| Property | Fehler |
|----------|--------|
| `ClipChildren` | ❌ Existiert nicht |
| `LayoutMode: Center` | ❌ Nur Top/Left/Right/Bottom |
| `Margin` | ❌ Nicht in offiziellen UIs verwendet |

### Element-Typen

```
Group { }           # Container
Label { }           # Text-Anzeige
TextButton { }      # Klickbarer Button mit Text
Button { }          # Klickbarer Button (Icon)
TextField { }       # Text-Eingabefeld
Sprite { }          # Bild/Animation
```

### Style-Definitionen

```
@MyButtonStyle = TextButtonStyle(
  Default: (Background: #3a7bd5, LabelStyle: (FontSize: 13, TextColor: #ffffff)),
  Hovered: (Background: #4a8be5, LabelStyle: (FontSize: 13, TextColor: #ffffff)),
  Pressed: (Background: #2a6bc5, LabelStyle: (FontSize: 13, TextColor: #ffffff))
);

TextButton #MyButton {
  Style: @MyButtonStyle;
}
```

### Farb-Format

```
#ffffff           # Weiß (volle Deckkraft)
#000000(0.5)      # Schwarz mit 50% Alpha
#141c26(0.98)     # Dunkelblau mit 98% Alpha
```

---

## Schwierigkeitsstufen

### Stufe 1: Statische Anzeige ⭐
Einfache Labels und Buttons ohne dynamischen Inhalt.

```
Group {
  Anchor: (Width: 300, Height: 200);
  Background: #141c26;
  LayoutMode: Top;

  Label {
    Text: "Hello World";
    Style: (FontSize: 20, TextColor: #ffffff);
  }
}
```

**Schwierigkeit:** Sehr einfach
**Funktioniert:** ✅ Zuverlässig

---

### Stufe 2: Button-Interaktion ⭐⭐
Buttons mit Event-Handling.

```java
// Java
evt.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#MyButton",
    new EventData().append("Action", "Click")
);
```

```
// UI
TextButton #MyButton {
  Text: "Click Me";
  Anchor: (Width: 100, Height: 40);
}
```

**Schwierigkeit:** Einfach
**Funktioniert:** ✅ Zuverlässig

---

### Stufe 3: Dynamische Listen ⭐⭐⭐
Listen mit variablen Einträgen via Index-Selektoren.

```java
// Java
cmd.clear("#PlayerList");

int i = 0;
for (Player p : players) {
    String selector = "#PlayerList[" + i + "]";
    cmd.append("#PlayerList", "Pages/PlayerEntry.ui");
    cmd.set(selector + " #Name.Text", p.getName());

    evt.addEventBinding(
        CustomUIEventBindingType.Activating,
        selector + " #RemoveButton",
        new EventData().append("Action", "Remove").append("UUID", p.getUuid()),
        false  // wichtig: false für indexed bindings!
    );
    i++;
}
```

**Schwierigkeit:** Mittel
**Funktioniert:** ✅ Mit korrekter Syntax
**Fallstrick:** `false` als 4. Parameter bei `addEventBinding` für indexierte Elemente!

---

### Stufe 4: UI-Updates ohne Neuladen ⭐⭐⭐
Live-Updates via `sendUpdate()`.

```java
private void refreshPage() {
    UICommandBuilder cmd = new UICommandBuilder();
    UIEventBuilder evt = new UIEventBuilder();

    cmd.set("#StatusLabel.Text", "ENABLED");
    cmd.set("#StatusLabel.Style.TextColor", "#4aff7f");

    // Liste neu aufbauen
    buildPlayerList(cmd, evt, getPlayers());

    sendUpdate(cmd, evt, false);
}
```

**Schwierigkeit:** Mittel
**Funktioniert:** ✅ Zuverlässig

---

### Stufe 5: Seiten-Navigation ⭐⭐⭐⭐
Zwischen verschiedenen Pages wechseln.

```java
// Zu anderer Page navigieren
AddPlayerPage addPage = new AddPlayerPage(playerRef);
player.getPageManager().openCustomPage(ref, store, addPage);

// Page schließen
player.getPageManager().setPage(ref, store, Page.None);
```

**Schwierigkeit:** Mittel-Schwer
**Funktioniert:** ✅ Mit gespeicherten Referenzen
**Fallstrick:** `ref` und `store` müssen als Instanzvariablen gespeichert werden für async Callbacks!

---

### Stufe 6: Text-Eingabe (TextField) ⭐⭐⭐⭐
Formulare mit Benutzereingaben.

```
// UI
TextField #NameInput {
  Anchor: (Height: 34);
  Padding: (Horizontal: 8);
  Background: #0a1119;
  PlaceholderText: "Enter name...";
}
```

```java
// Java - Wert auslesen via EventData
public static class FormData {
    public String action;
    public String playerName;  // Wird via @-Prefix gebunden

    public static final BuilderCodec<FormData> CODEC =
        BuilderCodec.builder(FormData.class, FormData::new)
            .append(new KeyedCodec<>("Action", Codec.STRING), ...)
            .append(new KeyedCodec<>("@PlayerName", Codec.STRING), ...)  // @ = Input-Binding
            .build();
}

// Event-Binding mit Input-Wert
evt.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#ConfirmButton",
    new EventData()
        .append("Action", "Confirm")
        .append("@PlayerName", "#NameInput.Value")  // Liest TextField-Wert
);
```

**Schwierigkeit:** Schwer
**Funktioniert:** ✅ Ohne Common.ui Dependency
**Fallstrick:** `$C.@TextField` (Common.ui) funktioniert nicht in Plugins - direkt `TextField` verwenden!

---

### Stufe 7: Fehler-Anzeige ⭐⭐⭐
Dynamische Fehlermeldungen einblenden.

```java
private void showError(String message) {
    UICommandBuilder cmd = new UICommandBuilder();
    cmd.set("#Error.Visible", true);
    cmd.set("#Error.Text", message);
    sendUpdate(cmd);
}
```

```
// UI
Label #Error {
  Text: "";
  Visible: false;
  Style: (FontSize: 12, TextColor: #ff6b6b);
}
```

**Schwierigkeit:** Einfach
**Funktioniert:** ✅ Zuverlässig

---

## Was funktioniert / Was nicht

### ✅ Funktioniert

| Feature | Anmerkung |
|---------|-----------|
| Statische Labels | Problemlos |
| TextButtons | Mit korrektem Style |
| Dynamische Listen | Mit Index-Selektoren `#List[0]` |
| Live-Updates | Via `sendUpdate()` |
| TextField | Direkt verwenden, ohne Common.ui |
| Seiten-Navigation | Mit gespeicherten ref/store |
| Farbige Hintergründe | Mit Alpha: `#000000(0.5)` |
| Padding/Anchor | Standard-Syntax |
| LayoutMode | Nur Top/Left/Right/Bottom |

### ❌ Funktioniert NICHT

| Feature | Problem | Lösung |
|---------|---------|--------|
| `$C = "../Common.ui"` | Datei nicht in Plugins verfügbar | Styles inline definieren |
| `$C.@TextField` | Common.ui nicht verfügbar | `TextField` direkt verwenden |
| `ClipChildren` | Property existiert nicht | Weglassen |
| `LayoutMode: Center` | Ungültiger Wert | `LayoutMode: Left` + `FlexWeight: 1` Spacer |
| `Margin` | Nicht unterstützt | `Padding` oder Spacer-Groups |
| Async Username-Lookup | `AuthUtil.lookupUuid()` generiert fake UUIDs | Connection-Attempts loggen |

---

## Fallstricke & Lösungen

### 1. "Failed to load CustomUI documents"

**Ursache:** Syntax-Fehler in .ui Datei

**Prüfen:**
- Keine ungültigen Properties (`ClipChildren`, `Margin`)
- Korrekte Klammer-Syntax
- Keine fehlenden Semikolons
- Keine Referenzen auf nicht-existierende Dateien (`$C = "..."`)

**Debugging:**
```bash
# UI-Dateien gegen Assets vergleichen
grep -r "ClipChildren" src/main/resources/  # Sollte nichts finden
```

### 2. Button-Events feuern nicht

**Ursache:** Falscher Selektor oder fehlender 4. Parameter

**Lösung:**
```java
// Für indexierte Elemente: 4. Parameter = false
evt.addEventBinding(
    CustomUIEventBindingType.Activating,
    "#List[0] #Button",
    new EventData().append("Action", "Click"),
    false  // ← WICHTIG!
);
```

### 3. Navigation funktioniert nicht nach async Operation

**Ursache:** `ref` und `store` sind nach async Callback nicht mehr verfügbar

**Lösung:**
```java
public class MyPage extends InteractiveCustomUIPage<...> {
    // Als Instanzvariablen speichern
    private Ref<EntityStore> currentRef;
    private Store<EntityStore> currentStore;

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ...) {
        this.currentRef = ref;
        this.currentStore = store;

        // Jetzt kann async navigiert werden
        someAsyncOperation().thenRun(() -> {
            navigateToOtherPage();  // Verwendet gespeicherte Referenzen
        });
    }
}
```

### 4. TextField-Wert ist leer

**Ursache:** Falsches Binding oder fehlender `@`-Prefix

**Lösung:**
```java
// Im CODEC: @-Prefix für Input-Werte
new KeyedCodec<>("@PlayerName", Codec.STRING)

// Im Event-Binding: Wert referenzieren
new EventData().append("@PlayerName", "#NameInput.Value")
```

### 5. UUID-Lookup liefert falsche UUIDs

**Ursache:** `AuthUtil.lookupUuid()` ist deprecated und generiert Offline-UUIDs

**Lösung:** Connection-Attempts mit echter UUID loggen:
```java
getEventRegistry().register(
    EventPriority.LAST,
    PlayerSetupConnectEvent.class,
    event -> {
        if (event.isCancelled() && event.getReason().contains("not whitelisted")) {
            // event.getUuid() = ECHTE UUID
            // event.getUsername() = Username
            saveAttempt(event.getUuid(), event.getUsername());
        }
    }
);
```

---

## Whitelist Manager - Entwicklungsverlauf

### Phase 1: Basis-UI
- Einfache Page mit Status-Anzeige
- Toggle-Button für Whitelist an/aus
- Player-Liste mit Remove-Buttons
- **Funktionierte:** ✅

### Phase 2: Add Player Form
- TextField für Username-Eingabe
- Problem: `$C.@TextField` nicht verfügbar
- **Lösung:** `TextField` direkt ohne Common.ui verwenden
- **Funktionierte:** ✅ Nach Fix

### Phase 3: Navigation
- Nach Add → zurück zur Übersicht
- Problem: Fenster schloss sich statt Navigation
- **Lösung:** `ref`/`store` als Instanzvariablen speichern
- **Funktionierte:** ✅ Nach Fix

### Phase 4: Username-Auflösung
- Problem: `AuthUtil.lookupUuid()` generiert fake UUIDs
- Spieler konnte nicht zur Whitelist hinzugefügt werden
- **Erkenntnis:** Alle offiziellen Whitelist-Commands haben das gleiche Problem!
- **Funktionierte:** ❌

### Phase 5: Pending Requests System
- Neue Lösung: Connection-Versuche abfangen
- `PlayerSetupConnectEvent` mit `EventPriority.LAST`
- Echte UUID + Username aus Event extrahieren
- In JSON-Datei speichern
- In UI als "Pending Requests" anzeigen
- One-Click Whitelist-Hinzufügung
- **Funktionierte:** ✅

### Phase 6: UI-Syntax Fixes
- "Failed to load CustomUI documents" Fehler
- **Entfernt:** `ClipChildren: true`
- **Entfernt:** `LayoutMode: Center`
- **Entfernt:** `Margin`
- **Entfernt:** `$C = "../Common.ui"`
- **Funktionierte:** ✅ Nach Fixes

### Finale Architektur

```
WhitelistPlugin
├── WhitelistPlugin.java          # Main, Event-Listener
├── commands/
│   └── WhitelistUICommand.java   # /wl Command
├── data/
│   ├── ConnectionAttempt.java    # Pending Request Model
│   └── ConnectionAttemptManager.java  # JSON Persistence
├── pages/
│   ├── WhitelistPage.java        # Hauptseite
│   └── AddPlayerPage.java        # Formular
└── resources/Common/UI/Custom/Pages/
    ├── WhitelistPage.ui
    ├── WhitelistEntry.ui
    ├── AddPlayerPage.ui
    └── PendingEntry.ui
```

---

## Zusammenfassung

### Do's ✅
- Styles inline definieren
- `TextField` direkt verwenden
- Index-Selektoren für Listen: `#List[0]`
- `false` als 4. Parameter bei indexed Event-Bindings
- `ref`/`store` speichern für async Navigation
- Events mit `EventPriority.LAST` für Post-Processing

### Don'ts ❌
- Keine `$C = "..."` Referenzen in Plugins
- Kein `ClipChildren`, `Margin`, `LayoutMode: Center`
- Nicht `AuthUtil.lookupUuid()` für echte UUIDs verwenden
- Keine async Operations ohne gespeicherte Referenzen

---

*Erstellt während der Entwicklung des Whitelist Manager Plugins, Januar 2026*
