insert into public.points_ledger(user_id, kind, points, reference_id)
select p.id, 'adjustment', 15 - coalesce(b.balance, 0), 'welcome-restoration-v2'
from public.profiles p
left join (
  select user_id, sum(points)::integer as balance
  from public.points_ledger
  group by user_id
) b on b.user_id = p.id
where coalesce(b.balance, 0) < 15
  and not exists (
    select 1 from public.points_ledger l
    where l.user_id = p.id and l.reference_id = 'welcome-restoration-v2'
  );
