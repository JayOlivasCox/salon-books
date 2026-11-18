-- Collapse the 20 starter categories into 9 broader ones for every existing business.
-- Expenses and receipt items move to the new category; the old starter rows are
-- soft-deleted (so phones drop them on the next sync). Categories a user created or
-- renamed are left alone.

create temporary table new_defaults (name text primary key, line text, color text, sort int) on commit drop;
insert into new_defaults values
    ('Supplies & Tools',      'SUPPLIES',            '#8E44AD', 0),
    ('Retail Products',       'COGS',                '#27AE60', 1),
    ('Equipment',             'DEPRECIATION',        '#2C3E50', 2),
    ('Booth Rent',            'RENT_OTHER_PROPERTY', '#E67E22', 3),
    ('Marketing',             'ADVERTISING',         '#E84393', 4),
    ('Fees & Subscriptions',  'COMMISSIONS_FEES',    '#0984E3', 5),
    ('Licenses & Insurance',  'TAXES_LICENSES',      '#C0392B', 6),
    ('Car, Travel & Meals',   'CAR_TRUCK',           '#00A8A8', 7),
    ('Education & Other',     'OTHER',               '#B2BEC3', 8);

create temporary table old_to_new (old_name text primary key, new_name text) on commit drop;
insert into old_to_new values
    ('Color & Chemicals',        'Supplies & Tools'),
    ('Backbar Products',         'Supplies & Tools'),
    ('Tools & Small Equipment',  'Supplies & Tools'),
    ('Laundry & Cleaning',       'Supplies & Tools'),
    ('Retail Products (resale)', 'Retail Products'),
    ('Large Equipment',          'Equipment'),
    ('Booth Rent',               'Booth Rent'),
    ('Advertising & Marketing',  'Marketing'),
    ('Card & App Fees',          'Fees & Subscriptions'),
    ('Booking Software',         'Fees & Subscriptions'),
    ('Phone & Internet',         'Fees & Subscriptions'),
    ('Licenses & Permits',       'Licenses & Insurance'),
    ('Insurance',                'Licenses & Insurance'),
    ('Car & Mileage',            'Car, Travel & Meals'),
    ('Travel',                   'Car, Travel & Meals'),
    ('Meals',                    'Car, Travel & Meals'),
    ('Education & Classes',      'Education & Other'),
    ('Accountant & Legal',       'Education & Other'),
    ('Repairs',                  'Education & Other'),
    ('Other',                    'Education & Other');

-- 1. Add the new categories to every business that doesn't already have one by that name.
insert into expense_category (id, business_id, name, schedule_c_line, color, default_category, sort_order,
                              created_at, updated_at, client_modified_at)
select gen_random_uuid(), b.id, n.name, n.line, n.color, true, n.sort, now(), now(), now()
  from business b
 cross join new_defaults n
 where not exists (select 1 from expense_category c
                    where c.business_id = b.id and c.name = n.name and c.deleted_at is null);

-- Kept categories (Booth Rent) move to their new spot in the list.
update expense_category c
   set sort_order = n.sort, updated_at = now(), client_modified_at = now()
  from new_defaults n
 where c.name = n.name and c.default_category and c.deleted_at is null and c.sort_order <> n.sort;

-- old category id -> new category id, per business
create temporary table category_moves on commit drop as
select oc.id as old_id, nc.id as new_id
  from expense_category oc
  join old_to_new m on m.old_name = oc.name and m.new_name <> m.old_name
  join expense_category nc on nc.business_id = oc.business_id and nc.name = m.new_name
                          and nc.default_category and nc.deleted_at is null
 where oc.default_category and oc.deleted_at is null;

-- 2. Point expenses and receipt items at the new categories.
update expense e
   set category_id = mv.new_id, updated_at = now()
  from category_moves mv
 where e.category_id = mv.old_id;

update expense_item i
   set category_id = mv.new_id
  from category_moves mv
 where i.category_id = mv.old_id;

-- Items changed under these expenses; bump them so phones pull the new item categories.
update expense e
   set updated_at = now()
 where exists (select 1 from expense_item i join category_moves mv on mv.new_id = i.category_id
                where i.expense_id = e.id);

-- 3. Retire the old starter categories.
update expense_category c
   set deleted_at = now(), updated_at = now(), client_modified_at = now()
 where c.id in (select old_id from category_moves);
