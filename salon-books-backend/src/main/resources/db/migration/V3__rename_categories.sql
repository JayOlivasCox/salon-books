-- Final names for the nine starter categories. Renames the V2 names in place, so expenses
-- keep their category. Only untouched starter categories change: one a user renamed
-- (or created themselves) no longer matches an old name and is skipped.

create temporary table renames (old_name text primary key, new_name text, line text) on commit drop;
insert into renames values
    ('Supplies & Tools',     'Supplies',             'SUPPLIES'),
    ('Retail Products',      'Retail Resale',        'COGS'),
    ('Equipment',            'Equipment',            'DEPRECIATION'),
    ('Booth Rent',           'Booth Rent',           'RENT_OTHER_PROPERTY'),
    ('Marketing',            'Marketing',            'ADVERTISING'),
    ('Fees & Subscriptions', 'Fees & Subscriptions', 'COMMISSIONS_FEES'),
    ('Licenses & Insurance', 'Licenses & Insurance', 'TAXES_LICENSES'),
    ('Car, Travel & Meals',  'Travel & Food',        'TRAVEL'),
    ('Education & Other',    'Training & Other',     'OTHER');

update expense_category c
   set name = r.new_name,
       schedule_c_line = r.line,
       updated_at = now(),
       client_modified_at = now()
  from renames r
 where c.name = r.old_name
   and c.default_category
   and c.deleted_at is null
   and (c.name <> r.new_name or c.schedule_c_line <> r.line);
