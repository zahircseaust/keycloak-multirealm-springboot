-- Initialize databases for Multi-Realm application and Keycloak
CREATE DATABASE tenants_db;
CREATE DATABASE keycloak_db;

GRANT ALL PRIVILEGES ON DATABASE tenants_db TO postgres;
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO postgres;
