package xyz.geik.farmer.integrations.foxclaims;

import arda.morkoc.api.FoxClaimsProvider;
import arda.morkoc.api.model.Claim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import xyz.geik.farmer.Main;
import xyz.geik.farmer.api.FarmerAPI;
import xyz.geik.farmer.api.handlers.FarmerBoughtEvent;
import xyz.geik.farmer.api.managers.FarmerManager;
import xyz.geik.farmer.model.Farmer;
import xyz.geik.farmer.model.user.FarmerPerm;
import xyz.geik.glib.chat.ChatUtils;

import java.util.Map;
import java.util.UUID;

public class FoxClaimsListener implements Listener, FoxClaimsProvider.ClaimCallback {
    @Override
    public void onClaimCreate(Object claimObject, Object playerObject) {
        Player player = (Player) playerObject;
        Claim claim = FoxClaimsProvider.convertObjectToClaim(claimObject);
        if (Main.getConfigFile().getSettings().isAutoCreateFarmer()) {
            Farmer farmer = new Farmer(String.valueOf(claim.id), 0);
            ChatUtils.sendMessage(player, Main.getLangFile().getMessages().getBoughtFarmer());
        }
    }

    @Override
    public void onClaimDelete(Object claimObject, Object playerObject) {
        Player player = (Player) playerObject;
        Claim claim = FoxClaimsProvider.convertObjectToClaim(claimObject);
        if (FarmerManager.farmers.containsKey(String.valueOf(claim.id))){
            FarmerAPI.getFarmerManager().removeFarmer(String.valueOf(claim.id), UUID.fromString(claim.ownerUUID));
        }
    }

    @EventHandler
    public void buyFarmer(FarmerBoughtEvent e) {
        String claimId = e.getFarmer().getRegionID();
        Claim claim = FoxClaimsProvider.getClaimById(Integer.parseInt(claimId));
        e.getFarmer().setRegionID(claimId);

        for (Map.Entry<UUID, Map<String, Object>> entry : claim.members.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Object> perms = entry.getValue();

            for (Map.Entry<String, Object> permEntry : perms.entrySet()) {
                if (permEntry.getKey().equals("owner") && permEntry.getValue().equals(false)) {
                    e.getFarmer().addUser(uuid, Bukkit.getOfflinePlayer(uuid).getName(), FarmerPerm.MEMBER);
                }
            }
        }
    }
}
