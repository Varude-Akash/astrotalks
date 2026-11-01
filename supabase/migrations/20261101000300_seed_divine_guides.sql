insert into public.divine_guides (id, religion, name, description, points_per_minute, is_active)
values
('11111111-1111-1111-1111-111111111111','Hinduism','Shri Krishna','For clarity, courage and a lighter heart.',15,true),
('22222222-2222-2222-2222-222222222222','Hinduism','Lord Shiva','For stillness, release and transformation.',20,true),
('33333333-3333-3333-3333-333333333333','Hinduism','Lord Ganesha','For new beginnings and clearing the way.',25,true),
('44444444-4444-4444-4444-444444444444','Hinduism','Shri Hanuman','For strength, devotion and resilience.',30,true),
('55555555-5555-5555-5555-555555555555','Hinduism','Goddess Saraswati','For learning, creativity and expression.',35,true),
('66666666-6666-6666-6666-666666666666','Hinduism','Mata Lakshmi','For gratitude, abundance and grace.',40,true),
('77777777-7777-7777-7777-777777777777','Hinduism','Maa Durga','For courage, protection and inner power.',30,true),
('88888888-8888-8888-8888-888888888888','Hinduism','Maa Kali','For truth, release and transformation.',35,true),
('99999999-9999-9999-9999-999999999999','Hinduism','Lord Kartikeya','For focus, discipline and fearless action.',30,true),
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa','Hinduism','Shani Dev','For patience, responsibility and balance.',25,true),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb','Christianity','Jesus Christ','For compassion, forgiveness and hope.',20,true)
on conflict (id) do update set description = excluded.description, points_per_minute = excluded.points_per_minute, is_active = true;
