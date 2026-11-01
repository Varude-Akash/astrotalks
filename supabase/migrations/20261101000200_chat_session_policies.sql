-- Active sessions may be created and updated only by their owner.
create policy "sessions owner can create" on public.chat_sessions
  for insert with check (auth.uid() = user_id);

create policy "sessions owner can update" on public.chat_sessions
  for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- Messages can be written only into a session owned by the authenticated user.
create policy "messages owner can create" on public.chat_messages
  for insert with check (exists (select 1 from public.chat_sessions s where s.id = session_id and s.user_id = auth.uid()));

-- Ended sessions are immutable to the client; only the secure server function should archive them.
create or replace function public.delete_expired_archives()
returns integer
language plpgsql
security definer
set search_path = public
as $$
declare deleted_count integer;
begin
  delete from public.chat_sessions
  where status = 'archived'
    and ended_at < now() - interval '30 days';
  get diagnostics deleted_count = row_count;
  return deleted_count;
end;
$$;

revoke all on function public.delete_expired_archives() from public;

comment on table public.chat_messages is 'Messages are retained only while a session is active and for 30 days after archival.';
