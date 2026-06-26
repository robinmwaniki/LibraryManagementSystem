-- Sample data for library system
INSERT INTO books (isbn, title, author, publisher, publication_year, category, total_copies, available_copies, shelf_location, created_at, updated_at)
SELECT * FROM (
                  SELECT '978-0-7475-3269-9', 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', 'Bloomsbury', 1997, 'Fantasy', 5, 5, 'A1-001', NOW(), NOW()
                  UNION ALL
                  SELECT '978-0-452-28423-4', '1984', 'George Orwell', 'Secker & Warburg', 1949, 'Dystopian', 3, 3, 'B2-002', NOW(), NOW()
                  UNION ALL
                  SELECT '978-0-061-12008-4', 'To Kill a Mockingbird', 'Harper Lee', 'J.B. Lippincott', 1960, 'Fiction', 4, 4, 'C3-003', NOW(), NOW()
                  UNION ALL
                  SELECT '978-0-141-03435-6', 'The Great Gatsby', 'F. Scott Fitzgerald', 'Scribner', 1925, 'Classic', 2, 2, 'D4-004', NOW(), NOW()
                  UNION ALL
                  SELECT '978-0-316-76948-0', 'The Catcher in the Rye', 'J.D. Salinger', 'Little, Brown', 1951, 'Fiction', 3, 3, 'E5-005', NOW(), NOW()
              ) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM books WHERE isbn = tmp.isbn);

INSERT INTO members (first_name, last_name, email, phone, address, status, max_books_allowed, created_at, updated_at)
SELECT * FROM (
                  SELECT 'John', 'Doe', 'john.doe@email.com', '+1234567890', '123 Main St, City', 'ACTIVE', 5, NOW(), NOW()
                  UNION ALL
                  SELECT 'Jane', 'Smith', 'jane.smith@email.com', '+0987654321', '456 Oak Ave, Town', 'ACTIVE', 5, NOW(), NOW()
                  UNION ALL
                  SELECT 'Bob', 'Johnson', 'bob.johnson@email.com', '+1122334455', '789 Pine Rd, Village', 'ACTIVE', 3, NOW(), NOW()
              ) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM members WHERE email = tmp.email);