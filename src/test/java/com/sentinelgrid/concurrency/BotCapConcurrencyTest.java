package com.sentinelgrid.concurrency;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.sentinelgrid.dto.request.CreateCommentRequest;
import com.sentinelgrid.entity.Bot;
import com.sentinelgrid.entity.Post;
import com.sentinelgrid.entity.User;
import com.sentinelgrid.enums.AuthorType;
import com.sentinelgrid.exception.RateLimitExceededException;
import com.sentinelgrid.repository.BotRepository;
import com.sentinelgrid.repository.CommentRepository;
import com.sentinelgrid.repository.PostRepository;
import com.sentinelgrid.repository.UserRepository;
import com.sentinelgrid.service.interfaces.CommentService;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BotCapConcurrencyTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
        DockerImageName.parse("postgres:15-alpine"))
        .withDatabaseName("sentinelgrid_test")
        .withUsername("sentinel_test")
        .withPassword("sentinel_test_secret");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>(
        DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379)
        .withCommand("redis-server", "--requirepass", "redis_test_secret");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "redis_test_secret");
    }

    @Autowired private CommentService commentService;
    @Autowired private PostRepository postRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private BotRepository botRepository;
    @Autowired private CommentRepository commentRepository;

    private Post testPost;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        botRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(
            User.builder().username("post_owner").premium(false).build());

        testPost = postRepository.save(Post.builder()
            .authorId(owner.getId())
            .authorType(AuthorType.HUMAN)
            .content("Target post for concurrency stress test")
            .build());
    }

    @Test
    void exactly100BotCommentsPersisted_under200ConcurrentRequests() throws InterruptedException {
        int totalRequests = 200;
        int expectedCap   = 100;

        List<Bot> bots = new ArrayList<>(totalRequests);
        for (int i = 0; i < totalRequests; i++) {
            bots.add(botRepository.save(Bot.builder()
                .name("stress-bot-" + i)
                .personaDescription("Stress bot " + i)
                .build()));
        }

        ExecutorService executor    = Executors.newFixedThreadPool(totalRequests);
        CountDownLatch  startGate   = new CountDownLatch(1);
        CountDownLatch  doneGate    = new CountDownLatch(totalRequests);
        AtomicInteger   accepted    = new AtomicInteger(0);
        AtomicInteger   rejected    = new AtomicInteger(0);
        UUID            postId      = testPost.getId();

        List<Future<?>> futures = new ArrayList<>(totalRequests);
        for (int i = 0; i < totalRequests; i++) {
            final Bot bot = bots.get(i);
            futures.add(executor.submit(() -> {
                try {
                    startGate.await();
                    commentService.createComment(postId, new CreateCommentRequest(
                        null, bot.getId(), AuthorType.BOT, "Concurrent bot reply"));
                    accepted.incrementAndGet();
                } catch (RateLimitExceededException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                } finally {
                    doneGate.countDown();
                }
            }));
        }

        startGate.countDown();
        boolean completed = doneGate.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("All threads should complete within 60 seconds").isTrue();

        long dbBotCommentCount = commentRepository.countByPostIdAndAuthorType(postId, AuthorType.BOT);

        assertThat(accepted.get())
            .as("Exactly 100 requests must be accepted")
            .isEqualTo(expectedCap);

        assertThat(rejected.get())
            .as("Exactly 100 requests must be rejected")
            .isEqualTo(totalRequests - expectedCap);

        assertThat(dbBotCommentCount)
            .as("Database must contain exactly 100 bot comments — no more, no less")
            .isEqualTo(expectedCap);

        assertThat(accepted.get() + rejected.get())
            .as("Total accepted + rejected must equal total requests")
            .isEqualTo(totalRequests);
    }
}
