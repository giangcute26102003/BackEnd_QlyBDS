-- Create roles
INSERT INTO role (role_name, description) VALUES
('ADMIN', 'Administrator with full system access'),
('MANAGER', 'Department manager with limited administration rights'),
('CONSULTANT', 'Property consultant with access to assigned properties and customers'),
('REVIEWER', 'Reviewer who can approve or reject properties'),
('PROPERTY_OWNER', 'Property owner who can manage their own properties')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- Create permissions
INSERT INTO permissions (permission_name, description, category, resource, action) VALUES

-- User management
('USER_VIEW_ALL', 'Permission to view all users', 'User Management', 'USER', 'VIEW_ALL'),
('USER_VIEW_DEPARTMENT', 'Permission to view users in department', 'User Management', 'USER', 'VIEW_DEPARTMENT'),
('USER_VIEW_OWN', 'Permission to view own profile', 'User Management', 'USER', 'VIEW_OWN'),
('USER_CREATE', 'Permission to create users', 'User Management', 'USER', 'CREATE'),
('USER_UPDATE_ALL', 'Permission to update any user', 'User Management', 'USER', 'UPDATE_ALL'),
('USER_UPDATE_DEPARTMENT', 'Permission to update users in department', 'User Management', 'USER', 'UPDATE_DEPARTMENT'),
('USER_UPDATE_OWN', 'Permission to update own profile', 'User Management', 'USER', 'UPDATE_OWN'),
('USER_DELETE', 'Permission to delete users', 'User Management', 'USER', 'DELETE'),
('USER_ACTIVATE_DEACTIVATE', 'Permission to activate/deactivate users', 'User Management', 'USER', 'ACTIVATE_DEACTIVATE'),
('USER_ASSIGN_ROLE', 'Permission to assign roles to users', 'User Management', 'USER', 'ASSIGN_ROLE'),

-- Department management
('DEPARTMENT_VIEW_ALL', 'Permission to view all departments', 'Department Management', 'DEPARTMENT', 'VIEW_ALL'),
('DEPARTMENT_CREATE', 'Permission to create departments', 'Department Management', 'DEPARTMENT', 'CREATE'),
('DEPARTMENT_UPDATE', 'Permission to update departments', 'Department Management', 'DEPARTMENT', 'UPDATE'),
('DEPARTMENT_DELETE', 'Permission to delete departments', 'Department Management', 'DEPARTMENT', 'DELETE'),
('DEPARTMENT_ASSIGN_MANAGER', 'Permission to assign managers to departments', 'Department Management', 'DEPARTMENT', 'ASSIGN_MANAGER'),

-- Property management
('PROPERTY_VIEW_ALL', 'Permission to view all properties', 'Property Management', 'PROPERTY', 'VIEW_ALL'),
('PROPERTY_VIEW_DEPARTMENT', 'Permission to view properties in department', 'Property Management', 'PROPERTY', 'VIEW_DEPARTMENT'),
('PROPERTY_VIEW_ASSIGNED', 'Permission to view assigned properties', 'Property Management', 'PROPERTY', 'VIEW_ASSIGNED'),
('PROPERTY_VIEW_OWNED', 'Permission to view owned properties', 'Property Management', 'PROPERTY', 'VIEW_OWNED'),
('PROPERTY_CREATE', 'Permission to create properties', 'Property Management', 'PROPERTY', 'CREATE'),
('PROPERTY_UPDATE_ALL', 'Permission to update any property', 'Property Management', 'PROPERTY', 'UPDATE_ALL'),
('PROPERTY_UPDATE_DEPARTMENT', 'Permission to update properties in department', 'Property Management', 'PROPERTY', 'UPDATE_DEPARTMENT'),
('PROPERTY_UPDATE_ASSIGNED', 'Permission to update assigned properties', 'Property Management', 'PROPERTY', 'UPDATE_ASSIGNED'),
('PROPERTY_UPDATE_OWNED', 'Permission to update owned properties', 'Property Management', 'PROPERTY', 'UPDATE_OWNED'),
('PROPERTY_DELETE', 'Permission to delete properties', 'Property Management', 'PROPERTY', 'DELETE'),
('PROPERTY_APPROVE', 'Permission to approve properties', 'Property Management', 'PROPERTY', 'APPROVE'),
('PROPERTY_REJECT', 'Permission to reject properties', 'Property Management', 'PROPERTY', 'REJECT'),
('PROPERTY_ASSIGN', 'Permission to assign properties to users', 'Property Management', 'PROPERTY', 'ASSIGN'),

-- Customer management
('CUSTOMER_VIEW_ALL', 'Permission to view all customers', 'Customer Management', 'CUSTOMER', 'VIEW_ALL'),
('CUSTOMER_VIEW_DEPARTMENT', 'Permission to view customers in department', 'Customer Management', 'CUSTOMER', 'VIEW_DEPARTMENT'),
('CUSTOMER_VIEW_ASSIGNED', 'Permission to view assigned customers', 'Customer Management', 'CUSTOMER', 'VIEW_ASSIGNED'),
('CUSTOMER_CREATE', 'Permission to create customers', 'Customer Management', 'CUSTOMER', 'CREATE'),
('CUSTOMER_UPDATE_ALL', 'Permission to update any customer', 'Customer Management', 'CUSTOMER', 'UPDATE_ALL'),
('CUSTOMER_UPDATE_DEPARTMENT', 'Permission to update customers in department', 'Customer Management', 'CUSTOMER', 'UPDATE_DEPARTMENT'),
('CUSTOMER_UPDATE_ASSIGNED', 'Permission to update assigned customers', 'Customer Management', 'CUSTOMER', 'UPDATE_ASSIGNED'),
('CUSTOMER_DELETE', 'Permission to delete customers', 'Customer Management', 'CUSTOMER', 'DELETE'),
('CUSTOMER_ASSIGN', 'Permission to assign customers to users', 'Customer Management', 'CUSTOMER', 'ASSIGN'),

-- Interaction management
('INTERACTION_VIEW_ALL', 'Permission to view all interactions', 'Interaction Management', 'INTERACTION', 'VIEW_ALL'),
('INTERACTION_VIEW_DEPARTMENT', 'Permission to view interactions in department', 'Interaction Management', 'INTERACTION', 'VIEW_DEPARTMENT'),
('INTERACTION_VIEW_OWN', 'Permission to view own interactions', 'Interaction Management', 'INTERACTION', 'VIEW_OWN'),
('INTERACTION_VIEW_PROPERTY_RELATED', 'Permission to view interactions related to owned properties', 'Interaction Management', 'INTERACTION', 'VIEW_PROPERTY_RELATED'),
('INTERACTION_CREATE', 'Permission to create interactions', 'Interaction Management', 'INTERACTION', 'CREATE'),
('INTERACTION_UPDATE_ALL', 'Permission to update any interaction', 'Interaction Management', 'INTERACTION', 'UPDATE_ALL'),
('INTERACTION_UPDATE_OWN', 'Permission to update own interactions', 'Interaction Management', 'INTERACTION', 'UPDATE_OWN'),
('INTERACTION_DELETE', 'Permission to delete interactions', 'Interaction Management', 'INTERACTION', 'DELETE'),

-- Appointment management
('APPOINTMENT_VIEW_ALL', 'Permission to view all appointments', 'Appointment Management', 'APPOINTMENT', 'VIEW_ALL'),
('APPOINTMENT_VIEW_DEPARTMENT', 'Permission to view appointments in department', 'Appointment Management', 'APPOINTMENT', 'VIEW_DEPARTMENT'),
('APPOINTMENT_VIEW_OWN', 'Permission to view own appointments', 'Appointment Management', 'APPOINTMENT', 'VIEW_OWN'),
('APPOINTMENT_VIEW_PROPERTY_RELATED', 'Permission to view appointments related to owned properties', 'Appointment Management', 'APPOINTMENT', 'VIEW_PROPERTY_RELATED'),
('APPOINTMENT_CREATE', 'Permission to create appointments', 'Appointment Management', 'APPOINTMENT', 'CREATE'),
('APPOINTMENT_UPDATE_ALL', 'Permission to update any appointment', 'Appointment Management', 'APPOINTMENT', 'UPDATE_ALL'),
('APPOINTMENT_UPDATE_OWN', 'Permission to update own appointments', 'Appointment Management', 'APPOINTMENT', 'UPDATE_OWN'),
('APPOINTMENT_DELETE', 'Permission to delete appointments', 'Appointment Management', 'APPOINTMENT', 'DELETE'),
('APPOINTMENT_CONFIRM_REJECT', 'Permission to confirm or reject appointments', 'Appointment Management', 'APPOINTMENT', 'CONFIRM_REJECT'),

-- Report management
('REPORT_VIEW_ALL', 'Permission to view all reports', 'Report Management', 'REPORT', 'VIEW_ALL'),
('REPORT_VIEW_DEPARTMENT', 'Permission to view department reports', 'Report Management', 'REPORT', 'VIEW_DEPARTMENT'),
('REPORT_EXPORT', 'Permission to export reports', 'Report Management', 'REPORT', 'EXPORT'),

-- District management
('DISTRICT_VIEW', 'Permission to view districts', 'District Management', 'DISTRICT', 'VIEW'),
('DISTRICT_CREATE', 'Permission to create districts', 'District Management', 'DISTRICT', 'CREATE'),
('DISTRICT_UPDATE', 'Permission to update districts', 'District Management', 'DISTRICT', 'UPDATE'),
('DISTRICT_DELETE', 'Permission to delete districts', 'District Management', 'DISTRICT', 'DELETE'),

-- Audit log
('AUDIT_LOG_VIEW', 'Permission to view audit logs', 'System Management', 'AUDIT', 'LOG_VIEW'),

-- System configuration
('SYSTEM_CONFIG_MANAGE', 'Permission to manage system configuration', 'System Management', 'SYSTEM', 'CONFIG_MANAGE')

ON DUPLICATE KEY UPDATE 
    description = VALUES(description),
    category = VALUES(category),
    resource = VALUES(resource),
    action = VALUES(action);

-- Assign all permissions to ADMIN role
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
CROSS JOIN permissions p
WHERE r.role_name = 'ADMIN'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- Assign MANAGER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permissions p ON p.permission_name IN (
    'USER_VIEW_DEPARTMENT', 'USER_VIEW_OWN', 'USER_UPDATE_DEPARTMENT', 'USER_UPDATE_OWN',
    'PROPERTY_VIEW_ALL', 'PROPERTY_VIEW_DEPARTMENT', 'PROPERTY_CREATE', 'PROPERTY_UPDATE_DEPARTMENT', 'PROPERTY_ASSIGN',
    'CUSTOMER_VIEW_DEPARTMENT', 'CUSTOMER_CREATE', 'CUSTOMER_UPDATE_DEPARTMENT', 'CUSTOMER_ASSIGN',
    'INTERACTION_VIEW_DEPARTMENT', 'INTERACTION_VIEW_OWN', 'INTERACTION_CREATE', 'INTERACTION_UPDATE_OWN',
    'REPORT_VIEW_DEPARTMENT', 'REPORT_EXPORT'
)
WHERE r.role_name = 'MANAGER'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- Assign CONSULTANT permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permissions p ON p.permission_name IN (
    'USER_VIEW_OWN', 'USER_UPDATE_OWN',
    'PROPERTY_VIEW_ASSIGNED', 'PROPERTY_UPDATE_ASSIGNED',
    'CUSTOMER_VIEW_ASSIGNED', 'CUSTOMER_UPDATE_ASSIGNED',
    'INTERACTION_VIEW_OWN', 'INTERACTION_CREATE', 'INTERACTION_UPDATE_OWN',
    'APPOINTMENT_VIEW_OWN', 'APPOINTMENT_CREATE', 'APPOINTMENT_UPDATE_OWN'
)
WHERE r.role_name = 'CONSULTANT'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- Assign REVIEWER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permissions p ON p.permission_name IN (
    'USER_VIEW_OWN', 'USER_UPDATE_OWN',
    'PROPERTY_VIEW_ALL', 'PROPERTY_APPROVE', 'PROPERTY_REJECT',
    'REPORT_VIEW_ALL'
)
WHERE r.role_name = 'REVIEWER'
ON DUPLICATE KEY UPDATE role_id = role_id;

-- Assign PROPERTY_OWNER permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.role_id, p.permission_id
FROM role r
JOIN permissions p ON p.permission_name IN (
    'USER_VIEW_OWN', 'USER_UPDATE_OWN',
    'PROPERTY_VIEW_OWNED', 'PROPERTY_CREATE', 'PROPERTY_UPDATE_OWNED', 'PROPERTY_DELETE',
    'INTERACTION_VIEW_PROPERTY_RELATED',
    'APPOINTMENT_VIEW_PROPERTY_RELATED', 'APPOINTMENT_CONFIRM_REJECT'
)
WHERE r.role_name = 'PROPERTY_OWNER'
ON DUPLICATE KEY UPDATE role_id = role_id; 