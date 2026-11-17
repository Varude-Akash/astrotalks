drop function if exists public.start_chat_session(uuid);

create or replace function public.start_chat_session(p_guide_id uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  guide_rate integer;
  balance integer;
  minutes integer;
  cost integer;
  session_id uuid;
begin
  select rate_points_per_minute into guide_rate
  from public.divine_guides
  where id = p_guide_id and is_active = true;
  if guide_rate is null then raise exception 'Guide unavailable'; end if;

  select public.points_balance() into balance;
  minutes := floor(balance::numeric / guide_rate)::integer;
  if minutes < 1 then raise exception 'Not enough points for one minute'; end if;
  cost := minutes * guide_rate;

  insert into public.chat_sessions(user_id, guide_id, status, points_used)
  values (auth.uid(), p_guide_id, 'active', guide_rate)
  returning id into session_id;

  insert into public.points_ledger(user_id, session_id, kind, points, reference_id)
  values (auth.uid(), session_id, 'usage', -cost, 'chat-session-block');

  return jsonb_build_object(
    'id', session_id,
    'cost', cost,
    'balance', balance - cost,
    'seconds', minutes * 60,
    'minutes', minutes
  );
end;
$$;

revoke all on function public.start_chat_session(uuid) from public;
grant execute on function public.start_chat_session(uuid) to authenticated;
