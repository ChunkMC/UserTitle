package com.chunkmc.usertitle.grpc;

import com.chunkmc.usertitle.UserTitlePlugin;
import com.chunkmc.usertitle.model.TitleConfig;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

import chunkmc.v1.UserTitleCallbackServiceGrpc;
import chunkmc.v1.NotifyTitleChangedRequest;
import chunkmc.v1.NotifyTitleChangedResponse;
import chunkmc.v1.TitleChangeType;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CallbackServer {

    private final int port;
    private final UserTitlePlugin plugin;
    private Server server;

    public CallbackServer(int port, UserTitlePlugin plugin) {
        this.port = port;
        this.plugin = plugin;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new UserTitleCallbackServiceImpl(plugin))
                .build()
                .start();
        plugin.getLogger().info("ChunkMC UserTitle gRPC callback server started on port " + port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("Shutting down UserTitle gRPC callback server...");
            CallbackServer.this.stop();
            System.err.println("UserTitle gRPC callback server shut down.");
        }));
    }

    public void stop() {
        if (server != null) {
            try {
                server.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public boolean isRunning() {
        return server != null && !server.isShutdown();
    }

    private static class UserTitleCallbackServiceImpl extends UserTitleCallbackServiceGrpc.UserTitleCallbackServiceImplBase {

        private final UserTitlePlugin plugin;

        UserTitleCallbackServiceImpl(UserTitlePlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public void notifyTitleChanged(NotifyTitleChangedRequest request,
                                       StreamObserver<NotifyTitleChangedResponse> responseObserver) {
            String uuid = request.getUuid();
            String titleId = request.getTitleId();
            TitleChangeType changeType = request.getChangeType();

            plugin.getLogger().info("Received title change callback: uuid=" + uuid
                    + ", titleId=" + titleId + ", type=" + changeType);

            // Update cache
            UUID playerUuid = UUID.fromString(uuid);
            if (changeType == TitleChangeType.TITLE_CHANGE_TYPE_EQUIPPED) {
                plugin.getTitleCache().setActiveTitle(playerUuid, titleId);
            } else if (changeType == TitleChangeType.TITLE_CHANGE_TYPE_UNEQUIPPED) {
                plugin.getTitleCache().setActiveTitle(playerUuid, null);
            } else if (changeType == TitleChangeType.TITLE_CHANGE_TYPE_ADDED) {
                plugin.getTitleCache().addOwnedTitle(playerUuid, titleId);
            }

            // Update player display if online
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.updatePlayerDisplayName(player);
                    plugin.updatePlayerTabList(player);
                });
            }

            responseObserver.onNext(NotifyTitleChangedResponse.newBuilder().build());
            responseObserver.onCompleted();
        }
    }
}
