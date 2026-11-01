create or replace function public.start_chat_session(p_guide_id uuid)
returns uuid language plpgsql security definer set search_path = public as $$
declare
  session_id uuid;
begin
  if auth.uid() is null then raise exception 'Authentication required'; end if;
  if not exists (select 1 from public.divine_guides where id = p_guide_id and is_active = true) then raise exception 'Guide unavailable'; end if;
  insert into public.chat_sessions(user_id, guide_id, status)
  values (auth.uid(), p_guide_id, 'active')
  returning id into session_id;
  return session_id;
end;
$$;

revoke all on function public.start_chat_session(uuid) from public;
grant execute on function public.start_chat_session(uuid) to authenticated;
