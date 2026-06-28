package com.chunkmc.usertitle.grpc;

import com.chunkmc.usertitle.UserTitlePlugin;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import chunkmc.v1.PluginServiceGrpc;
import chunkmc.v1.GetPlayerTitleRequest;
import chunkmc.v1.GetPlayerTitleResponse;
import chunkmc.v1.SetPlayerTitleRequest;
import chunkmc.v1.SetPlayerTitleResponse;
import chunkmc.v1.GetPlayerOwnedTitlesRequest;
import chunkmc.v1.GetPlayerOwnedTitlesResponse;
import chunkmc.v1.AddPlayerTitleRequest;
import chunkmc.v1.AddPlayerTitleResponse;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BackendClient {

    private final UserTitlePlugin plugin;
    private final ManagedChannel channel;
    private final PluginServiceGrpc.PluginServiceBlockingStub blockingStub;

    public BackendClient(String host, int port, UserTitlePlugin plugin) {
        this.plugin = plugin;
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        this.blockingStub = PluginServiceGrpc.newBlockingStub(channel);
    }

    public String getPlayerTitle(String uuid) {
        try {
            GetPlayerTitleRequest request = GetPlayerTitleRequest.newBuilder()
                    .setUuid(uuid)
                    .build();
            GetPlayerTitleResponse response = blockingStub.getPlayerTitle(request);
            String titleId = response.getTitleId();
            return titleId.isEmpty() ? null : titleId;
        } catch (StatusRuntimeException e) {
            plugin.getLogger().warning("Failed to get player title: " + e.getStatus());
            return null;
        }
    }

    public boolean setPlayerTitle(String uuid, String titleId) {
        try {
            SetPlayerTitleRequest request = SetPlayerTitleRequest.newBuilder()
                    .setUuid(uuid)
                    .setTitleId(titleId != null ? titleId : "")
                    .build();
            SetPlayerTitleResponse response = blockingStub.setPlayerTitle(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            plugin.getLogger().warning("Failed to set player title: " + e.getStatus());
            return false;
        }
    }

    public List<String> getPlayerOwnedTitles(String uuid) {
        try {
            GetPlayerOwnedTitlesRequest request = GetPlayerOwnedTitlesRequest.newBuilder()
                    .setUuid(uuid)
                    .build();
            GetPlayerOwnedTitlesResponse response = blockingStub.getPlayerOwnedTitles(request);
            return response.getTitleIdsList();
        } catch (StatusRuntimeException e) {
            plugin.getLogger().warning("Failed to get player owned titles: " + e.getStatus());
            return Collections.emptyList();
        }
    }

    public boolean addPlayerTitle(String uuid, String titleId) {
        try {
            AddPlayerTitleRequest request = AddPlayerTitleRequest.newBuilder()
                    .setUuid(uuid)
                    .setTitleId(titleId)
                    .build();
            AddPlayerTitleResponse response = blockingStub.addPlayerTitle(request);
            return response.getSuccess();
        } catch (StatusRuntimeException e) {
            plugin.getLogger().warning("Failed to add player title: " + e.getStatus());
            return false;
        }
    }

    public void shutdown() {
        if (channel != null) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                channel.shutdownNow();
            }
        }
    }
}
