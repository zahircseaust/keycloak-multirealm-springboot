#!/bin/sh
set -e

echo "==> Waiting for Keycloak to accept credentials..."
until /opt/keycloak/bin/kcadm.sh config credentials \
    --server http://keycloak:8180 \
    --realm master \
    --user admin \
    --password admin; do
  echo "Keycloak not ready yet, retrying in 3s..."
  sleep 3
done

echo "==> Successfully authenticated with Keycloak."

# 1. Configure tenant-provisioner client in master realm
CLIENT_ID=$(/opt/keycloak/bin/kcadm.sh get clients -r master -q clientId=tenant-provisioner --fields id --format csv --noquotes 2>/dev/null || true)

if [ -z "$CLIENT_ID" ]; then
  echo "==> Creating tenant-provisioner client in master realm..."
  /opt/keycloak/bin/kcadm.sh create clients -r master \
    -s clientId=tenant-provisioner \
    -s enabled=true \
    -s serviceAccountsEnabled=true \
    -s clientAuthenticatorType=client-secret \
    -s secret=admin-client-secret \
    -s publicClient=false \
    -s directAccessGrantsEnabled=false \
    -s standardFlowEnabled=false

  echo "==> Assigning create-realm and admin roles to tenant-provisioner..."
  /opt/keycloak/bin/kcadm.sh add-roles -r master \
    --uusername service-account-tenant-provisioner \
    --rolename create-realm
  /opt/keycloak/bin/kcadm.sh add-roles -r master \
    --uusername service-account-tenant-provisioner \
    --rolename admin
else
  echo "==> tenant-provisioner client already exists in master."
fi

# 2. Configure platform realm (Super Admin Realm)
PLATFORM_EXISTS=$(/opt/keycloak/bin/kcadm.sh get realms/platform --fields realm --format csv --noquotes 2>/dev/null || true)

if [ "$PLATFORM_EXISTS" != "platform" ]; then
  echo "==> Creating platform realm..."
  /opt/keycloak/bin/kcadm.sh create realms -s realm=platform -s enabled=true -s displayName="Platform Super Admin"

  echo "==> Creating super-admin role in platform realm..."
  /opt/keycloak/bin/kcadm.sh create roles -r platform -s name=super-admin

  echo "==> Creating web-app client in platform realm..."
  /opt/keycloak/bin/kcadm.sh create clients -r platform \
    -s clientId=web-app \
    -s enabled=true \
    -s publicClient=true \
    -s standardFlowEnabled=true \
    -s directAccessGrantsEnabled=true \
    -s 'redirectUris=["*"]' \
    -s 'webOrigins=["*"]'

  echo "==> Creating superadmin user (superadmin / SuperAdmin123!) in platform realm..."
  /opt/keycloak/bin/kcadm.sh create users -r platform \
    -s username=superadmin \
    -s email=superadmin@platform.local \
    -s firstName=Super \
    -s lastName=Admin \
    -s enabled=true \
    -s emailVerified=true

  /opt/keycloak/bin/kcadm.sh set-password -r platform --username superadmin --new-password SuperAdmin123!
  /opt/keycloak/bin/kcadm.sh add-roles -r platform --uusername superadmin --rolename super-admin
  echo "==> Platform realm and superadmin user setup completed!"
else
  echo "==> Platform realm already exists."
fi

echo "==> Keycloak setup completed successfully!"
