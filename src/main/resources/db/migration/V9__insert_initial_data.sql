
INSERT INTO roles (code, name)
VALUES
    ('ADMIN', 'Administrator'),
    ('CUSTOMER', 'Customer'),
    ('SALES_STAFF', 'Sales Staff'),
    ('WAREHOUSE_MANAGER', 'Warehouse Manager'),
    ('WAREHOUSE_STAFF', 'Warehouse Staff');

INSERT INTO warehouses (code, name, address, province)
VALUES
    (
        'WH-HN-01',
        'Kho Hà Nội',
        'Khu vực Hà Nội',
        'Hà Nội'
    ),
    (
        'WH-HCM-01',
        'Kho TP. Hồ Chí Minh',
        'Khu vực TP. Hồ Chí Minh',
        'TP. Hồ Chí Minh'
    );

INSERT INTO categories (name, slug, description)
VALUES (
    'Electronics',
    'electronics',
    'Electronic products and devices'
);
