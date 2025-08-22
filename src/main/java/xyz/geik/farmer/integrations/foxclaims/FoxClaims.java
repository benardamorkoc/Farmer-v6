package xyz.geik.farmer.integrations.foxclaims;

import arda.morkoc.api.FoxClaimsProvider;
import arda.morkoc.api.model.Claim;
import org.bukkit.Location;
import xyz.geik.farmer.integrations.Integrations;

import java.util.UUID;

public class FoxClaims extends Integrations {
    public FoxClaims() {super(new FoxClaimsListener());}

    @Override
    public UUID getOwnerUUID(String regionID) {
        Claim claim = FoxClaimsProvider.getClaimById(Integer.parseInt(regionID));
        if (claim != null) {
            return UUID.fromString(claim.ownerUUID);
        }

        return null;
    }

    @Override
    public UUID getOwnerUUID(Location location) {
        Claim claim = FoxClaimsProvider.getClaimAtLocation(location);
        if (claim != null) {
            return UUID.fromString(claim.ownerUUID);
        }

        return null;
    }

    @Override
    public String getRegionID(Location location) {
        Claim claim = FoxClaimsProvider.getClaimAtLocation(location);
        if (claim != null) {
            return String.valueOf(claim.id);
        }

        return null;
    }
}