package nl.geenhonger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class GeenHongerPlugin extends JavaPlugin implements Listener {
    private boolean actief = true;
    private boolean uitzondering = true;
    private final Set<String> werelden = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        laden();
        getServer().getPluginManager().registerEvents(this, this);
        // Vangnet: elke 5 seconden iedereen opnieuw vullen (voor het geval een andere plugin honger veroorzaakt).
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) vul(p);
        }, 100L, 100L);
        getLogger().info("GeenHonger 1.0.0 aan.");
    }

    private void laden() {
        reloadConfig();
        actief = getConfig().getBoolean("actief", true);
        uitzondering = getConfig().getBoolean("uitzondering-permissie", true);
        werelden.clear();
        for (String s : getConfig().getStringList("werelden")) werelden.add(s.trim().toLowerCase(Locale.ROOT));
    }

    /** Geldt dit voor deze speler? */
    private boolean geldt(Player p) {
        if (!actief) return false;
        if (uitzondering && p.hasPermission("geenhonger.honger")) return false;
        if (werelden.isEmpty()) return true;
        World w = p.getWorld();
        return werelden.contains(w.getName().toLowerCase(Locale.ROOT));
    }

    private void vul(Player p) {
        if (!geldt(p)) return;
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        if (p.getFoodLevel() < 20) p.setFoodLevel(20);
        if (p.getSaturation() < 20f) p.setSaturation(20f);
        p.setExhaustion(0f);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (!(e.getEntity() instanceof Player p) || !geldt(p)) return;
        // Eten (omhoog) mag; elke daling wordt tegengehouden.
        if (e.getFoodLevel() < p.getFoodLevel()) e.setCancelled(true);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        vul(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> vul(p), 2L);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent e) {
        vul(e.getPlayer());
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        if (!s.hasPermission("geenhonger.admin")) {
            s.sendMessage("Je hebt hier geen toestemming voor.");
            return true;
        }
        if (a.length > 0 && a[0].equalsIgnoreCase("reload")) {
            laden();
            s.sendMessage("GeenHonger: instellingen herladen.");
        } else {
            s.sendMessage("GeenHonger is " + (actief ? "aan" : "uit") + ", werelden: " + (werelden.isEmpty() ? "alle" : String.join(", ", werelden)) + ".");
        }
        return true;
    }
}
