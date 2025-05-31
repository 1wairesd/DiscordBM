package com.wairesd.discordbm.velocity.commandbuilder.channel;

import com.wairesd.discordbm.velocity.network.NettyServer;
import io.netty.channel.Channel;

public class ChannelFinder {
    private final NettyServer nettyServer;

    public ChannelFinder(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    public Channel findChannelForServer(String serverName) {
        for (var entry : nettyServer.getChannelToServerName().entrySet()) {
            if (entry.getValue().equals(serverName)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
