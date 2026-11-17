package com.example.librarymanagementsystem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import com.example.librarymanagementsystem.model.Book;
import com.example.librarymanagementsystem.model.Member;
import com.example.librarymanagementsystem.repository.BookRepository;
import com.example.librarymanagementsystem.repository.MemberRepository;
import java.util.List;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the real PostgreSQL 18 schema migration and Hibernate mapping.
 * The test is skipped automatically on machines without a Docker daemon.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PostgreSqlLiquibaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18-alpine")
                    .withDatabaseName("library_integration")
                    .withUsername("library_test")
                    .withPassword("library_test_password");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.liquibase.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("library.security.admin.username", () -> "integration-admin");
        registry.add("library.security.admin.password", () -> "integration-test-password");
        registry.add("library.notifications.enabled", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void liquibaseCreatesAllApplicationTablesAndHibernateValidatesMappings() {
        assertThat(tableExists("books")).isTrue();
        assertThat(tableExists("members")).isTrue();
        assertThat(tableExists("library_settings")).isTrue();
        assertThat(tableExists("borrowing_records")).isTrue();
        assertThat(tableExists("databasechangelog")).isTrue();

        Integer changesets = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM databasechangelog", Integer.class);
        assertThat(changesets).isEqualTo(4);

        Integer defaultSettings = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM library_settings WHERE id = 1 AND loan_period_days = 14",
                Integer.class);
        assertThat(defaultSettings).isEqualTo(1);
    }

    @Test
    void bookAndMemberSearchQueriesApplyPaginationInsidePostgreSql() {
        List<Book> books = java.util.stream.IntStream.rangeClosed(1, 3).mapToObj(index -> {
            Book book = new Book();
            book.setTitle("Pagination Catalogue " + index);
            book.setAuthor("Pagination Author");
            book.setIsbn("97800000010" + index);
            book.setPublisher("Northwind Press");
            book.setCategory("Testing");
            book.setTotalCopies(2);
            book.setAvailableCopies(2);
            return book;
        }).toList();
        bookRepository.saveAll(books);

        List<Member> members = java.util.stream.IntStream.rangeClosed(1, 3).mapToObj(index -> {
            Member member = new Member();
            member.setFirstName("Pagination");
            member.setLastName("Member " + index);
            member.setEmail("pagination.member." + index + "@example.test");
            member.setPhone("+2547000000" + index);
            member.setStatus(Member.MembershipStatus.ACTIVE);
            return member;
        }).toList();
        memberRepository.saveAll(members);

        Page<Book> firstBookPage = bookRepository.searchBooks("Pagination", "Testing", false,
                PageRequest.of(0, 2, Sort.by("id")));
        Page<Book> secondBookPage = bookRepository.searchBooks("Pagination", "Testing", false,
                PageRequest.of(1, 2, Sort.by("id")));
        assertThat(firstBookPage.getContent()).hasSize(2);
        assertThat(firstBookPage.getTotalElements()).isEqualTo(3);
        assertThat(firstBookPage.getTotalPages()).isEqualTo(2);
        assertThat(secondBookPage.getContent()).hasSize(1);

        // Publisher is a searchable field even when the search term differs from title/author.
        Page<Book> publisherMatches = bookRepository.searchBooks("NORTHWIND", null, false,
                PageRequest.of(0, 10, Sort.by("title")));
        assertThat(publisherMatches.getContent()).hasSize(3);
        assertThat(publisherMatches.getContent()).allMatch(book -> "Northwind Press".equals(book.getPublisher()));

        Page<Member> firstMemberPage = memberRepository.searchMembers("Pagination",
                Member.MembershipStatus.ACTIVE, PageRequest.of(0, 2, Sort.by("id")));
        Page<Member> secondMemberPage = memberRepository.searchMembers("Pagination",
                Member.MembershipStatus.ACTIVE, PageRequest.of(1, 2, Sort.by("id")));
        assertThat(firstMemberPage.getContent()).hasSize(2);
        assertThat(firstMemberPage.getTotalElements()).isEqualTo(3);
        assertThat(firstMemberPage.getTotalPages()).isEqualTo(2);
        assertThat(secondMemberPage.getContent()).hasSize(1);
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = current_schema() AND table_name = ?",
                Integer.class,
                tableName);
        return count != null && count == 1;
    }
}
