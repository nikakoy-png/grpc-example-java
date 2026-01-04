# gRPC Examples

Example gRPC services for Spring Boot.
The app implements the server side and Unit Tests the client side.
There are services of the four basic concepts:

 Type                    | Structure                       
-------------------------|---------------------------------
 Unary                   | simple request, simple response 
 Server Streaming        | simple request, stream response 
 Client Streaming        | stream request, simple response 
 Bidirectional Streaming | stream request, stream response 

And there are also services for uploading and downloading files as well as a service which requires authentication.

# Tutorials

Here are some helpful sources to read:

- gRPC: https://grpc.io/
- Spring gRPC: https://docs.spring.io/spring-grpc/reference/
- Medium: https://medium.com/@dinesharney/building-a-simple-grpc-client-and-server-with-spring-boot-3-4672c1e4fab7
- File Upload: https://blog.vinsguru.com/grpc-file-upload-client-streaming/
- Protobuf: https://protobuf.dev/

# CURLs

grpcurl: https://snapcraft.io/install/grpcurl/ubuntu

## List Services

```bash
grpcurl -plaintext localhost:9090 list
```

## Unary

```bash
grpcurl -d '{"number": 2, "string":"abc"}' -plaintext localhost:9090 de.saschaufer.grpc_example.proto.unary.UnaryService.call
```

## Server Streaming

```bash
grpcurl -d '{}' -plaintext localhost:9090 de.saschaufer.grpc_example.proto.server_streaming.ServerStreamingService.call
```

## Client Streaming

```bash
grpcurl -d '{"string":"a"}{"string":"b"}{"string":"c"}' -plaintext localhost:9090 de.saschaufer.grpc_example.proto.client_streaming.ClientStreamingService.call
```

## Bidirectional Streaming

```bash
grpcurl -d '{"string":"ABc"}{"string":"dEf"}{"string":"ghI"}' -plaintext localhost:9090 de.saschaufer.grpc_example.proto.bidirectional_streaming.BidirectionalStreamingService.call
```

## Auth Service

```bash
grpcurl -H 'Authorization: 1234' -d '{"string":"World"}' -plaintext localhost:9090 de.saschaufer.grpc_example.proto.auth.AuthService/call
```

## Bidirectional Streaming: Chat example

This example demonstrates a long-lived bidirectional gRPC stream:
clients send `ChatMessage` and the server broadcasts `ChatEvent` to all connected clients
(including simple system events for join/leave).

### Run server
```bash
mvn spring-boot:run
```

### Run clients

```bash
java -cp "target/classes:target/dependency/*" de.saschaufer.grpc_example.clients.ChatCliClient localhost 9090 mykyta
```

```bash
java -cp "target/classes:target/dependency/*" de.saschaufer.grpc_example.clients.ChatCliClient localhost 9090 sascha
```


