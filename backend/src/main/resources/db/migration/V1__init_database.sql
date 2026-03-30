-- 1. Tabele niezależne (Słowniki / Wymiary i Użytkownicy)
CREATE TABLE users (
                       id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE dim_patient (
                             id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                             patient_hash VARCHAR(64) NOT NULL,
                             birth_year INT DEFAULT NULL,
                             gender ENUM('M', 'F') DEFAULT NULL
);

CREATE TABLE dim_test_type (
                               id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                               test_code VARCHAR(10) NOT NULL,
                               test_name VARCHAR(100) NOT NULL,
                               category_name VARCHAR(50) DEFAULT NULL,
                               norm_min DECIMAL(10,2) DEFAULT NULL,
                               norm_max DECIMAL(10,2) DEFAULT NULL,
                               unit VARCHAR(20) DEFAULT NULL
);

CREATE TABLE dim_facility (
                              id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                              facility_name VARCHAR(100) NOT NULL,
                              city VARCHAR(100) DEFAULT NULL,
                              province VARCHAR(100) DEFAULT NULL
);

-- 2. Tabela historii plików (Zależy od users)
CREATE TABLE files_history (
                               id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               filename VARCHAR(255) NOT NULL,
                               status ENUM('SUCCESS', 'PARTIAL_SUCCESS', 'ERROR') NOT NULL,
                               success_count INT DEFAULT 0,
                               error_count INT DEFAULT 0,
                               upload_time TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_file_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 3. Tabela błędów (Zależy od files_history)
CREATE TABLE processing_errors (
                                   id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                   file_id BIGINT NOT NULL,
                                   error_row_number INT DEFAULT NULL,
                                   error_message TEXT DEFAULT NULL,
                                   raw_line_data TEXT DEFAULT NULL,
                                   CONSTRAINT fk_error_file FOREIGN KEY (file_id) REFERENCES files_history (id) ON DELETE CASCADE
);

-- 4. Tabela główna z wynikami (Zależy od plików i wszystkich wymiarów)
CREATE TABLE fact_test_results (
                                   id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                                   file_id BIGINT NOT NULL,
                                   patient_id BIGINT NOT NULL,
                                   test_id BIGINT NOT NULL,
                                   facility_id BIGINT NOT NULL,
                                   result_value DECIMAL(10,2) DEFAULT NULL,
                                   is_abnormal TINYINT(1) DEFAULT NULL,
                                   CONSTRAINT fk_fact_facility FOREIGN KEY (facility_id) REFERENCES dim_facility (id),
                                   CONSTRAINT fk_fact_file FOREIGN KEY (file_id) REFERENCES files_history (id) ON DELETE CASCADE,
                                   CONSTRAINT fk_fact_patient FOREIGN KEY (patient_id) REFERENCES dim_patient (id),
                                   CONSTRAINT fk_fact_test FOREIGN KEY (test_id) REFERENCES dim_test_type (id)
);