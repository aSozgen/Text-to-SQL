INSERT INTO users (user_id, username, email, password, role, active, created_at)
VALUES (
           '11111111-1111-1111-1111-111111111111',
           'ramazan',
           'ramazan@example.com',
           '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', -- Şifre: password
           'USER',
           true,
           CURRENT_TIMESTAMP
       );

INSERT INTO databases (database_id, user_id, name, description, version, active, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'E-Commerce DB', 'Online satış veritabanı', 1, true, CURRENT_TIMESTAMP);

INSERT INTO tables (table_id, database_id, name, description, active, created_at)
VALUES ('20000000-0000-0000-0000-000000000001', '22222222-2222-2222-2222-222222222222', 'products', 'Ürünlerin tutulduğu tablo', true, CURRENT_TIMESTAMP);

INSERT INTO columns (column_id, table_id, name, data_type, is_primary_key, active, created_at) VALUES
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000001', 'id', 'UUID', true, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000001', 'name', 'VARCHAR(255)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000001', 'price', 'DECIMAL(10,2)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000001', 'stock', 'INTEGER', false, true, CURRENT_TIMESTAMP);

INSERT INTO tables (table_id, database_id, name, description, active, created_at)
VALUES ('20000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', 'orders', 'Sipariş kayıtları', true, CURRENT_TIMESTAMP);

INSERT INTO columns (column_id, table_id, name, data_type, is_primary_key, active, created_at) VALUES
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000002', 'id', 'UUID', true, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000002', 'user_id', 'UUID', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000002', 'total_amount', 'DECIMAL(10,2)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '20000000-0000-0000-0000-000000000002', 'order_date', 'TIMESTAMP', false, true, CURRENT_TIMESTAMP);

INSERT INTO databases (database_id, user_id, name, description, version, active, created_at)
VALUES ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111', 'School DB', 'Öğrenci işleri otomasyonu', 1, true, CURRENT_TIMESTAMP);

INSERT INTO tables (table_id, database_id, name, description, active, created_at)
VALUES ('30000000-0000-0000-0000-000000000001', '33333333-3333-3333-3333-333333333333', 'students', 'Öğrenci listesi', true, CURRENT_TIMESTAMP);

INSERT INTO columns (column_id, table_id, name, data_type, is_primary_key, active, created_at) VALUES
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000001', 'student_no', 'VARCHAR(20)', true, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000001', 'first_name', 'VARCHAR(50)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000001', 'last_name', 'VARCHAR(50)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000001', 'enrollment_year', 'INTEGER', false, true, CURRENT_TIMESTAMP);

INSERT INTO tables (table_id, database_id, name, description, active, created_at)
VALUES ('30000000-0000-0000-0000-000000000002', '33333333-3333-3333-3333-333333333333', 'courses', 'Dersler', true, CURRENT_TIMESTAMP);

INSERT INTO columns (column_id, table_id, name, data_type, is_primary_key, active, created_at) VALUES
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000002', 'code', 'VARCHAR(10)', true, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000002', 'title', 'VARCHAR(100)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '30000000-0000-0000-0000-000000000002', 'credits', 'INTEGER', false, true, CURRENT_TIMESTAMP);

INSERT INTO databases (database_id, user_id, name, description, version, active, created_at)
VALUES ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', 'HR System', 'İnsan kaynakları yönetimi', 1, true, CURRENT_TIMESTAMP);

INSERT INTO tables (table_id, database_id, name, description, active, created_at)
VALUES ('40000000-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'employees', 'Çalışan kayıtları', true, CURRENT_TIMESTAMP);

INSERT INTO columns (column_id, table_id, name, data_type, is_primary_key, active, created_at) VALUES
                                                                                                   (gen_random_uuid(), '40000000-0000-0000-0000-000000000001', 'id', 'BIGINT', true, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '40000000-0000-0000-0000-000000000001', 'full_name', 'VARCHAR(100)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '40000000-0000-0000-0000-000000000001', 'department', 'VARCHAR(50)', false, true, CURRENT_TIMESTAMP),
                                                                                                   (gen_random_uuid(), '40000000-0000-0000-0000-000000000001', 'salary', 'DECIMAL(12,2)', false, true, CURRENT_TIMESTAMP);

INSERT INTO schema_versions (version_id, database_id, version_number, schema_structure, created_at)
VALUES (
           gen_random_uuid(),
           '22222222-2222-2222-2222-222222222222', -- E-Commerce DB ID
           1,
           'products: id (UUID) [PK], name (VARCHAR(255)), price (DECIMAL(10,2)), stock (INTEGER)' || CHR(10) ||
           'orders: id (UUID) [PK], user_id (UUID), total_amount (DECIMAL(10,2)), order_date (TIMESTAMP)',
           CURRENT_TIMESTAMP
       );

INSERT INTO schema_versions (version_id, database_id, version_number, schema_structure, created_at)
VALUES (
           gen_random_uuid(),
           '33333333-3333-3333-3333-333333333333', -- School DB ID
           1,
           'students: student_no (VARCHAR(20)) [PK], first_name (VARCHAR(50)), last_name (VARCHAR(50)), enrollment_year (INTEGER)' || CHR(10) ||
           'courses: code (VARCHAR(10)) [PK], title (VARCHAR(100)), credits (INTEGER)',
           CURRENT_TIMESTAMP
       );

INSERT INTO schema_versions (version_id, database_id, version_number, schema_structure, created_at)
VALUES (
           gen_random_uuid(),
           '44444444-4444-4444-4444-444444444444', -- HR System ID
           1,
           'employees: id (BIGINT) [PK], full_name (VARCHAR(100)), department (VARCHAR(50)), salary (DECIMAL(12,2))',
           CURRENT_TIMESTAMP
       );

INSERT INTO chats (user_id, chat_id, name, active, created_at)
VALUES (
        '11111111-1111-1111-1111-111111111111',
        '55555555-5555-5555-5555-555555555555',
        'First Chat',
        true,
        CURRENT_TIMESTAMP
       )