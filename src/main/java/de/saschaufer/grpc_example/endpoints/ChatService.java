package de.saschaufer.grpc_example.endpoints;

import de.saschaufer.grpc_example.proto.chat.ChatEvent;
import de.saschaufer.grpc_example.proto.chat.ChatMessage;
import de.saschaufer.grpc_example.proto.chat.ChatServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
class ChatService extends ChatServiceGrpc.ChatServiceImplBase {

    private final CopyOnWriteArrayList<StreamObserver<ChatEvent>> subscribers = new CopyOnWriteArrayList<>();

    @Override
    public StreamObserver<ChatMessage> chat(final StreamObserver<ChatEvent> responseObserver) {
        subscribers.add(responseObserver);
        broadcast(system("server", "User joined (" + subscribers.size() + " online)"));

        return new StreamObserver<>() {
            @Override
            public void onNext(final ChatMessage msg) {
                if (msg.getUser().isBlank() || msg.getText().isBlank()) {
                    responseObserver.onError(
                            Status.INVALID_ARGUMENT.withDescription("user/text must be non-empty").asRuntimeException()
                    );
                    return;
                }

                final ChatEvent event = ChatEvent.newBuilder()
                        .setType(ChatEvent.Type.MESSAGE)
                        .setUser(msg.getUser())
                        .setText(msg.getText())
                        .setTsEpochMillis(Instant.now().toEpochMilli())
                        .build();

                log.info("[chat] {}: {}", msg.getUser(), msg.getText());
                broadcast(event);
            }

            @Override
            public void onError(final Throwable t) {
                subscribers.remove(responseObserver);
                broadcast(system("server", "User disconnected (" + subscribers.size() + " online)"));
                log.debug("[chat] client error", t);
            }

            @Override
            public void onCompleted() {
                subscribers.remove(responseObserver);
                broadcast(system("server", "User left (" + subscribers.size() + " online)"));
                responseObserver.onCompleted();
            }
        };
    }

    private void broadcast(final ChatEvent event) {
        for (final StreamObserver<ChatEvent> sub : subscribers) {
            try {
                sub.onNext(event);
            } catch (final Exception e) {
                subscribers.remove(sub);
            }
        }
    }

    private ChatEvent system(final String user, final String text) {
        return ChatEvent.newBuilder()
                .setType(ChatEvent.Type.SYSTEM)
                .setUser(user)
                .setText(text)
                .setTsEpochMillis(Instant.now().toEpochMilli())
                .build();
    }
}
