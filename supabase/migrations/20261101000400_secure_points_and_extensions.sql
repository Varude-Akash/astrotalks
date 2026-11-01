-- Welcome points are granted once, server-side, when a Google account is created.
create or replace function public.handle_new_user() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name) values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', new.email));
  insert into public.points_ledger (user_id, kind, points, reference_id)
  values (new.id, 'welcome', 15, 'welcome-v1')
  on conflict do nothing;
  return new;
end;
$$;

create or replace function public.points_balance()
returns integer language sql stable security invoker as $$
  select coalesce(sum(points), 0)::integer from public.points_ledger where user_id = auth.uid();
$$;

create or replace function public.purchase_chat_extension(p_session_id uuid)
returns jsonb language plpgsql security definer set search_path = public as $$
declare
  session_row public.chat_sessions;
  guide_rate integer;
  cost integer;
  balance integer;
begin
  select * into session_row from public.chat_sessions where id = p_session_id and user_id = auth.uid() for update;
  if not found or session_row.status not in ('active', 'awaiting_choice') then raise exception 'Session is not eligible for extension'; end if;
  select rate_points_per_minute into guide_rate from public.divine_guides where id = session_row.guide_id;
  cost := guide_rate * 5;
  select public.points_balance() into balance;
  if balance < cost then raise exception 'Not enough points'; end if;
  insert into public.points_ledger(user_id, session_id, kind, points, reference_id) values (auth.uid(), p_session_id, 'usage', -cost, 'five-minute-extension');
  update public.chat_sessions set status = 'paid_active', points_used = points_used + cost where id = p_session_id;
  return jsonb_build_object('cost', cost, 'balance', balance - cost, 'seconds', 300);
end;
$$;

create or replace function public.archive_chat_session(p_session_id uuid, p_billable_seconds integer)
returns void language plpgsql security definer set search_path = public as $$
begin
  update public.chat_sessions
  set status = 'archived', ended_at = now(), billable_seconds = greatest(0, p_billable_seconds)
  where id = p_session_id and user_id = auth.uid() and status in ('active','paid_active','awaiting_choice');
end;
$$;

revoke all on function public.purchase_chat_extension(uuid) from public;
grant execute on function public.purchase_chat_extension(uuid) to authenticated;
revoke all on function public.archive_chat_session(uuid, integer) from public;
grant execute on function public.archive_chat_session(uuid, integer) to authenticated;
