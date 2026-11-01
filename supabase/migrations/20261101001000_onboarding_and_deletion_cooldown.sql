alter table public.profiles
  add column if not exists onboarding_completed boolean not null default false;

update public.profiles
set onboarding_completed = true
where full_name is not null
  and religion is not null
  and date_of_birth is not null
  and birth_time is not null
  and birthplace is not null
  and trim(birthplace) <> '';

create table if not exists public.account_deletion_cooldowns (
  email text primary key,
  deleted_at timestamptz not null default now()
);

alter table public.account_deletion_cooldowns enable row level security;
revoke all on public.account_deletion_cooldowns from anon, authenticated;

comment on table public.account_deletion_cooldowns is 'Server-only tombstones preventing repeated account deletion within 24 hours.';
