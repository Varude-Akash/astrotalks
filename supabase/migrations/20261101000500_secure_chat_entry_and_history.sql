create or replace function public.start_chat_session(p_guide_id uuid)
returns uuid language plpgsql security definer set search_path = public as $$
declare
  guide_rate integer;
  balance integer;
  session_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  select rate_points_per_minute into guide_rate from public.divine_guides where id = p_guide_id and is_active = true;
  if guide_rate is null then raise exception 'Guide unavailable'; end if;
  select public.points_balance() into balance;
  if balance < guide_rate * 5 then raise exception 'Not enough points'; end if;
  insert into public.chat_sessions(user_id, guide_id, status) values (auth.uid(), p_guide_id, 'active') returning id into session_id;
  return session_id;
end;
$$;

revoke all on function public.start_chat_session(uuid) from public;
grant execute on function public.start_chat_session(uuid) to authenticated;
