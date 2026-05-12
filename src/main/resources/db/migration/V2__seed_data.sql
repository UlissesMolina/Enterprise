-- Seed roles
INSERT INTO roles (name) VALUES ('EMPLOYEE'), ('MANAGER'), ('ADMIN');

-- Seed departments
INSERT INTO departments (name, monthly_budget) VALUES
    ('Engineering', 50000.00),
    ('Sales',       30000.00),
    ('Marketing',   20000.00);
