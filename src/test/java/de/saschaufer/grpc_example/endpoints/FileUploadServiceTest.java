package de.saschaufer.grpc_example.endpoints;

import com.google.protobuf.ByteString;
import de.saschaufer.grpc_example.proto.file_upload.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@PropertySource("classpath:application.yml")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileUploadServiceTest {

    @TempDir
    private static Path tempDir;

    private static ManagedChannel channel;
    private static FileUploadServiceGrpc.FileUploadServiceStub stub;

    @DynamicPropertySource
    static void dynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("files.path", () -> tempDir.toAbsolutePath().toString());
    }

    @BeforeAll
    static void beforeAll() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
        stub = FileUploadServiceGrpc.newStub(channel);
    }

    @AfterAll
    static void afterAll() {
        channel.shutdown();
    }

    @Test
    void upload() throws IOException, InterruptedException {

        final TestObserver<FileUploadResponse> testObserver = new TestObserver<>(1, false);

        final Path testfileSource = tempDir.resolve("testfile-source.txt");
        final Path testfileTarget = tempDir.resolve("testfile-target.txt");
        final String filenameTarget = "testfile-target";
        final String extensionTarget = "txt";

        createLargeFile(testfileSource);

        final long startTime = System.currentTimeMillis();
        final StreamObserver<FileUploadRequest> streamObserver = stub.upload(testObserver);

        // Send metadata
        final FileUploadRequest metadata = FileUploadRequest.newBuilder()
                .setMetadata(Metadata.newBuilder()
                        .setName(filenameTarget)
                        .setType(extensionTarget).build())
                .build();
        streamObserver.onNext(metadata);

        // Send bytes
        try (final InputStream inputStream = Files.newInputStream(testfileSource)) {
            final byte[] bytes = new byte[4096];
            int size;
            while ((size = inputStream.read(bytes)) > 0) {
                final FileUploadRequest uploadRequest = FileUploadRequest.newBuilder()
                        .setFile(File.newBuilder().setContent(ByteString.copyFrom(bytes, 0, size)).build())
                        .build();
                streamObserver.onNext(uploadRequest);
            }
        }
        streamObserver.onCompleted();

        testObserver.getLatch().await(10, TimeUnit.SECONDS);

        final long endTime = System.currentTimeMillis();
        System.out.println("Time taken to upload file: " + (endTime - startTime) + " ms");

        assertThat(testObserver.getError(), nullValue());
        assertThat(Files.mismatch(testfileSource, testfileTarget), is(-1L));
    }

    private void createLargeFile(final Path path) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
        raf.setLength(1024 * 1024 * 1024); // Creates a 1 GB file
        raf.close();
    }
}
