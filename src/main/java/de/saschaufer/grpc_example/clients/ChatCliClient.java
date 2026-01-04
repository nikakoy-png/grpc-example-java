package de.saschaufer.grpc_example.clients;

import de.saschaufer.grpc_example.proto.chat.ChatEvent;
import de.saschaufer.grpc_example.proto.chat.ChatMessage;
import de.saschaufer.grpc_example.proto.chat.ChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ChatCliClient {

    static void main(final String[] args) throws Exception {
        final String host = args.length > 0 ? args[0] : "localhost";
        final int port = args.length > 1 ? Integer.parseInt(args[1]) : 9090;
        final String user = args.length > 2 ? args[2] : "mykyta";

        final ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        final ChatServiceGrpc.ChatServiceStub stub = ChatServiceGrpc.newStub(channel);

        final CountDownLatch done = new CountDownLatch(1);

        final StreamObserver<ChatEvent> inbound = new StreamObserver<>() {

            @Override
            public void onNext(final ChatEvent e) {
                final String ts = Instant.ofEpochMilli(e.getTsEpochMillis()).toString();
                final String prefix = e.getType() == ChatEvent.Type.SYSTEM ? "[system]" : "[" + e.getUser() + "]";
                System.out.println(ts + " " + prefix + " " + e.getText());
            }

            @Override
            public void onError(final Throwable t) {
                System.err.println("stream error: " + t.getMessage());
                done.countDown();
            }

            @Override
            public void onCompleted() {
                System.out.println("server closed stream");
                done.countDown();
            }
        };

        final StreamObserver<ChatMessage> outbound = stub.chat(inbound);

        System.out.println("Connected. Type messages, '/quit' to exit.");

        try (final BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                final String line = br.readLine();

                if (line == null || line.equalsIgnoreCase("/quit")){
                    break;
                }

                outbound.onNext(ChatMessage.newBuilder()
                        .setUser(user)
                        .setText(line)
                        .build());
            }
        } finally {
            outbound.onCompleted();
            done.await(3, TimeUnit.SECONDS);
            channel.shutdownNow();
        }
    }
}
