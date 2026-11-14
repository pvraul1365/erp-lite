SELECT
    t.table_schema AS schema_name,
    t.table_name,
    c.column_name,
    c.ordinal_position AS column_order,
    c.data_type,
    c.character_maximum_length,
    c.numeric_precision,
    c.numeric_scale,
    c.is_nullable,
    c.column_default,
    CASE
        WHEN pk.column_name IS NOT NULL THEN 'YES'
        ELSE 'NO'
    END AS is_primary_key,
    CASE
        WHEN uq.column_name IS NOT NULL THEN 'YES'
        ELSE 'NO'
    END AS is_unique,
    -- *** FOREIGN KEY INFO ***
    fk.tabla_destino,
    fk.columna_destino,
    fk.fk_name,
    fk.on_update,
    fk.on_delete
FROM information_schema.tables t
JOIN information_schema.columns c
    ON t.table_name = c.table_name
    AND t.table_schema = c.table_schema
LEFT JOIN (
    SELECT ku.table_schema, ku.table_name, ku.column_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage ku
        ON tc.constraint_name = ku.constraint_name
        AND tc.table_schema = ku.table_schema
    WHERE tc.constraint_type = 'PRIMARY KEY'
) pk ON c.table_name = pk.table_name
    AND c.column_name = pk.column_name
    AND c.table_schema = pk.table_schema
LEFT JOIN (
    SELECT ku.table_schema, ku.table_name, ku.column_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage ku
        ON tc.constraint_name = ku.constraint_name
        AND tc.table_schema = ku.table_schema
    WHERE tc.constraint_type = 'UNIQUE'
) uq ON c.table_name = uq.table_name
    AND c.column_name = uq.column_name
    AND c.table_schema = uq.table_schema
-- *** LEFT JOIN PARA FOREIGN KEYS ***
LEFT JOIN (
    SELECT
        tc.table_schema,
        tc.table_name AS tabla_origen,
        kcu.column_name AS columna_origen,
        ccu.table_name AS tabla_destino,
        ccu.column_name AS columna_destino,
        tc.constraint_name AS fk_name,
        rc.update_rule AS on_update,
        rc.delete_rule AS on_delete
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    JOIN information_schema.constraint_column_usage ccu
        ON ccu.constraint_name = tc.constraint_name
        AND ccu.table_schema = tc.table_schema
    JOIN information_schema.referential_constraints rc
        ON tc.constraint_name = rc.constraint_name
    WHERE tc.constraint_type = 'FOREIGN KEY'
) fk ON c.table_name = fk.tabla_origen
    AND c.column_name = fk.columna_origen
    AND c.table_schema = fk.table_schema
WHERE t.table_schema = 'public'
  AND t.table_type = 'BASE TABLE'
ORDER BY t.table_name, c.ordinal_position;