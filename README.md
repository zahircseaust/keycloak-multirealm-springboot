# Keycloak Multi-Realm with Super Admin — Spring Boot 3 (Repository Pattern)

One Keycloak realm per tenant, with a **super admin** above all tenants who creates and
configures tenant realms from the application.

```
Super Admin            realm "platform", role super-admin
   │  creates, configures, disables, deletes tenants
   ├── Tenant "bti"     realm "bti"   → tenant-admin, user, ...
   ├── Tenant "acme"    realm "acme"  → tenant-admin, user, ...
   └── Tenant ...
```

## Project structure

Folders follow the request flow, top to bottom:

```
src/main/java/com/example/multirealm
├── MultiRealmApplication.java          Entry point
│
├── controller/                         1. REST layer — receives HTTP requests
│   ├── SuperAdminTenantController      /super-admin/tenants/**
│   └── TenantUserController            /api/** for tenant users
│
├── dto/                                2. Data sent in and out of the API
│   ├── request/
│   │   ├── CreateTenantRequest
│   │   ├── TenantConfigRequest
│   │   ├── SmtpConfigRequest
│   │   └── AddTenantAdminRequest
│   └── response/
│       ├── TenantResponse
│       ├── TenantDetailsResponse
│       ├── TenantConfigResponse
│       └── CurrentUserResponse
│
├── service/                            3. Business logic (interfaces)
│   ├── TenantService
│   ├── TenantRegistry
│   └── impl/
│       ├── TenantServiceImpl           create / configure / enable / disable / delete
│       └── TenantRegistryImpl          trusted-realm list
│
├── mapper/                             4. Converts between entity, DTO and Keycloak objects
│   ├── TenantMapper
│   └── RealmConfigMapper
│
├── repository/                         5. Data access (interfaces)
│   ├── TenantRepository                JPA — tenants table
│   ├── KeycloakRealmRepository         Keycloak — realms, clients, roles, users
│   └── impl/
│       └── KeycloakRealmRepositoryImpl Keycloak Admin API calls
│
├── entity/                             6. Database tables
│   ├── Tenant
│   └── TenantStatus
│
├── security/                           Cross-cutting: authentication and tenant isolation
│   ├── SecurityConfig
│   ├── MultiRealmAuthenticationManagerResolver
│   ├── TenantMatchFilter
│   ├── TenantResolver
│   ├── KeycloakRoleConverter
│   └── TenantContext
│
├── exception/                          Cross-cutting: errors → JSON
│   ├── GlobalExceptionHandler
│   ├── TenantNotFoundException
│   ├── TenantConflictException
│   └── TenantProvisioningException
│
├── config/                             Cross-cutting: settings and beans
│   ├── KeycloakProperties
│   ├── TenantProperties
│   └── KeycloakAdminConfig
│
└── constant/
    └── AppConstants                    Role names, client id, header names
```

**Request flow:** Controller → Service → Repository → Database / Keycloak.
Controllers never touch repositories; services never build HTTP responses.

## Who can do what

| Who | Realm | Can do |
|---|---|---|
| Super admin | `platform` | Everything under `/super-admin/**` |
| Tenant admin | own tenant realm | Admin features of their own tenant (`/api/admin/**`) |
| Tenant user | own tenant realm | Normal app features of their own tenant (`/api/**`) |

## How access is enforced

1. `JwtIssuerAuthenticationManagerResolver` reads `iss` from the token.
2. `MultiRealmAuthenticationManagerResolver` accepts it only if the realm is the super admin realm
   or an **ACTIVE** tenant, then verifies signature, expiry and issuer with that realm's keys.
3. `TenantMatchFilter`:
   - `/super-admin/**` — token must come from the super admin realm (realm is checked, not just the role).
   - all other paths — token realm must equal the request's tenant (`X-Tenant-ID` or subdomain), else 403.
4. Disabling a tenant blocks login in Keycloak **and** rejects its existing tokens in the API immediately.

## Keycloak setup (once)

1. Run Keycloak 26 and set `KC_HOSTNAME` so the issuer equals `app.keycloak.base-url`.
2. **Service account** — in the `master` realm create a confidential client `tenant-provisioner`
   with *Client authentication* and *Service accounts* on, and assign the realm role `create-realm`.
   Keycloak gives the creator admin rights over each realm it creates. (To manage realms created
   elsewhere, assign `admin` instead.) Put its secret in `KEYCLOAK_ADMIN_CLIENT_SECRET`.
3. **Super admin realm** — create realm `platform`, role `super-admin`, a public client `web-app`
   (PKCE S256), and your super admin user with the `super-admin` role.

## Run with Docker Compose (Recommended)

The setup includes PostgreSQL 16 (creating `tenants_db` and `keycloak_db`), Keycloak 26, and the Spring Boot application.

```bash
# 1. Start all services
docker compose up -d --build

# 2. View logs
docker compose logs -f app
```

## Run Locally

```bash
# Set PostgreSQL connection and admin secret
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/tenants_db
export SPRING_DATASOURCE_USERNAME=postgres
export SPRING_DATASOURCE_PASSWORD=postgres
export KEYCLOAK_ADMIN_CLIENT_SECRET=xxxx

mvn spring-boot:run
```

## Super admin API

All calls need `Authorization: Bearer <super admin token from realm "platform">`.

| Method | Path | Action |
|---|---|---|
| GET | `/super-admin/tenants` | List tenants with status |
| POST | `/super-admin/tenants` | Create tenant realm with user, role, client, scope settings |
| GET | `/super-admin/tenants/{code}` | Tenant record + live settings from Keycloak |
| PATCH | `/super-admin/tenants/{code}/config` | Change only the settings sent |
| POST | `/super-admin/tenants/{code}/enable` | Re-enable a tenant |
| POST | `/super-admin/tenants/{code}/disable` | Block login and API access |
| POST | `/super-admin/tenants/{code}/admins` | Add another tenant admin |
| GET | `/super-admin/tenants/{code}/client-scopes` | List client scopes for tenant realm |
| POST | `/super-admin/tenants/{code}/client-scopes` | Create client scope in tenant realm |
| GET | `/super-admin/tenants/{code}/clients` | List clients for tenant realm |
| POST | `/super-admin/tenants/{code}/clients` | Create client in tenant realm |
| GET | `/super-admin/tenants/{code}/roles` | List realm roles for tenant |
| POST | `/super-admin/tenants/{code}/roles` | Create realm role in tenant |
| GET | `/super-admin/tenants/{code}/users` | List users and their roles in tenant |
| POST | `/super-admin/tenants/{code}/users` | Create user with roles in tenant |
| DELETE | `/super-admin/tenants/{code}?confirm={code}` | Permanently delete realm and users |

### Create a tenant

```bash
curl -X POST http://localhost:8080/super-admin/tenants \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{
    "code": "bti",
    "adminEmail": "admin@bti.com",
    "tempPassword": "Temp12345",
    "config": {
      "displayName": "Building Technology & Ideas Ltd.",
      "appUrl": "https://bti.app.example.com",
      "registrationAllowed": false,
      "verifyEmail": true,
      "passwordPolicy": "length(10) and digits(1) and upperCase(1) and notUsername",
      "accessTokenLifespan": 300,
      "ssoSessionIdleTimeout": 1800,
      "ssoSessionMaxLifespan": 36000,
      "loginTheme": "keycloak",
      "supportedLocales": ["en", "bn"],
      "defaultLocale": "en",
      "smtp": { "host": "smtp.bti.com", "port": 587, "from": "no-reply@bti.com",
                "fromDisplayName": "BTI", "user": "no-reply@bti.com", "password": "secret",
                "starttls": true },
      "roles": ["marketing", "viewer"],
      "clientScopes": [
        { "name": "read:reports", "description": "Read report scope", "defaultScope": true }
      ],
      "clients": [
        {
          "clientId": "mobile-app",
          "name": "Mobile Application",
          "publicClient": true,
          "redirectUris": ["https://bti.app.example.com/oauth2/callback"]
        }
      ],
      "users": [
        {
          "email": "staff@bti.com",
          "username": "staff",
          "tempPassword": "TempPassword123",
          "roles": ["viewer"]
        }
      ]
    }
  }'
```

Creates, in order: realm `bti` → client `web-app` (public, PKCE, redirect to `appUrl`) →
roles `tenant-admin`, `user` + extra roles → first tenant admin (temporary password).
If any step fails, the half-created realm and the database row are removed.

Only `code`, `adminEmail`, `tempPassword` and `config.appUrl` are required.

### Change settings later

```bash
curl -X PATCH http://localhost:8080/super-admin/tenants/bti/config \
  -H "Authorization: Bearer $SUPER_ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{ "accessTokenLifespan": 600, "rememberMe": true, "roles": ["auditor"] }'
```

### Settings per tenant

| Group | Fields |
|---|---|
| General | `displayName`, `appUrl` (updates the client's redirect URIs and web origins) |
| Login | `registrationAllowed`, `resetPasswordAllowed`, `rememberMe`, `verifyEmail`, `loginWithEmailAllowed`, `bruteForceProtected` |
| Security | `passwordPolicy`, `accessTokenLifespan`, `ssoSessionIdleTimeout`, `ssoSessionMaxLifespan` (seconds) |
| Branding | `loginTheme`, `emailTheme`, `supportedLocales`, `defaultLocale` |
| Email | `smtp`: `host`, `port`, `from`, `fromDisplayName`, `user`, `password`, `ssl`, `starttls` |
| Roles | `roles` — added to the realm; existing roles are never removed |

The SMTP password is never returned; leave it out on update to keep the stored one.

## Tenant users

```bash
curl http://localhost:8080/api/me -H "Authorization: Bearer <bti token>" -H "X-Tenant-ID: bti"
```

React login per tenant (keycloak-js):

```js
const tenant = window.location.hostname.split(".")[0];   // bti.app.example.com -> "bti"
const keycloak = new Keycloak({ url: "http://localhost:8180", realm: tenant, clientId: "web-app" });
await keycloak.init({ onLoad: "login-required", pkceMethod: "S256" });
```

The super admin UI logs in the same way with `realm: "platform"`.

## Before production

- Add `tenant_id` to business tables and filter every query with `TenantContext.get()`.
- Store an audit log of super admin actions in the database (currently written to the app log).
- Require MFA (OTP) for the `platform` realm.
- Set `app.tenants.allowed-origins` to your real domains.
- Plan for scale: hundreds of realms are fine; thousands get heavy for Keycloak.
