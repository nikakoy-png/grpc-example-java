package de.saschaufer.grpc_example.endpoints;

import com.google.protobuf.ByteString;
import de.saschaufer.grpc_example.proto.file_download.File;
import de.saschaufer.grpc_example.proto.file_download.FileDownloadServiceGrpc;
import de.saschaufer.grpc_example.proto.file_download.Metadata;
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
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@PropertySource("classpath:application.yml")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileDownloadServiceTest {

    @TempDir
    private static Path tempDir;

    private static ManagedChannel channel;
    private static FileDownloadServiceGrpc.FileDownloadServiceStub stub;

    @DynamicPropertySource
    static void dynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("files.path", () -> tempDir.toAbsolutePath().toString());
    }

    @BeforeAll
    static void beforeAll() {
        channel = ManagedChannelBuilder.forAddress("localhost", 9090).usePlaintext().build();
        stub = FileDownloadServiceGrpc.newStub(channel);
    }

    @AfterAll
    static void afterAll() {
        channel.shutdown();
    }

    @Test
    void download() throws InterruptedException, IOException {

        final Path testfileSource = tempDir.resolve("testfile-source.txt");
        final Path testfileTarget = tempDir.resolve("testfile-target.txt");
        final String filenameSource = "testfile-source";
        final String extensionSource = "txt";

        createLargeFile(testfileSource);

        final Metadata metaData = Metadata.newBuilder().setName(filenameSource).setType(extensionSource).build();

        final CountDownLatch latch = new CountDownLatch(1);
        final long startTime = System.currentTimeMillis();

        stub.download(metaData, new StreamObserver<>() {

            OutputStream writer = null;

            @Override
            public void onNext(final File file) {
                try {
                    if (writer == null) {
                        writer = getFilePath(testfileTarget);
                    }
                    writeFile(writer, file.getContent());
                } catch (final IOException e) {
                    onError(e);
                }
            }

            @Override
            public void onError(final Throwable throwable) {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                }
                throwable.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (final IOException e) {
                        onError(e);
                    }
                }
                latch.countDown();
            }
        });

        latch.await(10, TimeUnit.SECONDS);

        final long endTime = System.currentTimeMillis();
        System.out.println("Time taken to download file: " + (endTime - startTime) + " ms");

        assertThat(Files.mismatch(testfileSource, testfileTarget), is(-1L));
    }

    private OutputStream getFilePath(final Path path) throws IOException {
        Files.createDirectories(path.getParent());
        if (Files.exists(path)) {
            throw new IOException("File already exists");
        }
        return Files.newOutputStream(path, StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
    }

    private void writeFile(final OutputStream writer, final ByteString content) throws IOException {
        writer.write(content.toByteArray());
        writer.flush();
    }

    private void createLargeFile(final Path path) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(path.toFile(), "rw");
        raf.setLength(1024 * 1024 * 1024); // Creates a 1 GB file
        raf.close();
    }
}
