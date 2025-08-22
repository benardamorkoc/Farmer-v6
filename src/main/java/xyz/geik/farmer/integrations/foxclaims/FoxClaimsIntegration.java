package xyz.geik.farmer.integrations.foxclaims;

import arda.morkoc.api.FoxClaimsProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class FoxClaimsIntegration {

    private static FoxClaimsListener listener;

    /**
     * FoxClaims entegrasyonunu başlatır
     */
    public static void initialize(JavaPlugin plugin) {
        // FoxClaims plugin'inin mevcut olup olmadığını kontrol et
        if (plugin.getServer().getPluginManager().getPlugin("FoxClaims") == null) {
            return;
        }

        // FoxClaims API'sinin mevcut olup olmadığını kontrol et
        if (!FoxClaimsProvider.isAvailable()) {
            return;
        }

        try {
            // Listener'ı oluştur
            listener = new FoxClaimsListener();

            // Bukkit event listener olarak kaydet
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);

            // FoxClaims callback olarak kaydet
            FoxClaimsProvider.registerCallback(listener);
        } catch (Exception e) {
            plugin.getLogger().warning(e.getMessage());
        }
    }

    /**
     * Listener'ı döndürür (gerekirse)
     */
    public static FoxClaimsListener getListener() {
        return listener;
    }
}
