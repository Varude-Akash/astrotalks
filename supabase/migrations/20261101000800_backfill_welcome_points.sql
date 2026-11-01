insert into public.points_ledger(user_id, kind, points, reference_id)
select p.id, 'welcome', 15, 'welcome-v1'
from public.profiles p
where not exists (
  select 1 from public.points_ledger l
  where l.user_id = p.id and l.kind = 'welcome' and l.reference_id = 'welcome-v1'
);
