INSERT INTO api_clients (name, api_key_hash)
VALUES
    ('demo-client-01', '7ef01915c9607d617849c617d2700193cdd1e77e03c89055debe57fc4ec1b47f'),
    ('demo-client-02', 'ddc6019cdb45c70075ba2adf27c705726035310a21da4fabf19ea8ed25ab49df'),
    ('demo-client-03', '8e98af291e2bb0c33383a41727ac0e2ac9de061c96f0e5a4a8ee396e8a093a3d'),
    ('demo-client-04', '83e570811c50e77d729254539cc185b00cb8ebd47c78a0da65eaa86b2c09ce26'),
    ('demo-client-05', '0d0d7abc0ff2466ee0ca41ecdcf28f9f97a84dd6af14d464c93b3f17e9a61bc0'),
    ('demo-client-06', '369e1d51b4e1f28d0395a60a104f36eb22fe52a7e399a782812a82df5542695c'),
    ('demo-client-07', 'ab9735e1b6e01654624c5d7ecb6bc28669c4c747da629d2b29948e2226935487'),
    ('demo-client-08', 'fd8635d69b58489dd46c0a9e1e9b8cdea91bbcc2f35d4833f7e260f5c9c17d13'),
    ('demo-client-09', '70dac3a71f3d9d16193b6b2c7fbed71b997486d4e06db16b0529b191db114327'),
    ('demo-client-10', '6c3fddf7b8f2e6a7202a32120b4fa1a8f86a7040cda2b36c764f126ad6c3af92')
ON CONFLICT (name) DO NOTHING;

INSERT INTO api_routes (method, path_pattern, description)
VALUES
    ('GET', '/api/products', '상품 목록 조회'),
    ('GET', '/api/products/{id}', '상품 단건 조회'),
    ('POST', '/api/orders', '주문 생성'),
    ('GET', '/api/reports/sales', '매출 리포트 조회'),
    ('GET', '/api/admin/health-check', '관리자 헬스 체크'),
    ('GET', '/api/unstable', '불안정 API')
ON CONFLICT (method, path_pattern) DO NOTHING;

INSERT INTO api_client_route_permissions (api_client_id, api_route_id)
SELECT c.id, r.id
FROM api_clients c
JOIN api_routes r ON r.method = 'GET' AND r.path_pattern IN ('/api/products', '/api/products/{id}', '/api/unstable')
ON CONFLICT DO NOTHING;

INSERT INTO api_client_route_permissions (api_client_id, api_route_id)
SELECT c.id, r.id
FROM api_clients c
JOIN api_routes r ON r.method = 'POST' AND r.path_pattern = '/api/orders'
WHERE c.name BETWEEN 'demo-client-01' AND 'demo-client-06'
ON CONFLICT DO NOTHING;

INSERT INTO api_client_route_permissions (api_client_id, api_route_id)
SELECT c.id, r.id
FROM api_clients c
JOIN api_routes r ON r.method = 'GET' AND r.path_pattern = '/api/reports/sales'
WHERE c.name BETWEEN 'demo-client-01' AND 'demo-client-03'
ON CONFLICT DO NOTHING;

INSERT INTO api_client_route_permissions (api_client_id, api_route_id)
SELECT c.id, r.id
FROM api_clients c
JOIN api_routes r ON r.method = 'GET' AND r.path_pattern = '/api/admin/health-check'
WHERE c.name = 'demo-client-01'
ON CONFLICT DO NOTHING;
