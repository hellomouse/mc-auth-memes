package net.hellomouse.authmemes;

import java.util.ArrayList;

public record OfflinePlayer(String username, ArrayList<SubnetInfo> allowedIps) {
    public static OfflinePlayer parseConfigLine(String entry) throws IllegalArgumentException {
        var split1 = entry.split("\\s+", 2);
        if (split1.length != 2) {
            throw new IllegalArgumentException("expected space between username and IP");
        }

        String username = split1[0];
        String addresses_str = split1[1];

        var subnetsStr = addresses_str.split(",");
        var allowedIps = new ArrayList<SubnetInfo>();

        for (var subnetStr : subnetsStr) {
            allowedIps.add(SubnetInfo.parseSubnet(subnetStr));
        }

        return new OfflinePlayer(username, allowedIps);
    }
}
