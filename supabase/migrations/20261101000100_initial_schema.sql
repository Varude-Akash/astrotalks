create table if not exists public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text,
  religion text not null default 'Hinduism',
  date_of_birth date,
  birth_time time,
  birthplace text,
  personalize_birth_details boolean not null default true,
  chat_history_enabled boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.divine_guides (
  id uuid primary key default gen_random_uuid(),
  religion text not null,
  name text not null,
  description text not null,
  rate_points_per_minute integer not null check (rate_points_per_minute > 0),
  personality_prompt text not null,
  is_active boolean not null default true,
  created_at timestamptz not null default now()
);

create table if not exists public.chat_sessions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  guide_id uuid not null references public.divine_guides(id),
  status text not null default 'active' check (status in ('active','awaiting_choice','paid_active','archived','expired')),
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  billable_seconds integer not null default 0 check (billable_seconds >= 0),
  points_used integer not null default 0 check (points_used >= 0),
  created_at timestamptz not null default now()
);

create table if not exists public.chat_messages (
  id uuid primary key default gen_random_uuid(),
  session_id uuid not null references public.chat_sessions(id) on delete cascade,
  role text not null check (role in ('user','assistant','system')),
  content text not null,
  delivered boolean not null default false,
  created_at timestamptz not null default now()
);

create table if not exists public.points_ledger (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  session_id uuid references public.chat_sessions(id),
  kind text not null check (kind in ('purchase','welcome','usage','refund','adjustment')),
  points integer not null,
  reference_id text,
  created_at timestamptz not null default now()
);

create table if not exists public.payment_transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  provider text not null,
  provider_payment_id text unique,
  amount_minor integer not null check (amount_minor > 0),
  currency text not null default 'INR',
  points integer not null check (points > 0),
  status text not null default 'pending' check (status in ('pending','verified','failed','refunded')),
  created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.chat_sessions enable row level security;
alter table public.chat_messages enable row level security;
alter table public.points_ledger enable row level security;
alter table public.payment_transactions enable row level security;
alter table public.divine_guides enable row level security;

create policy "profiles own row" on public.profiles for all using (auth.uid() = id) with check (auth.uid() = id);
create policy "sessions own rows" on public.chat_sessions for select using (auth.uid() = user_id);
create policy "messages own sessions" on public.chat_messages for select using (exists (select 1 from public.chat_sessions s where s.id = session_id and s.user_id = auth.uid()));
create policy "ledger own rows" on public.points_ledger for select using (auth.uid() = user_id);
create policy "payments own rows" on public.payment_transactions for select using (auth.uid() = user_id);
create policy "active guides readable" on public.divine_guides for select using (is_active = true);

create index if not exists chat_sessions_user_id_idx on public.chat_sessions(user_id, created_at desc);
create index if not exists chat_messages_session_id_idx on public.chat_messages(session_id, created_at);
create index if not exists points_ledger_user_id_idx on public.points_ledger(user_id, created_at desc);

create or replace function public.handle_new_user() returns trigger language plpgsql security definer set search_path = public as $$
begin
  insert into public.profiles (id, full_name) values (new.id, coalesce(new.raw_user_meta_data ->> 'full_name', new.email));
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users for each row execute procedure public.handle_new_user();
