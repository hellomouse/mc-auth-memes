package net.hellomouse.authmemes;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class SubnetInfo {
    public InetAddress address;
    @Nullable
    public Integer cidr;

    private SubnetInfo(InetAddress address, @Nullable Integer cidr) {
        this.address = address;
        this.cidr = cidr;
    }

    public static SubnetInfo parseSubnet(String what) throws IllegalArgumentException {
        var cidr_split = what.split("/", 2);
        InetAddress address;
        try {
            // this could potentially hit DNS but I no longer care
            address = InetAddress.getByName(cidr_split[0]);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
        Integer cidr = null;
        if (cidr_split.length == 2) {
            cidr = Integer.parseInt(cidr_split[1], 10);
        }

        return new SubnetInfo(address, cidr);
    }

    public boolean matches(SocketAddress saddr) {
        Objects.requireNonNull(saddr);
        if (saddr instanceof InetSocketAddress saddrInet) {
            var peerAddress = saddrInet.getAddress();
            return this.matches(peerAddress);
        } else {
            // where'd it come from?
            return false;
        }
    }

    public boolean matches(InetAddress other) {
        Objects.requireNonNull(other);
        if (this.cidr == null) {
            return other.equals(this.address);
        } else {
            var selfBytes = this.address.getAddress();
            var otherBytes = other.getAddress();
            if (selfBytes.length != otherBytes.length) {
                return false;
            }

            var bytemask = this.cidr >> 3;
            var bitmask = this.cidr & 0x7;

            for (int idx = 0; idx < selfBytes.length; idx++) {
                var selfByte = selfBytes[idx];
                var otherByte = otherBytes[idx];

                if (idx < bytemask) {
                    // compare full byte
                    if (selfByte != otherByte) {
                        return false;
                    }
                } else if (idx == bytemask) {
                    // compare after mask
                    var shift = 8 - bitmask;
                    if (((selfByte & 0xff) >> shift) != ((otherByte & 0xff) >> shift)) {
                        return false;
                    }
                    // match succeeded
                    break;
                } else {
                    throw new RuntimeException("unreachable");
                }
            }

            return true;
        }
    }
}
