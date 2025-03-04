/*
 * Copyright 2004-2014 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.lealone.db.auth;

import java.util.HashMap;

import org.lealone.db.Database;
import org.lealone.db.DbObject;
import org.lealone.db.DbObjectBase;
import org.lealone.db.schema.SchemaObject;

/**
 * A right owner (sometimes called principal).
 */
public abstract class RightOwner extends DbObjectBase {

    /**
     * The map of granted roles.
     */
    private HashMap<Role, Right> grantedRoles;

    /**
     * The map of granted rights.
     */
    private HashMap<DbObject, Right> grantedRights;

    protected RightOwner(Database database, int id, String name) {
        super(database, id, name);
    }

    /**
     * Check if a role has been granted for this right owner.
     *
     * @param grantedRole the role
     * @return true if the role has been granted
     */
    public boolean isRoleGranted(Role grantedRole) {
        if (grantedRole == this) {
            return true;
        }
        if (grantedRoles != null) {
            for (Role role : grantedRoles.keySet()) {
                if (role == grantedRole) {
                    return true;
                }
                if (role.isRoleGranted(grantedRole)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Check if a right is already granted to this object or to objects that
     * were granted to this object. The rights for schemas takes
     * precedence over rights of tables, in other words, the rights of schemas
     * will be valid for every each table in the related schema.
     *
     * @param schemaObject the schema object(table or service) to check
     * @param rightMask the right mask to check
     * @return true if the right was already granted
     */
    boolean isRightGrantedRecursive(SchemaObject schemaObject, int rightMask) {
        Right right;
        if (grantedRights != null) {
            if (schemaObject != null) {
                right = grantedRights.get(schemaObject.getSchema());
                if (right != null) {
                    if ((right.getRightMask() & rightMask) == rightMask) {
                        return true;
                    }
                }
            }
            right = grantedRights.get(schemaObject);
            if (right != null) {
                if ((right.getRightMask() & rightMask) == rightMask) {
                    return true;
                }
            }
        }
        if (grantedRoles != null) {
            for (RightOwner role : grantedRoles.keySet()) {
                if (role.isRightGrantedRecursive(schemaObject, rightMask)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Grant a right for the given table. Only one right object per table is
     * supported.
     *
     * @param object the object (table or schema)
     * @param right the right
     */
    public void grantRight(DbObject object, Right right) {
        if (grantedRights == null) {
            grantedRights = new HashMap<>();
        }
        grantedRights.put(object, right);
    }

    /**
     * Revoke the right for the given object (table or schema).
     *
     * @param object the object
     */
    void revokeRight(DbObject object) {
        if (grantedRights == null) {
            return;
        }
        grantedRights.remove(object);
        if (grantedRights.isEmpty()) {
            grantedRights = null;
        }
    }

    /**
     * Grant a role to this object.
     *
     * @param role the role
     * @param right the right to grant
     */
    public void grantRole(Role role, Right right) {
        if (grantedRoles == null) {
            grantedRoles = new HashMap<>();
        }
        grantedRoles.put(role, right);
    }

    /**
     * Remove the right for the given role.
     *
     * @param role the role to revoke
     */
    void revokeRole(Role role) {
        if (grantedRoles == null) {
            return;
        }
        Right right = grantedRoles.get(role);
        if (right == null) {
            return;
        }
        grantedRoles.remove(role);
        if (grantedRoles.isEmpty()) {
            grantedRoles = null;
        }
    }

    /**
     * Get the 'grant schema' right of this object.
     *
     * @param object the granted object (table or schema)
     * @return the right or null if the right has not been granted
     */
    public Right getRightForObject(DbObject object) {
        if (grantedRights == null) {
            return null;
        }
        return grantedRights.get(object);
    }

    /**
     * Get the 'grant role' right of this object.
     *
     * @param role the granted role
     * @return the right or null if the right has not been granted
     */
    public Right getRightForRole(Role role) {
        if (grantedRoles == null) {
            return null;
        }
        return grantedRoles.get(role);
    }
}
