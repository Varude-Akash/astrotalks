drop function if exists public.start_chat_session(uuid);

create function public.start_chat_session(p_guide_id uuid)
returns jsonb language plpgsql security definer set search_path = public as $$
declare
  session_id uuid;
  guide_rate integer;
  balance integer;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select rate_points_per_minute into guide_rate from public.divine_guides where id = p_guide_id and is_active = true;
  if guide_rate is null then raise exception 'Guide unavailable'; end if;
  select public.points_balance() into balance;
  if balance < guide_rate then raise exception 'Not enough points for one minute'; end if;
  insert into public.chat_sessions(user_id, guide_id, status, points_used)
  values (auth.uid(), p_guide_id, 'active', guide_rate)
  returning id into session_id;
  insert into public.points_ledger(user_id, session_id, kind, points, reference_id)
  values (auth.uid(), session_id, 'usage', -guide_rate, 'initial-one-minute-block');
  return jsonb_build_object('id', session_id, 'cost', guide_rate, 'balance', balance - guide_rate, 'seconds', 60);
end;
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
  if not found or session_row.status not in ('active', 'awaiting_choice', 'paid_active') then raise exception 'Session is not eligible for extension'; end if;
  select rate_points_per_minute into guide_rate from public.divine_guides where id = session_row.guide_id;
  cost := guide_rate * 5;
  select public.points_balance() into balance;
  if balance < cost then raise exception 'Not enough points'; end if;
  insert into public.points_ledger(user_id, session_id, kind, points, reference_id)
  values (auth.uid(), p_session_id, 'usage', -cost, 'five-minute-extension');
  update public.chat_sessions set status = 'paid_active', points_used = points_used + cost where id = p_session_id;
  return jsonb_build_object('cost', cost, 'balance', balance - cost, 'seconds', 300);
end;
$$;

revoke all on function public.start_chat_session(uuid) from public;
grant execute on function public.start_chat_session(uuid) to authenticated;
revoke all on function public.purchase_chat_extension(uuid) from public;
grant execute on function public.purchase_chat_extension(uuid) to authenticated;
