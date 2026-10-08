-- Authorized sample menu for The Workshop Specialty Coffee on MoodCafe server.
-- Manual script: deliberately outside Flyway migration/seed locations.
-- Inserts only. Existing menu rows are never updated or deleted.
-- Representative sample images, not the cafe's official product photography.
-- Run with psql -v ON_ERROR_STOP=1, UTF-8, against the intended server database.

BEGIN;
SET LOCAL lock_timeout = '10s';
SET LOCAL statement_timeout = '60s';

DO $seed$
DECLARE
    target_store uuid := '582b75bc-1ced-43c1-a849-be08350f98d3';
    target_name text;
    category_row record;
    item_row record;
    chosen_category uuid;
    seed_category_id uuid;
    seed_item_id uuid;
    categories_added integer := 0;
    items_added integer := 0;
    category_matches integer;
    next_order integer;
BEGIN
    SELECT name INTO target_name
    FROM stores
    WHERE store_id = target_store AND NOT is_deleted
    FOR UPDATE;

    IF target_name IS DISTINCT FROM 'The Workshop Specialty Coffee' THEN
        RAISE EXCEPTION 'Wrong or missing target store; aborting Workshop seed';
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('workshop-menu-seed:' || target_store::text));

    FOR category_row IN
        SELECT * FROM (VALUES
            ('coffee', 'Cà phê'),
            ('tea', 'Trà'),
            ('cake', 'Bánh')
        ) AS category_seed(slug, name)
    LOOP
        chosen_category := NULL;
        SELECT count(*) INTO category_matches
        FROM store_menu_categories
        WHERE store_id = target_store AND NOT is_deleted
          AND lower(btrim(name)) = lower(category_row.name);
        IF category_matches > 1 THEN
            RAISE EXCEPTION 'Duplicate live category name: %; resolve before seeding', category_row.name;
        END IF;

        SELECT category_id INTO chosen_category
        FROM store_menu_categories
        WHERE store_id = target_store AND NOT is_deleted
          AND lower(btrim(name)) = lower(category_row.name);

        seed_category_id := md5('moodcafe-workshop-menu-v1:' || target_store::text || ':category:' || category_row.slug)::uuid;
        IF chosen_category IS NULL THEN
            -- Respect a previously seeded category that was deliberately deleted.
            IF EXISTS (SELECT 1 FROM store_menu_categories WHERE category_id = seed_category_id) THEN
                RAISE NOTICE 'Skipping previously seeded/deleted category %', category_row.name;
                CONTINUE;
            END IF;
            SELECT COALESCE(max(display_order), -1) + 1 INTO next_order
            FROM store_menu_categories WHERE store_id = target_store AND NOT is_deleted;
            INSERT INTO store_menu_categories
                (category_id, store_id, name, display_order, is_deleted, created_at, updated_at)
            VALUES
                (seed_category_id, target_store, category_row.name, next_order, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            chosen_category := seed_category_id;
            categories_added := categories_added + 1;
        END IF;

        FOR item_row IN
            SELECT * FROM (VALUES
                ('coffee', 'espresso', 'Espresso', 40000, 'Cà phê espresso đậm vị.', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80'),
                ('coffee', 'americano', 'Americano', 45000, 'Espresso pha nước, phục vụ nóng hoặc lạnh.', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80'),
                ('coffee', 'latte', 'Latte', 55000, 'Espresso kết hợp sữa tươi và lớp bọt mịn.', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80'),
                ('coffee', 'cappuccino', 'Cappuccino', 55000, 'Cà phê cùng sữa nóng và bọt sữa.', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80'),
                ('coffee', 'cold-brew', 'Cold Brew', 60000, 'Cà phê ủ lạnh vị thanh nhẹ.', 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?auto=format&fit=crop&w=800&q=80'),
                ('tea', 'peach-tea', 'Trà đào', 50000, 'Trà trái cây hương đào.', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=800&q=80'),
                ('tea', 'lychee-tea', 'Trà vải', 50000, 'Trà trái cây hương vải.', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=800&q=80'),
                ('tea', 'jasmine-tea', 'Trà lài', 45000, 'Trà lài thơm nhẹ.', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=800&q=80'),
                ('tea', 'oolong-tea', 'Trà ô long', 50000, 'Trà ô long vị dịu.', 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=800&q=80'),
                ('cake', 'chocolate-cake', 'Bánh chocolate', 65000, 'Bánh chocolate ăn kèm đồ uống.', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=80'),
                ('cake', 'cheesecake', 'Cheesecake', 65000, 'Bánh phô mai mềm mịn.', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=80'),
                ('cake', 'tiramisu', 'Tiramisu', 70000, 'Bánh vị cà phê và cacao.', 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=800&q=80')
            ) AS item_seed(category_slug, slug, name, price, description, image_url)
            WHERE category_slug = category_row.slug
        LOOP
            seed_item_id := md5('moodcafe-workshop-menu-v1:' || target_store::text || ':item:' || item_row.slug)::uuid;
            -- Skip deterministic IDs (including deleted rows) and live names.
            IF EXISTS (SELECT 1 FROM store_menu_items WHERE item_id = seed_item_id)
               OR EXISTS (
                   SELECT 1 FROM store_menu_items
                   WHERE store_id = target_store AND NOT is_deleted
                     AND lower(btrim(name)) = lower(item_row.name)
               ) THEN
                CONTINUE;
            END IF;
            INSERT INTO store_menu_items
                (item_id, store_id, category_id, name, description, price, image_url,
                 is_available, is_deleted, created_at, updated_at)
            VALUES
                (seed_item_id, target_store, chosen_category, item_row.name,
                 '[Dữ liệu mẫu] ' || item_row.description, item_row.price, item_row.image_url,
                 true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
            items_added := items_added + 1;
        END LOOP;
    END LOOP;
    RAISE NOTICE 'Workshop seed: % categories inserted, % items inserted', categories_added, items_added;
END
$seed$;

SELECT c.name AS category, count(i.item_id) AS item_count,
       count(i.item_id) FILTER (WHERE i.image_url IS NOT NULL AND btrim(i.image_url) <> '') AS items_with_images
FROM store_menu_categories c
LEFT JOIN store_menu_items i ON i.category_id = c.category_id AND NOT i.is_deleted
WHERE c.store_id = '582b75bc-1ced-43c1-a849-be08350f98d3' AND NOT c.is_deleted
GROUP BY c.category_id, c.name, c.display_order
ORDER BY c.display_order, c.name;

COMMIT;
