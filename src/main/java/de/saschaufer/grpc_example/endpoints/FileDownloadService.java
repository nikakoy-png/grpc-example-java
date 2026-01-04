package de.saschaufer.grpc_example.endpoints;

import com.google.protobuf.ByteString;
import de.saschaufer.grpc_example.config.AppProperties;
import de.saschaufer.grpc_example.proto.file_download.File;
import de.saschaufer.grpc_example.proto.file_download.FileDownloadServiceGrpc;
import de.saschaufer.grpc_example.proto.file_download.Metadata;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileDownloadService extends FileDownloadServiceGrpc.FileDownloadServiceImplBase {

    private final AppProperties appProperties;

    @Override
    public void download(final Metadata metaData, final StreamObserver<File> responseObserver) {

        final String fileName = "%s.%s".formatted(metaData.getName(), metaData.getType());
        final Path filePath = appProperties.filesPath().resolve(fileName);

        if (!Files.exists(filePath)) {
            responseObserver.onError(new Exception("File not found"));
        }

        try (final InputStream inputStream = Files.newInputStream(filePath)) {
            final byte[] bytes = new byte[4096];
            int size;
            while ((size = inputStream.read(bytes)) > 0) {
                final File file = File.newBuilder()
                        .setContent(ByteString.copyFrom(bytes, 0, size))
                        .build();
                responseObserver.onNext(file);
            }
        } catch (final IOException e) {
            responseObserver.onError(new Exception("Couldn't read file", e));
        }

        responseObserver.onCompleted();
    }
}
