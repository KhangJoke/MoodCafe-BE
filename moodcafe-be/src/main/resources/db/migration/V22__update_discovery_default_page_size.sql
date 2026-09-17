-- Migration V22: Update default discovery page size from 12 to 15 for balanced 3-column grid layout
UPDATE system_configurations
SET config_value = '15',
    updated_at = CURRENT_TIMESTAMP
WHERE config_key = 'DISCOVERY_DEFAULT_PAGE_SIZE';
